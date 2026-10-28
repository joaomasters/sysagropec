package br.com.sysagropec.pesagem.application.usecase;

import br.com.sysagropec.pesagem.application.port.in.PesagemUseCase;
import br.com.sysagropec.pesagem.application.port.out.PesagemPorts.AnimaisPort;
import br.com.sysagropec.pesagem.application.port.out.PesagemPorts.AnimaisPort.AnimalPesavel;
import br.com.sysagropec.pesagem.application.port.out.PesagemPorts.PesagemRepositoryPort;
import br.com.sysagropec.pesagem.application.port.out.PesagemPorts.TarefasPort;
import br.com.sysagropec.pesagem.domain.model.Pesagem;
import br.com.sysagropec.shared.application.Hashes;
import br.com.sysagropec.shared.application.port.out.AuditoriaPort;
import br.com.sysagropec.shared.application.port.out.AuditoriaPort.EventoAuditoria;
import br.com.sysagropec.shared.application.port.out.IdempotenciaPort;
import br.com.sysagropec.shared.application.port.out.RelogioPort;
import br.com.sysagropec.shared.application.seguranca.ContextoExecucao;
import br.com.sysagropec.shared.domain.ConflitoException;
import br.com.sysagropec.shared.domain.NaoEncontradoException;
import br.com.sysagropec.shared.domain.RegraNegocioException;
import br.com.sysagropec.shared.domain.Verificar;
import br.com.sysagropec.shared.domain.acesso.Permissao;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class PesagemService implements PesagemUseCase, PesagemUseCase.Consultas {

    static final String ESCOPO = "PESAGEM";

    private final PesagemRepositoryPort pesagens;
    private final AnimaisPort animais;
    private final TarefasPort tarefas;
    private final IdempotenciaPort idempotencia;
    private final AuditoriaPort auditoria;
    private final RelogioPort relogio;

    public PesagemService(PesagemRepositoryPort pesagens, AnimaisPort animais, TarefasPort tarefas,
                          IdempotenciaPort idempotencia, AuditoriaPort auditoria, RelogioPort relogio) {
        this.pesagens = pesagens;
        this.animais = animais;
        this.tarefas = tarefas;
        this.idempotencia = idempotencia;
        this.auditoria = auditoria;
        this.relogio = relogio;
    }

    @Override
    public PesagemRegistrada registrar(ContextoExecucao ctx, RegistrarPesagemComando cmd) {
        UUID org = ctx.organizacaoId();
        AnimalPesavel animal = animal(ctx, cmd.animalId());
        ctx.exigir(Permissao.PESAGEM_REGISTRAR, animal.propriedadeId());

        String hash = Hashes.sha256(cmd.toString());
        String chave = Verificar.textoOpcional(cmd.chaveIdempotencia(), "Chave de idempotência", 100);
        if (chave != null) {
            Optional<UUID> anterior = idempotencia.resultadoAnterior(org, ESCOPO, chave, hash);
            if (anterior.isPresent()) {
                Pesagem p = pesagens.buscar(org, anterior.get()).orElseThrow(() -> new NaoEncontradoException("Pesagem"));
                return new PesagemRegistrada(p.id(), false);
            }
        }

        Verificar.regra(animal.ativo(), "Somente animais ativos podem ser pesados");
        Instant agora = relogio.agora();
        Pesagem.Finalidade finalidade = enumOuPadrao(Pesagem.Finalidade.class, cmd.finalidade(),
                Pesagem.Finalidade.ACOMPANHAMENTO, "Finalidade");
        if (finalidade == Pesagem.Finalidade.ENTRADA && pesagens.existeEntradaValida(animal.id())) {
            throw new ConflitoException("O animal já tem pesagem de entrada. Cancele a anterior para corrigi-la");
        }
        Pesagem pesagem = Pesagem.registrar(UUID.randomUUID(), org, animal.propriedadeId(), animal.id(), cmd.pesoKg(),
                enumOuPadrao(Pesagem.Natureza.class, cmd.natureza(), Pesagem.Natureza.MEDIDO, "Natureza"),
                enumOuPadrao(Pesagem.Metodo.class, cmd.metodo(), Pesagem.Metodo.BALANCA_INDIVIDUAL, "Método"),
                finalidade, cmd.equipamento(), cmd.observacao(), cmd.ocorridoEm(), animal.entradaEm(), agora,
                ctx.usuarioId(), cmd.tarefaId());
        pesagens.salvar(pesagem);

        boolean tarefaConcluida = cmd.tarefaId() != null
                && tarefas.vincularPesagem(ctx, cmd.tarefaId(), animal.id(), pesagem.id());
        if (chave != null) {
            idempotencia.registrar(org, ESCOPO, chave, hash, pesagem.id());
        }
        return new PesagemRegistrada(pesagem.id(), tarefaConcluida);
    }

    @Override
    public void cancelar(ContextoExecucao ctx, UUID pesagemId, String motivo) {
        Pesagem pesagem = pesagens.buscar(ctx.organizacaoId(), pesagemId)
                .orElseThrow(() -> new NaoEncontradoException("Pesagem"));
        ctx.exigir(Permissao.PESAGEM_CANCELAR, pesagem.propriedadeId());
        Instant agora = relogio.agora();
        pesagem.cancelar(ctx.usuarioId(), motivo, agora);
        pesagens.salvar(pesagem);
        auditoria.registrar(new EventoAuditoria(ctx.organizacaoId(), ctx.usuarioId(), "PESAGEM_CANCELADA", "PESAGEM",
                pesagemId, Map.of("motivo", pesagem.motivoCancelamento(), "pesoKg", pesagem.pesoKg().toPlainString()),
                agora));
    }

    @Override
    public List<PesagemResumo> listarPorAnimal(ContextoExecucao ctx, UUID animalId) {
        animal(ctx, animalId);
        return pesagens.listarPorAnimal(ctx.organizacaoId(), animalId).stream()
                .sorted(Comparator.comparing(Pesagem::ocorridoEm).reversed())
                .map(PesagemService::resumo)
                .toList();
    }

    /** Animal de propriedade sem acesso é tratado como inexistente. */
    private AnimalPesavel animal(ContextoExecucao ctx, UUID animalId) {
        AnimalPesavel animal = animais.animal(ctx.organizacaoId(), Verificar.obrigatorio(animalId, "Animal"))
                .orElseThrow(() -> new NaoEncontradoException("Animal"));
        if (!ctx.pode(Permissao.CONSULTAR, animal.propriedadeId())) {
            throw new NaoEncontradoException("Animal");
        }
        return animal;
    }

    private static PesagemResumo resumo(Pesagem p) {
        return new PesagemResumo(p.id(), p.animalId(), p.pesoKg(), p.natureza().name(), p.metodo().name(),
                p.finalidade().name(), p.equipamento(), p.observacao(), p.ocorridoEm(), p.registradoEm(),
                p.responsavelId(), p.tarefaId(), p.cancelada(), p.canceladaEm(), p.motivoCancelamento());
    }

    private static <E extends Enum<E>> E enumOuPadrao(Class<E> tipo, String valor, E padrao, String campo) {
        if (valor == null || valor.isBlank()) {
            return padrao;
        }
        try {
            return Enum.valueOf(tipo, valor);
        } catch (IllegalArgumentException e) {
            throw new RegraNegocioException(campo + " inválido(a): " + valor);
        }
    }
}
