package br.com.sysagropec.pesagem.domain.model;

import br.com.sysagropec.shared.domain.Momento;
import br.com.sysagropec.shared.domain.RegraNegocioException;
import br.com.sysagropec.shared.domain.Verificar;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Pesagem individual. Distingue peso medido (balança) de estimado (fita, avaliação visual).
 * Não é apagada nem editada: correções são feitas por cancelamento justificado e novo registro.
 */
public class Pesagem {

    public enum Natureza { MEDIDO, ESTIMADO }

    public enum Metodo {
        BALANCA_INDIVIDUAL(Natureza.MEDIDO),
        FITA_BARIMETRICA(Natureza.ESTIMADO),
        VISUAL(Natureza.ESTIMADO),
        OUTRO(null);

        /** Natureza exigida pelo método; nulo quando depende do caso. */
        private final Natureza naturezaExigida;

        Metodo(Natureza naturezaExigida) {
            this.naturezaExigida = naturezaExigida;
        }
    }

    public enum Finalidade { ENTRADA, ACOMPANHAMENTO, SAIDA }

    public static final BigDecimal PESO_MAXIMO_KG = new BigDecimal("2000");

    private final UUID id;
    private final UUID organizacaoId;
    private final UUID propriedadeId;
    private final UUID animalId;
    private final BigDecimal pesoKg;
    private final Natureza natureza;
    private final Metodo metodo;
    private final Finalidade finalidade;
    private final String equipamento;
    private final String observacao;
    private final Instant ocorridoEm;
    private final Instant registradoEm;
    private final UUID responsavelId;
    private final UUID tarefaId;
    private Instant canceladaEm;
    private UUID canceladaPor;
    private String motivoCancelamento;
    private final Long versao;

    private Pesagem(UUID id, UUID organizacaoId, UUID propriedadeId, UUID animalId, BigDecimal pesoKg,
                    Natureza natureza, Metodo metodo, Finalidade finalidade, String equipamento, String observacao,
                    Instant ocorridoEm, Instant registradoEm, UUID responsavelId, UUID tarefaId, Instant canceladaEm,
                    UUID canceladaPor, String motivoCancelamento, Long versao) {
        this.id = Objects.requireNonNull(id);
        this.organizacaoId = Objects.requireNonNull(organizacaoId);
        this.propriedadeId = Objects.requireNonNull(propriedadeId);
        this.animalId = Objects.requireNonNull(animalId);
        this.pesoKg = pesoKg;
        this.natureza = natureza;
        this.metodo = metodo;
        this.finalidade = finalidade;
        this.equipamento = equipamento;
        this.observacao = observacao;
        this.ocorridoEm = ocorridoEm;
        this.registradoEm = registradoEm;
        this.responsavelId = responsavelId;
        this.tarefaId = tarefaId;
        this.canceladaEm = canceladaEm;
        this.canceladaPor = canceladaPor;
        this.motivoCancelamento = motivoCancelamento;
        this.versao = versao;
    }

    /**
     * @param entradaAnimal início da permanência do animal na propriedade; a pesagem não pode ser anterior a ela
     */
    public static Pesagem registrar(UUID id, UUID organizacaoId, UUID propriedadeId, UUID animalId, BigDecimal pesoKg,
                                    Natureza natureza, Metodo metodo, Finalidade finalidade, String equipamento,
                                    String observacao, Instant ocorridoEm, Instant entradaAnimal, Instant agora,
                                    UUID responsavelId, UUID tarefaId) {
        Momento.exigirNaoFuturo(ocorridoEm, agora, "Data da pesagem");
        Verificar.regra(entradaAnimal == null || !ocorridoEm.isBefore(entradaAnimal),
                "A pesagem não pode ser anterior à entrada do animal na propriedade");
        validarPeso(pesoKg);
        Verificar.obrigatorio(natureza, "Natureza do peso");
        Verificar.obrigatorio(metodo, "Método");
        Verificar.obrigatorio(finalidade, "Finalidade");
        if (metodo.naturezaExigida != null && metodo.naturezaExigida != natureza) {
            throw new RegraNegocioException("Pesagem pelo método " + metodo + " deve ser registrada como "
                    + metodo.naturezaExigida);
        }
        return new Pesagem(id, organizacaoId, propriedadeId, animalId, pesoKg.setScale(2), natureza, metodo,
                finalidade, Verificar.textoOpcional(equipamento, "Equipamento", 80),
                Verificar.textoOpcional(observacao, "Observação", 500), ocorridoEm, agora,
                Verificar.obrigatorio(responsavelId, "Responsável"), tarefaId, null, null, null, null);
    }

    public static Pesagem reconstituir(UUID id, UUID organizacaoId, UUID propriedadeId, UUID animalId,
                                       BigDecimal pesoKg, Natureza natureza, Metodo metodo, Finalidade finalidade,
                                       String equipamento, String observacao, Instant ocorridoEm,
                                       Instant registradoEm, UUID responsavelId, UUID tarefaId, Instant canceladaEm,
                                       UUID canceladaPor, String motivoCancelamento, Long versao) {
        return new Pesagem(id, organizacaoId, propriedadeId, animalId, pesoKg, natureza, metodo, finalidade,
                equipamento, observacao, ocorridoEm, registradoEm, responsavelId, tarefaId, canceladaEm, canceladaPor,
                motivoCancelamento, versao);
    }

    private static void validarPeso(BigDecimal pesoKg) {
        Verificar.obrigatorio(pesoKg, "Peso");
        Verificar.regra(pesoKg.signum() > 0, "O peso deve ser maior que zero");
        Verificar.regra(pesoKg.compareTo(PESO_MAXIMO_KG) <= 0, "Peso acima de " + PESO_MAXIMO_KG + " kg");
        Verificar.regra(pesoKg.stripTrailingZeros().scale() <= 2, "Informe o peso com no máximo 2 casas decimais");
    }

    public void cancelar(UUID usuarioId, String motivo, Instant agora) {
        Verificar.regra(canceladaEm == null, "Pesagem já cancelada");
        this.motivoCancelamento = Verificar.texto(motivo, "Motivo do cancelamento", 300);
        this.canceladaPor = Verificar.obrigatorio(usuarioId, "Usuário");
        this.canceladaEm = agora;
    }

    public boolean cancelada() {
        return canceladaEm != null;
    }

    public UUID id() { return id; }
    public UUID organizacaoId() { return organizacaoId; }
    public UUID propriedadeId() { return propriedadeId; }
    public UUID animalId() { return animalId; }
    public BigDecimal pesoKg() { return pesoKg; }
    public Natureza natureza() { return natureza; }
    public Metodo metodo() { return metodo; }
    public Finalidade finalidade() { return finalidade; }
    public String equipamento() { return equipamento; }
    public String observacao() { return observacao; }
    public Instant ocorridoEm() { return ocorridoEm; }
    public Instant registradoEm() { return registradoEm; }
    public UUID responsavelId() { return responsavelId; }
    public UUID tarefaId() { return tarefaId; }
    public Instant canceladaEm() { return canceladaEm; }
    public UUID canceladaPor() { return canceladaPor; }
    public String motivoCancelamento() { return motivoCancelamento; }
    public Long versao() { return versao; }
}
