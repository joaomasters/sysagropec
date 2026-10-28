package br.com.sysagropec.pesagem.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Apenas os campos de cancelamento são atualizáveis. */
@Entity
@Table(name = "pesagem")
class PesagemJpaEntity {

    @Id
    UUID id;
    @Column(name = "organizacao_id", updatable = false)
    UUID organizacaoId;
    @Column(name = "propriedade_id", updatable = false)
    UUID propriedadeId;
    @Column(name = "animal_id", updatable = false)
    UUID animalId;
    @Column(name = "peso_kg", updatable = false)
    BigDecimal pesoKg;
    @Column(updatable = false)
    String natureza;
    @Column(updatable = false)
    String metodo;
    @Column(updatable = false)
    String finalidade;
    @Column(updatable = false)
    String equipamento;
    @Column(updatable = false)
    String observacao;
    @Column(name = "ocorrido_em", updatable = false)
    Instant ocorridoEm;
    @Column(name = "registrado_em", updatable = false)
    Instant registradoEm;
    @Column(name = "responsavel_id", updatable = false)
    UUID responsavelId;
    @Column(name = "tarefa_id", updatable = false)
    UUID tarefaId;
    @Column(name = "cancelada_em")
    Instant canceladaEm;
    @Column(name = "cancelada_por")
    UUID canceladaPor;
    @Column(name = "motivo_cancelamento")
    String motivoCancelamento;
    @Version
    Long versao;
}
