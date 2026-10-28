package br.com.sysagropec.rebanho.application.usecase;

import br.com.sysagropec.rebanho.application.port.in.RebanhoUseCase;
import br.com.sysagropec.rebanho.application.port.out.RebanhoPorts.AnimalRepositoryPort;
import br.com.sysagropec.rebanho.application.port.out.RebanhoPorts.LoteRepositoryPort;
import br.com.sysagropec.rebanho.application.port.out.RebanhoPorts.MovimentacaoRepositoryPort;
import br.com.sysagropec.rebanho.application.port.out.RebanhoPorts.PropriedadesPort;
import br.com.sysagropec.rebanho.application.port.out.RebanhoPorts.PropriedadesPort.InstalacaoAlocavel;
import br.com.sysagropec.rebanho.domain.model.AlteracaoAlocacao;
import br.com.sysagropec.rebanho.domain.model.Animal;
import br.com.sysagropec.rebanho.domain.model.Enums.Categoria;
import br.com.sysagropec.rebanho.domain.model.Enums.FaseLote;
import br.com.sysagropec.rebanho.domain.model.Enums.Finalidade;
import br.com.sysagropec.rebanho.domain.model.Enums.OrigemAnimal;
import br.com.sysagropec.rebanho.domain.model.Enums.Sexo;
import br.com.sysagropec.rebanho.domain.model.Enums.TipoIdentificacao;
import br.com.sysagropec.rebanho.domain.model.Enums.TipoMovimentacao;
import br.com.sysagropec.rebanho.domain.model.Lote;
import br.com.sysagropec.rebanho.domain.model.Movimentacao;
import br.com.sysagropec.rebanho.domain.model.Recebimento;
import br.com.sysagropec.rebanho.domain.service.PoliticaLotacao;
import br.com.sysagropec.shared.application.Hashes;
import br.com.sysagropec.shared.application.port.out.AuditoriaPort;
import br.com.sysagropec.shared.application.port.out.AuditoriaPort.EventoAuditoria;
import br.com.sysagropec.shared.application.port.out.IdempotenciaPort;
import br.com.sysagropec.shared.application.port.out.RelogioPort;
import br.com.sysagropec.shared.application.seguranca.ContextoExecucao;
import br.com.sysagropec.shared.domain.ConflitoException;
import br.com.sysagropec.shared.domain.Momento;
import br.com.sysagropec.shared.domain.NaoEncontradoException;
import br.com.sysagropec.shared.domain.RegraNegocioException;
import br.com.sysagropec.shared.domain.Verificar;
import br.com.sysagropec.shared.domain.acesso.Permissao;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

public class RebanhoService implements RebanhoUseCase {

    static final String ESCOPO_RECEBIMENTO = "RECEBIMENTO";

    private final AnimalRepositoryPort animais;
    private final LoteRepositoryPort lotes;
    private final MovimentacaoRepositoryPort movimentacoes;
    private final PropriedadesPort propriedades;
    private final IdempotenciaPort idempotencia;
    private final AuditoriaPort auditoria;
    private final RelogioPort relogio;

    public RebanhoService(AnimalRepositoryPort animais, LoteRepositoryPort lotes,
                          MovimentacaoRepositoryPort movimentacoes, PropriedadesPort propriedades,
                          IdempotenciaPort idempotencia, AuditoriaPort auditoria, RelogioPort relogio) {
        this.animais = animais;
        this.lotes = lotes;
        this.movimentacoes = movimentacoes;
        this.propriedades = propriedades;
        this.idempotencia = idempotencia;
        this.auditoria = auditoria;
        this.relogio = relogio;
    }

    @Override
    public RecebimentoRegistrado receber(ContextoExecucao ctx, ReceberAnimaisComando cmd) {
        ctx.exigir(Permissao.ANIMAL_RECEBER, cmd.propriedadeId());
        UUID org = ctx.organizacaoId();
        String hash = Hashes.sha256(cmd.toString());
        String chave = Verificar.textoOpcional(cmd.chaveIdempotencia(), "Chave de idempotência", 100);
        if (chave != null) {
            var anterior = idempotencia.resultadoAnterior(org, ESCOPO_RECEBIMENTO, chave, hash);
            if (anterior.isPresent()) {
                return resultadoDoRecebimento(org, anterior.get());
            }
        }

        List<NovoAnimal> novos = Verificar.naoVazia(cmd.animais(), "Animais");
        Verificar.regra(novos.size() <= Recebimento.MAXIMO_ANIMAIS,
                "Um recebimento aceita no máximo " + Recebimento.MAXIMO_ANIMAIS + " animais");
        Instant agora = relogio.agora();
        Instant ocorridoEm = Momento.exigirNaoFuturo(cmd.ocorridoEm(), agora, "Data do recebimento");

        // Ordem de bloqueio fixa (lote, depois instalação) em todas as operações, para evitar deadlocks.
        if (cmd.loteId() != null) {
            loteDaPropriedade(org, cmd.propriedadeId(), cmd.loteId()).exigirAptoAReceber(ocorridoEm);
        }
        InstalacaoAlocavel instalacao = instalacaoDaPropriedade(org, cmd.propriedadeId(), cmd.instalacaoId());
        exigirIdentificacoesLivres(org, TipoIdentificacao.BRINCO, novos.stream().map(NovoAnimal::brinco).toList());
        exigirIdentificacoesLivres(org, TipoIdentificacao.ELETRONICO,
                novos.stream().map(NovoAnimal::identificadorEletronico).filter(Objects::nonNull).toList());
        PoliticaLotacao.verificar(instalacao.codigo(), instalacao.capacidadeCabecas(),
                animais.contarNaInstalacao(instalacao.id()), novos.size());

        ZoneId zona = propriedades.fusoHorario(org, cmd.propriedadeId());
        LocalDate dataEntrada = LocalDate.ofInstant(ocorridoEm, zona);
        OrigemAnimal origem = enumOuPadrao(OrigemAnimal.class, cmd.origem(), OrigemAnimal.COMPRA, "Origem");
        UUID recebimentoId = UUID.randomUUID();
        UUID movimentacaoId = UUID.randomUUID();
        long numero = animais.reservarNumeros(org, novos.size());

        List<Animal> criados = new ArrayList<>();
        for (NovoAnimal n : novos) {
            Animal.DadosEntrada dados = new Animal.DadosEntrada(n.brinco(), n.identificadorEletronico(),
                    enumObrigatorio(Sexo.class, n.sexo(), "Sexo"),
                    enumObrigatorio(Categoria.class, n.categoria(), "Categoria"),
                    enumOuPadrao(Finalidade.class, n.finalidade(), Finalidade.ENGORDA, "Finalidade"),
                    n.raca(), n.dataNascimento(), n.nascimentoEstimado(), origem, cmd.origemDescricao());
            criados.add(Animal.registrarEntrada(UUID.randomUUID(), org, cmd.propriedadeId(), numero++, dados,
                    recebimentoId, movimentacaoId, instalacao.id(), cmd.loteId(), ocorridoEm, dataEntrada, agora));
        }

        Movimentacao movimentacao = Movimentacao.registrar(movimentacaoId, org, cmd.propriedadeId(),
                TipoMovimentacao.RECEBIMENTO, motivoRecebimento(cmd), ocorridoEm, agora, ctx.usuarioId(),
                criados.stream().map(a -> new AlteracaoAlocacao(a.id(), null, cmd.loteId(), null, instalacao.id()))
                        .toList());
        Recebimento recebimento = Recebimento.registrar(recebimentoId, org, cmd.propriedadeId(), instalacao.id(),
                cmd.loteId(), movimentacaoId, ocorridoEm, agora, cmd.origemDescricao(), cmd.documento(),
                cmd.observacao(), criados.size(), ctx.usuarioId());

        movimentacoes.salvar(movimentacao);
        movimentacoes.salvar(recebimento);
        criados.forEach(animais::salvar);
        if (chave != null) {
            idempotencia.registrar(org, ESCOPO_RECEBIMENTO, chave, hash, recebimentoId);
        }
        auditoria.registrar(new EventoAuditoria(org, ctx.usuarioId(), "ANIMAIS_RECEBIDOS", "RECEBIMENTO",
                recebimentoId, Map.of("quantidade", criados.size(), "instalacaoId", instalacao.id()), agora));
        return new RecebimentoRegistrado(recebimentoId, movimentacaoId, criados.stream().map(RebanhoService::recebido)
                .toList());
    }

    @Override
    public LoteFormado formarLote(ContextoExecucao ctx, FormarLoteComando cmd) {
        ctx.exigir(Permissao.LOTE_GERIR, cmd.propriedadeId());
        UUID org = ctx.organizacaoId();
        Instant agora = relogio.agora();
        Instant ocorridoEm = Momento.exigirNaoFuturo(cmd.ocorridoEm(), agora, "Data de formação");
        Lote lote = Lote.abrir(UUID.randomUUID(), org, cmd.propriedadeId(), cmd.codigo(),
                enumObrigatorio(FaseLote.class, cmd.fase(), "Fase"),
                enumOuPadrao(Finalidade.class, cmd.finalidade(), Finalidade.ENGORDA, "Finalidade"),
                cmd.observacao(), ocorridoEm, agora);
        if (lotes.existeCodigoAtivo(cmd.propriedadeId(), lote.codigo())) {
            throw new ConflitoException("Já existe um lote ativo com o código " + lote.codigo());
        }
        lotes.salvar(lote);

        UUID movimentacaoId = null;
        List<UUID> animalIds = cmd.animalIds() == null ? List.of() : cmd.animalIds();
        if (!animalIds.isEmpty()) {
            String motivo = cmd.motivo() == null || cmd.motivo().isBlank()
                    ? "Formação do lote " + lote.codigo() : cmd.motivo();
            movimentacaoId = realocar(ctx, cmd.propriedadeId(), animalIds,
                    new Animal.Destino(lote.id(), false, cmd.instalacaoId()), lote, TipoMovimentacao.FORMACAO_LOTE,
                    motivo, ocorridoEm, agora);
        }
        auditoria.registrar(new EventoAuditoria(org, ctx.usuarioId(), "LOTE_FORMADO", "LOTE", lote.id(),
                Map.of("codigo", lote.codigo(), "animais", animalIds.size()), agora));
        return new LoteFormado(lote.id(), movimentacaoId);
    }

    @Override
    public UUID movimentar(ContextoExecucao ctx, MovimentarAnimaisComando cmd) {
        ctx.exigir(Permissao.ANIMAL_MOVIMENTAR, cmd.propriedadeId());
        Instant agora = relogio.agora();
        Instant ocorridoEm = Momento.exigirNaoFuturo(cmd.ocorridoEm(), agora, "Data da movimentação");
        Animal.Destino destino = new Animal.Destino(cmd.loteDestinoId(), cmd.retirarDoLote(),
                cmd.instalacaoDestinoId());
        Lote loteDestino = null;
        if (cmd.loteDestinoId() != null) {
            loteDestino = lotes.buscarLoteComBloqueio(ctx.organizacaoId(), cmd.loteDestinoId())
                    .filter(l -> l.propriedadeId().equals(cmd.propriedadeId()))
                    .orElseThrow(() -> new NaoEncontradoException("Lote"));
        }
        UUID movimentacaoId = realocar(ctx, cmd.propriedadeId(), Verificar.naoVazia(cmd.animalIds(), "Animais"),
                destino, loteDestino, TipoMovimentacao.TRANSFERENCIA_INTERNA, cmd.motivo(), ocorridoEm, agora);
        auditoria.registrar(new EventoAuditoria(ctx.organizacaoId(), ctx.usuarioId(), "ANIMAIS_MOVIMENTADOS",
                "MOVIMENTACAO", movimentacaoId, Map.of("animais", cmd.animalIds().size()), agora));
        return movimentacaoId;
    }

    @Override
    public void encerrarLote(ContextoExecucao ctx, UUID loteId, Instant ocorridoEm) {
        Lote lote = lotes.buscarLoteComBloqueio(ctx.organizacaoId(), loteId)
                .orElseThrow(() -> new NaoEncontradoException("Lote"));
        ctx.exigir(Permissao.LOTE_GERIR, lote.propriedadeId());
        Instant agora = relogio.agora();
        Instant quando = ocorridoEm == null ? agora : Momento.exigirNaoFuturo(ocorridoEm, agora, "Data de encerramento");
        lote.encerrar(quando, animais.contarNoLote(loteId));
        lotes.salvar(lote);
        auditoria.registrar(new EventoAuditoria(ctx.organizacaoId(), ctx.usuarioId(), "LOTE_ENCERRADO", "LOTE",
                loteId, Map.of("codigo", lote.codigo()), agora));
    }

    /**
     * Núcleo comum das movimentações: valida o destino, bloqueia a instalação de destino para checar a lotação
     * sob concorrência, aplica a regra de cada animal e grava animais e movimentação na mesma transação.
     */
    private UUID realocar(ContextoExecucao ctx, UUID propriedadeId, List<UUID> animalIds, Animal.Destino destino,
                          Lote loteDestino, TipoMovimentacao tipo, String motivo, Instant ocorridoEm, Instant agora) {
        UUID org = ctx.organizacaoId();
        Set<UUID> ids = new LinkedHashSet<>(animalIds);
        Verificar.regra(ids.size() == animalIds.size(), "A lista de animais contém itens repetidos");
        Verificar.regra(ids.size() <= Recebimento.MAXIMO_ANIMAIS,
                "Uma movimentação aceita no máximo " + Recebimento.MAXIMO_ANIMAIS + " animais");
        if (loteDestino != null) {
            loteDestino.exigirAptoAReceber(ocorridoEm);
        }
        InstalacaoAlocavel instalacao = destino.instalacaoId() == null
                ? null : instalacaoDaPropriedade(org, propriedadeId, destino.instalacaoId());

        Map<UUID, Animal> encontrados = animais.buscarVarios(org, ids).stream()
                .collect(Collectors.toMap(Animal::id, Function.identity()));
        List<Animal> selecionados = new ArrayList<>();
        for (UUID id : ids) {
            Animal a = encontrados.get(id);
            if (a == null || !a.propriedadeId().equals(propriedadeId)) {
                throw new NaoEncontradoException("Animal " + id);
            }
            selecionados.add(a);
        }
        selecionados.sort(Comparator.comparingLong(Animal::numero));

        if (instalacao != null) {
            long entrando = selecionados.stream()
                    .filter(a -> !a.instalacaoAtual().map(instalacao.id()::equals).orElse(false))
                    .count();
            PoliticaLotacao.verificar(instalacao.codigo(), instalacao.capacidadeCabecas(),
                    animais.contarNaInstalacao(instalacao.id()), entrando);
        }

        UUID movimentacaoId = UUID.randomUUID();
        List<AlteracaoAlocacao> itens = selecionados.stream()
                .map(a -> a.realocar(destino, movimentacaoId, ocorridoEm))
                .toList();
        movimentacoes.salvar(Movimentacao.registrar(movimentacaoId, org, propriedadeId, tipo, motivo, ocorridoEm,
                agora, ctx.usuarioId(), itens));
        selecionados.forEach(animais::salvar);
        return movimentacaoId;
    }

    private InstalacaoAlocavel instalacaoDaPropriedade(UUID org, UUID propriedadeId, UUID instalacaoId) {
        InstalacaoAlocavel instalacao = propriedades.instalacaoComBloqueio(org, Verificar.obrigatorio(instalacaoId,
                        "Instalação"))
                .filter(i -> i.propriedadeId().equals(propriedadeId))
                .orElseThrow(() -> new NaoEncontradoException("Instalação"));
        Verificar.regra(instalacao.ativa(), "Instalação " + instalacao.codigo() + " está inativa");
        return instalacao;
    }

    private Lote loteDaPropriedade(UUID org, UUID propriedadeId, UUID loteId) {
        return lotes.buscarLoteComBloqueio(org, loteId)
                .filter(l -> l.propriedadeId().equals(propriedadeId))
                .orElseThrow(() -> new NaoEncontradoException("Lote"));
    }

    private void exigirIdentificacoesLivres(UUID org, TipoIdentificacao tipo, List<String> valores) {
        List<String> normalizados = valores.stream()
                .filter(v -> v != null && !v.isBlank())
                .map(v -> v.strip().toLowerCase(Locale.ROOT))
                .toList();
        Set<String> repetidos = normalizados.stream()
                .filter(v -> normalizados.indexOf(v) != normalizados.lastIndexOf(v))
                .collect(Collectors.toCollection(java.util.TreeSet::new));
        if (!repetidos.isEmpty()) {
            throw new RegraNegocioException("Identificação (" + tipo + ") repetida no recebimento: "
                    + String.join(", ", repetidos));
        }
        Set<String> emUso = animais.identificacoesEmUso(org, tipo, normalizados);
        if (!emUso.isEmpty()) {
            throw new ConflitoException("Identificação (" + tipo + ") já usada por animal ativo: "
                    + String.join(", ", new java.util.TreeSet<>(emUso)));
        }
    }

    private RecebimentoRegistrado resultadoDoRecebimento(UUID org, UUID recebimentoId) {
        Recebimento r = movimentacoes.buscarRecebimento(org, recebimentoId)
                .orElseThrow(() -> new NaoEncontradoException("Recebimento"));
        List<AnimalRecebido> recebidos = animais.listarPorRecebimento(org, recebimentoId).stream()
                .sorted(Comparator.comparingLong(Animal::numero))
                .map(RebanhoService::recebido)
                .toList();
        return new RecebimentoRegistrado(r.id(), r.movimentacaoId(), recebidos);
    }

    private static AnimalRecebido recebido(Animal a) {
        return new AnimalRecebido(a.id(), a.numero(), a.identificacaoAtiva(TipoIdentificacao.BRINCO).orElse(null));
    }

    private static String motivoRecebimento(ReceberAnimaisComando cmd) {
        String base = "Recebimento de animais";
        return cmd.documento() == null || cmd.documento().isBlank() ? base : base + " - doc. " + cmd.documento().strip();
    }

    static <E extends Enum<E>> E enumObrigatorio(Class<E> tipo, String valor, String campo) {
        Verificar.obrigatorio(valor, campo);
        try {
            return Enum.valueOf(tipo, valor);
        } catch (IllegalArgumentException e) {
            throw new RegraNegocioException(campo + " inválido(a): " + valor);
        }
    }

    static <E extends Enum<E>> E enumOuPadrao(Class<E> tipo, String valor, E padrao, String campo) {
        return valor == null || valor.isBlank() ? padrao : enumObrigatorio(tipo, valor, campo);
    }
}
