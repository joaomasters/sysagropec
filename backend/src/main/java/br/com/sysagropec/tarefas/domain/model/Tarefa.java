package br.com.sysagropec.tarefas.domain.model;

import br.com.sysagropec.shared.domain.RegraNegocioException;
import br.com.sysagropec.shared.domain.Verificar;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Tarefa de manejo com checklist, autoria e evidência do registro técnico produzido.
 * O atraso é calculado (prazo vencido com a tarefa aberta) e não substitui o estado operacional.
 */
public class Tarefa {

    public enum Tipo {
        TRATO, LEITURA_COCHO, LIMPEZA, INSPECAO_AGUA, PESAGEM, PROCEDIMENTO_SANITARIO, MANUTENCAO, OUTRA;

        /** Tipo de registro técnico que comprova a execução; nulo quando a conclusão manual basta. */
        public String resultadoExigido() {
            return this == PESAGEM ? "PESAGEM" : null;
        }
    }

    public enum Status {
        PENDENTE, EM_ANDAMENTO, CONCLUIDA, IMPEDIDA, CANCELADA;

        public boolean aberta() {
            return this == PENDENTE || this == EM_ANDAMENTO;
        }
    }

    public enum Prioridade { BAIXA, MEDIA, ALTA }

    public enum TipoAlvo { PROPRIEDADE, INSTALACAO, LOTE, ANIMAL }

    public record Alvo(TipoAlvo tipo, UUID id) {
        public Alvo {
            Verificar.obrigatorio(tipo, "Tipo do alvo");
            Verificar.obrigatorio(id, "Alvo");
        }
    }

    public record Resultado(String tipo, UUID id, Instant vinculadoEm) {
    }

    public static final class ItemChecklist {
        private final UUID id;
        private final int ordem;
        private final String descricao;
        private final boolean obrigatorio;
        private boolean concluido;
        private UUID concluidoPor;
        private Instant concluidoEm;

        private ItemChecklist(UUID id, int ordem, String descricao, boolean obrigatorio, boolean concluido,
                              UUID concluidoPor, Instant concluidoEm) {
            this.id = id;
            this.ordem = ordem;
            this.descricao = descricao;
            this.obrigatorio = obrigatorio;
            this.concluido = concluido;
            this.concluidoPor = concluidoPor;
            this.concluidoEm = concluidoEm;
        }

        public static ItemChecklist reconstituir(UUID id, int ordem, String descricao, boolean obrigatorio,
                                                 boolean concluido, UUID concluidoPor, Instant concluidoEm) {
            return new ItemChecklist(id, ordem, descricao, obrigatorio, concluido, concluidoPor, concluidoEm);
        }

        private void marcar(UUID usuarioId, Instant agora) {
            if (!concluido) {
                concluido = true;
                concluidoPor = usuarioId;
                concluidoEm = agora;
            }
        }

        public UUID id() { return id; }
        public int ordem() { return ordem; }
        public String descricao() { return descricao; }
        public boolean obrigatorio() { return obrigatorio; }
        public boolean concluido() { return concluido; }
        public UUID concluidoPor() { return concluidoPor; }
        public Instant concluidoEm() { return concluidoEm; }
    }

    public record NovoItem(String descricao, boolean obrigatorio) {
    }

    public static final int MAXIMO_ITENS_CHECKLIST = 50;

    private final UUID id;
    private final UUID organizacaoId;
    private final UUID propriedadeId;
    private final Tipo tipo;
    private final String titulo;
    private final String descricao;
    private final Alvo alvo;
    private final UUID responsavelId;
    private final Instant prazo;
    private final Prioridade prioridade;
    private Status status;
    private final UUID criadaPor;
    private final Instant criadaEm;
    private Instant iniciadaEm;
    private UUID iniciadaPor;
    private Instant finalizadaEm;
    private UUID finalizadaPor;
    private String observacaoConclusao;
    private String justificativa;
    private final List<ItemChecklist> checklist;
    private final List<Resultado> resultados;
    private final Long versao;

    private Tarefa(UUID id, UUID organizacaoId, UUID propriedadeId, Tipo tipo, String titulo, String descricao,
                   Alvo alvo, UUID responsavelId, Instant prazo, Prioridade prioridade, Status status, UUID criadaPor,
                   Instant criadaEm, Instant iniciadaEm, UUID iniciadaPor, Instant finalizadaEm, UUID finalizadaPor,
                   String observacaoConclusao, String justificativa, List<ItemChecklist> checklist,
                   List<Resultado> resultados, Long versao) {
        this.id = Objects.requireNonNull(id);
        this.organizacaoId = Objects.requireNonNull(organizacaoId);
        this.propriedadeId = Objects.requireNonNull(propriedadeId);
        this.tipo = tipo;
        this.titulo = titulo;
        this.descricao = descricao;
        this.alvo = alvo;
        this.responsavelId = responsavelId;
        this.prazo = prazo;
        this.prioridade = prioridade;
        this.status = status;
        this.criadaPor = criadaPor;
        this.criadaEm = criadaEm;
        this.iniciadaEm = iniciadaEm;
        this.iniciadaPor = iniciadaPor;
        this.finalizadaEm = finalizadaEm;
        this.finalizadaPor = finalizadaPor;
        this.observacaoConclusao = observacaoConclusao;
        this.justificativa = justificativa;
        this.checklist = new ArrayList<>(checklist);
        this.resultados = new ArrayList<>(resultados);
        this.versao = versao;
    }

    public static Tarefa criar(UUID id, UUID organizacaoId, UUID propriedadeId, Tipo tipo, String titulo,
                               String descricao, Alvo alvo, UUID responsavelId, Instant prazo, Prioridade prioridade,
                               List<NovoItem> itens, UUID criadaPor, Instant agora) {
        Verificar.obrigatorio(tipo, "Tipo");
        Verificar.obrigatorio(alvo, "Alvo");
        Verificar.obrigatorio(prazo, "Prazo");
        List<NovoItem> novos = itens == null ? List.of() : itens;
        Verificar.regra(novos.size() <= MAXIMO_ITENS_CHECKLIST,
                "O checklist aceita no máximo " + MAXIMO_ITENS_CHECKLIST + " itens");
        List<ItemChecklist> checklist = new ArrayList<>();
        for (int i = 0; i < novos.size(); i++) {
            checklist.add(new ItemChecklist(UUID.randomUUID(), i + 1,
                    Verificar.texto(novos.get(i).descricao(), "Item do checklist", 200), novos.get(i).obrigatorio(),
                    false, null, null));
        }
        return new Tarefa(id, organizacaoId, propriedadeId, tipo, Verificar.texto(titulo, "Título", 120),
                Verificar.textoOpcional(descricao, "Descrição", 1000), alvo, responsavelId, prazo,
                prioridade == null ? Prioridade.MEDIA : prioridade, Status.PENDENTE, criadaPor, agora, null, null,
                null, null, null, null, checklist, List.of(), null);
    }

    public static Tarefa reconstituir(UUID id, UUID organizacaoId, UUID propriedadeId, Tipo tipo, String titulo,
                                      String descricao, Alvo alvo, UUID responsavelId, Instant prazo,
                                      Prioridade prioridade, Status status, UUID criadaPor, Instant criadaEm,
                                      Instant iniciadaEm, UUID iniciadaPor, Instant finalizadaEm, UUID finalizadaPor,
                                      String observacaoConclusao, String justificativa,
                                      List<ItemChecklist> checklist, List<Resultado> resultados, Long versao) {
        return new Tarefa(id, organizacaoId, propriedadeId, tipo, titulo, descricao, alvo, responsavelId, prazo,
                prioridade, status, criadaPor, criadaEm, iniciadaEm, iniciadaPor, finalizadaEm, finalizadaPor,
                observacaoConclusao, justificativa, checklist, resultados, versao);
    }

    public void iniciar(UUID usuarioId, Instant agora) {
        Verificar.regra(status == Status.PENDENTE, "Só é possível iniciar tarefas pendentes");
        marcarInicio(usuarioId, agora);
    }

    public void concluir(UUID usuarioId, Instant agora, String observacao, Set<UUID> itensMarcados) {
        exigirAberta();
        Set<UUID> marcados = itensMarcados == null ? Set.of() : itensMarcados;
        for (UUID itemId : marcados) {
            Verificar.regra(checklist.stream().anyMatch(i -> i.id().equals(itemId)), "Item de checklist inválido");
        }
        List<String> pendentes = checklist.stream()
                .filter(i -> i.obrigatorio() && !i.concluido() && !marcados.contains(i.id()))
                .map(ItemChecklist::descricao)
                .toList();
        if (!pendentes.isEmpty()) {
            throw new RegraNegocioException("Conclua os itens obrigatórios do checklist: " + String.join("; ", pendentes));
        }
        String exigido = tipo.resultadoExigido();
        if (exigido != null && resultados.stream().noneMatch(r -> r.tipo().equals(exigido))) {
            throw new RegraNegocioException("Registre a " + exigido.toLowerCase() + " a partir da tarefa antes de concluí-la");
        }
        checklist.stream().filter(i -> marcados.contains(i.id())).forEach(i -> i.marcar(usuarioId, agora));
        finalizar(Status.CONCLUIDA, usuarioId, agora);
        this.observacaoConclusao = Verificar.textoOpcional(observacao, "Observação", 1000);
    }

    public void impedir(UUID usuarioId, Instant agora, String justificativa) {
        exigirAberta();
        this.justificativa = Verificar.texto(justificativa, "Justificativa", 500);
        finalizar(Status.IMPEDIDA, usuarioId, agora);
    }

    public void cancelar(UUID usuarioId, Instant agora, String justificativa) {
        exigirAberta();
        this.justificativa = Verificar.texto(justificativa, "Justificativa", 500);
        finalizar(Status.CANCELADA, usuarioId, agora);
    }

    /**
     * Vincula o registro técnico que a tarefa produziu. Se o registro atende exatamente o alvo da tarefa
     * e não há itens obrigatórios pendentes, a tarefa é concluída.
     *
     * @return true se a tarefa foi concluída
     */
    public boolean vincularResultado(String tipoResultado, UUID resultadoId, Alvo alvoDoResultado, UUID usuarioId,
                                     Instant agora) {
        exigirAberta();
        Verificar.regra(Objects.equals(tipo.resultadoExigido(), tipoResultado),
                "Esta tarefa não aceita registros do tipo " + tipoResultado);
        Verificar.regra(alvo.tipo() != TipoAlvo.ANIMAL || alvo.equals(alvoDoResultado),
                "O registro não corresponde ao animal da tarefa");
        Verificar.regra(resultados.stream().noneMatch(r -> r.id().equals(resultadoId)), "Registro já vinculado");
        resultados.add(new Resultado(tipoResultado, resultadoId, agora));
        if (status == Status.PENDENTE) {
            marcarInicio(usuarioId, agora);
        }
        boolean pendenciasChecklist = checklist.stream().anyMatch(i -> i.obrigatorio() && !i.concluido());
        if (alvo.equals(alvoDoResultado) && !pendenciasChecklist) {
            finalizar(Status.CONCLUIDA, usuarioId, agora);
            return true;
        }
        return false;
    }

    public boolean atrasada(Instant agora) {
        return status.aberta() && prazo.isBefore(agora);
    }

    private void exigirAberta() {
        Verificar.regra(status.aberta(), "Tarefa já finalizada (" + status + ")");
    }

    private void marcarInicio(UUID usuarioId, Instant agora) {
        this.status = Status.EM_ANDAMENTO;
        this.iniciadaEm = agora;
        this.iniciadaPor = usuarioId;
    }

    private void finalizar(Status novo, UUID usuarioId, Instant agora) {
        this.status = novo;
        this.finalizadaEm = agora;
        this.finalizadaPor = usuarioId;
    }

    public UUID id() { return id; }
    public UUID organizacaoId() { return organizacaoId; }
    public UUID propriedadeId() { return propriedadeId; }
    public Tipo tipo() { return tipo; }
    public String titulo() { return titulo; }
    public String descricao() { return descricao; }
    public Alvo alvo() { return alvo; }
    public UUID responsavelId() { return responsavelId; }
    public Instant prazo() { return prazo; }
    public Prioridade prioridade() { return prioridade; }
    public Status status() { return status; }
    public UUID criadaPor() { return criadaPor; }
    public Instant criadaEm() { return criadaEm; }
    public Instant iniciadaEm() { return iniciadaEm; }
    public UUID iniciadaPor() { return iniciadaPor; }
    public Instant finalizadaEm() { return finalizadaEm; }
    public UUID finalizadaPor() { return finalizadaPor; }
    public String observacaoConclusao() { return observacaoConclusao; }
    public String justificativa() { return justificativa; }
    public List<ItemChecklist> checklist() { return Collections.unmodifiableList(checklist); }
    public List<Resultado> resultados() { return Collections.unmodifiableList(resultados); }
    public Long versao() { return versao; }
}
