package br.com.sysagropec.tarefas.application.usecase;

import br.com.sysagropec.shared.application.Pagina;
import br.com.sysagropec.shared.application.port.out.AuditoriaPort;
import br.com.sysagropec.shared.application.port.out.AuditoriaPort.EventoAuditoria;
import br.com.sysagropec.shared.application.port.out.RelogioPort;
import br.com.sysagropec.shared.application.seguranca.ContextoExecucao;
import br.com.sysagropec.shared.domain.NaoEncontradoException;
import br.com.sysagropec.shared.domain.RegraNegocioException;
import br.com.sysagropec.shared.domain.Verificar;
import br.com.sysagropec.shared.domain.acesso.Permissao;
import br.com.sysagropec.tarefas.application.port.in.TarefasResultadoUseCase;
import br.com.sysagropec.tarefas.application.port.in.TarefasUseCase;
import br.com.sysagropec.tarefas.application.port.out.TarefasPorts.AlvosPort;
import br.com.sysagropec.tarefas.application.port.out.TarefasPorts.ConsultaTarefas;
import br.com.sysagropec.tarefas.application.port.out.TarefasPorts.PropriedadesPort;
import br.com.sysagropec.tarefas.application.port.out.TarefasPorts.TarefaRepositoryPort;
import br.com.sysagropec.tarefas.application.port.out.TarefasPorts.UsuariosPort;
import br.com.sysagropec.tarefas.domain.model.Tarefa;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TarefasService implements TarefasUseCase, TarefasUseCase.Consultas, TarefasResultadoUseCase {

    private final TarefaRepositoryPort tarefas;
    private final AlvosPort alvos;
    private final UsuariosPort usuarios;
    private final PropriedadesPort propriedades;
    private final AuditoriaPort auditoria;
    private final RelogioPort relogio;

    public TarefasService(TarefaRepositoryPort tarefas, AlvosPort alvos, UsuariosPort usuarios,
                          PropriedadesPort propriedades, AuditoriaPort auditoria, RelogioPort relogio) {
        this.tarefas = tarefas;
        this.alvos = alvos;
        this.usuarios = usuarios;
        this.propriedades = propriedades;
        this.auditoria = auditoria;
        this.relogio = relogio;
    }

    @Override
    public UUID criar(ContextoExecucao ctx, CriarTarefaComando cmd) {
        ctx.exigir(Permissao.TAREFA_GERIR, cmd.propriedadeId());
        UUID org = ctx.organizacaoId();
        Tarefa.Alvo alvo = new Tarefa.Alvo(
                enumObrigatorio(Tarefa.TipoAlvo.class, cmd.alvoTipo(), "Tipo do alvo"),
                cmd.alvoTipo() != null && cmd.alvoTipo().equals("PROPRIEDADE") && cmd.alvoId() == null
                        ? cmd.propriedadeId() : cmd.alvoId());
        if (!alvos.pertence(org, cmd.propriedadeId(), alvo)) {
            throw new NaoEncontradoException("Alvo da tarefa");
        }
        if (cmd.responsavelId() != null && !usuarios.podeExecutar(org, cmd.responsavelId(), cmd.propriedadeId())) {
            throw new RegraNegocioException("O responsável não tem permissão para executar tarefas nesta propriedade");
        }
        List<Tarefa.NovoItem> itens = cmd.checklist() == null ? List.of() : cmd.checklist().stream()
                .map(i -> new Tarefa.NovoItem(i.descricao(), i.obrigatorio())).toList();
        Instant agora = relogio.agora();
        Tarefa tarefa = Tarefa.criar(UUID.randomUUID(), org, cmd.propriedadeId(),
                enumObrigatorio(Tarefa.Tipo.class, cmd.tipo(), "Tipo"), cmd.titulo(), cmd.descricao(), alvo,
                cmd.responsavelId(), cmd.prazo(),
                cmd.prioridade() == null ? null : enumObrigatorio(Tarefa.Prioridade.class, cmd.prioridade(), "Prioridade"),
                itens, ctx.usuarioId(), agora);
        tarefas.salvar(tarefa);
        return tarefa.id();
    }

    @Override
    public void iniciar(ContextoExecucao ctx, UUID tarefaId) {
        executar(ctx, tarefaId, null, (t, agora) -> t.iniciar(ctx.usuarioId(), agora));
    }

    @Override
    public void concluir(ContextoExecucao ctx, UUID tarefaId, String observacao, Set<UUID> itensConcluidos) {
        executar(ctx, tarefaId, null, (t, agora) -> t.concluir(ctx.usuarioId(), agora, observacao, itensConcluidos));
    }

    @Override
    public void impedir(ContextoExecucao ctx, UUID tarefaId, String justificativa) {
        executar(ctx, tarefaId, "TAREFA_IMPEDIDA", (t, agora) -> t.impedir(ctx.usuarioId(), agora, justificativa));
    }

    @Override
    public void cancelar(ContextoExecucao ctx, UUID tarefaId, String justificativa) {
        Tarefa tarefa = carregar(ctx, tarefaId);
        ctx.exigir(Permissao.TAREFA_GERIR, tarefa.propriedadeId());
        Instant agora = relogio.agora();
        tarefa.cancelar(ctx.usuarioId(), agora, justificativa);
        tarefas.salvar(tarefa);
        auditar(ctx, "TAREFA_CANCELADA", tarefa, agora);
    }

    @Override
    public boolean registrarResultado(ContextoExecucao ctx, UUID tarefaId, Resultado resultado) {
        Tarefa tarefa = carregar(ctx, tarefaId);
        ctx.exigir(Permissao.TAREFA_EXECUTAR, tarefa.propriedadeId());
        Tarefa.Alvo alvoResultado = new Tarefa.Alvo(
                enumObrigatorio(Tarefa.TipoAlvo.class, resultado.alvoTipo(), "Tipo do alvo"), resultado.alvoId());
        if (!alvos.pertence(ctx.organizacaoId(), tarefa.propriedadeId(), alvoResultado)) {
            throw new RegraNegocioException("O registro não pertence à propriedade da tarefa");
        }
        Instant agora = relogio.agora();
        boolean concluiu = tarefa.vincularResultado(resultado.tipo(), resultado.id(), alvoResultado, ctx.usuarioId(),
                agora);
        tarefas.salvar(tarefa);
        return concluiu;
    }

    @Override
    public Pagina<TarefaResumo> listar(ContextoExecucao ctx, FiltroTarefas f) {
        ctx.exigir(Permissao.CONSULTAR, f.propriedadeId());
        Instant agora = relogio.agora();
        Set<Tarefa.Status> status = f.status() == null ? Set.of() : f.status().stream()
                .map(s -> enumObrigatorio(Tarefa.Status.class, s, "Status"))
                .collect(Collectors.toSet());
        if (f.apenasAtrasadas()) {
            status = status.isEmpty() ? EnumSet.of(Tarefa.Status.PENDENTE, Tarefa.Status.EM_ANDAMENTO)
                    : status.stream().filter(Tarefa.Status::aberta).collect(Collectors.toSet());
            if (status.isEmpty()) {
                return new Pagina<>(List.of(), Pagina.paginaValida(f.pagina()), Pagina.tamanhoValido(f.tamanho()), 0);
            }
        }
        Tarefa.TipoAlvo alvoTipo = f.alvoTipo() == null ? null
                : enumObrigatorio(Tarefa.TipoAlvo.class, f.alvoTipo(), "Tipo do alvo");
        Pagina<Tarefa> pagina = tarefas.listar(new ConsultaTarefas(ctx.organizacaoId(), f.propriedadeId(), status,
                f.responsavelId(), alvoTipo, f.alvoId(), f.apenasAtrasadas() ? agora : null, null,
                Pagina.paginaValida(f.pagina()), Pagina.tamanhoValido(f.tamanho())));
        return resumir(ctx, pagina, agora);
    }

    /**
     * Agenda do operador: tarefas abertas com prazo até o fim do dia (inclui atrasadas de dias anteriores)
     * e as finalizadas hoje, no fuso horário da propriedade.
     */
    @Override
    public RotinasDoDia rotinasDeHoje(ContextoExecucao ctx, UUID propriedadeId) {
        ctx.exigir(Permissao.CONSULTAR, propriedadeId);
        UUID org = ctx.organizacaoId();
        Instant agora = relogio.agora();
        ZoneId zona = propriedades.fusoHorario(org, propriedadeId);
        LocalDate hoje = LocalDate.ofInstant(agora, zona);
        Instant inicioDoDia = hoje.atStartOfDay(zona).toInstant();
        Instant fimDoDia = hoje.plusDays(1).atStartOfDay(zona).toInstant();

        Pagina<Tarefa> abertas = tarefas.listar(new ConsultaTarefas(org, propriedadeId,
                EnumSet.of(Tarefa.Status.PENDENTE, Tarefa.Status.EM_ANDAMENTO), null, null, null, fimDoDia, null, 0,
                Pagina.TAMANHO_MAXIMO));
        Pagina<Tarefa> finalizadas = tarefas.listar(new ConsultaTarefas(org, propriedadeId,
                EnumSet.of(Tarefa.Status.CONCLUIDA, Tarefa.Status.IMPEDIDA, Tarefa.Status.CANCELADA), null, null, null,
                null, inicioDoDia, 0, Pagina.TAMANHO_MAXIMO));
        return new RotinasDoDia(hoje, zona.getId(), resumir(ctx, abertas, agora).itens(),
                resumir(ctx, finalizadas, agora).itens());
    }

    @Override
    public TarefaDetalhe detalhar(ContextoExecucao ctx, UUID tarefaId) {
        Tarefa t = carregar(ctx, tarefaId);
        Instant agora = relogio.agora();
        Map<UUID, String> nomes = usuarios.nomes(ctx.organizacaoId(),
                Stream.of(t.responsavelId(), t.finalizadaPor()).filter(Objects::nonNull).collect(Collectors.toSet()));
        return new TarefaDetalhe(resumo(ctx, t, nomes, agora), t.descricao(), t.criadaPor(), t.criadaEm(),
                t.iniciadaEm(), t.finalizadaPor(), t.finalizadaPor() == null ? null : nomes.get(t.finalizadaPor()),
                t.observacaoConclusao(), t.justificativa(),
                t.checklist().stream().map(i -> new ItemChecklistDetalhe(i.id(), i.ordem(), i.descricao(),
                        i.obrigatorio(), i.concluido(), i.concluidoEm())).toList(),
                t.resultados().stream().map(r -> new ResultadoDetalhe(r.tipo(), r.id(), r.vinculadoEm())).toList());
    }

    private void executar(ContextoExecucao ctx, UUID tarefaId, String acaoAuditada,
                          BiConsumer<Tarefa, Instant> operacao) {
        Tarefa tarefa = carregar(ctx, tarefaId);
        ctx.exigir(Permissao.TAREFA_EXECUTAR, tarefa.propriedadeId());
        Instant agora = relogio.agora();
        operacao.accept(tarefa, agora);
        tarefas.salvar(tarefa);
        if (acaoAuditada != null) {
            auditar(ctx, acaoAuditada, tarefa, agora);
        }
    }

    /** Tarefa de propriedade sem acesso é tratada como inexistente. */
    private Tarefa carregar(ContextoExecucao ctx, UUID tarefaId) {
        Tarefa tarefa = tarefas.buscar(ctx.organizacaoId(), tarefaId)
                .orElseThrow(() -> new NaoEncontradoException("Tarefa"));
        if (!ctx.pode(Permissao.CONSULTAR, tarefa.propriedadeId())) {
            throw new NaoEncontradoException("Tarefa");
        }
        return tarefa;
    }

    private Pagina<TarefaResumo> resumir(ContextoExecucao ctx, Pagina<Tarefa> pagina, Instant agora) {
        Set<UUID> responsaveis = new HashSet<>();
        pagina.itens().forEach(t -> {
            if (t.responsavelId() != null) {
                responsaveis.add(t.responsavelId());
            }
        });
        Map<UUID, String> nomes = responsaveis.isEmpty() ? Map.of() : usuarios.nomes(ctx.organizacaoId(), responsaveis);
        return pagina.map(t -> resumo(ctx, t, nomes, agora));
    }

    private TarefaResumo resumo(ContextoExecucao ctx, Tarefa t, Map<UUID, String> nomes, Instant agora) {
        int concluidos = (int) t.checklist().stream().filter(Tarefa.ItemChecklist::concluido).count();
        return new TarefaResumo(t.id(), t.propriedadeId(), t.tipo().name(), t.titulo(), t.alvo().tipo().name(),
                t.alvo().id(), alvos.descrever(ctx.organizacaoId(), t.alvo()).orElse(null), t.responsavelId(),
                t.responsavelId() == null ? null : nomes.get(t.responsavelId()), t.prazo(), t.prioridade().name(),
                t.status().name(), t.atrasada(agora), t.checklist().size(), concluidos, t.finalizadaEm());
    }

    private void auditar(ContextoExecucao ctx, String acao, Tarefa t, Instant agora) {
        auditoria.registrar(new EventoAuditoria(ctx.organizacaoId(), ctx.usuarioId(), acao, "TAREFA", t.id(),
                Map.of("justificativa", Objects.toString(t.justificativa(), "")), agora));
    }

    private static <E extends Enum<E>> E enumObrigatorio(Class<E> tipo, String valor, String campo) {
        Verificar.obrigatorio(valor, campo);
        try {
            return Enum.valueOf(tipo, valor);
        } catch (IllegalArgumentException e) {
            throw new RegraNegocioException(campo + " inválido(a): " + valor);
        }
    }
}
