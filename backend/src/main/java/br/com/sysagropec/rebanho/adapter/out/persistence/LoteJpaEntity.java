package br.com.sysagropec.rebanho.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "lote")
class LoteJpaEntity {

    @Id
    UUID id;
    @Column(name = "organizacao_id", nullable = false, updatable = false)
    UUID organizacaoId;
    @Column(name = "propriedade_id", nullable = false, updatable = false)
    UUID propriedadeId;
    String codigo;
    String fase;
    String finalidade;
    String status;
    Instant inicio;
    Instant fim;
    String observacao;
    @Column(name = "criado_em", updatable = false)
    Instant criadoEm;
    @Column(name = "atualizado_em")
    Instant atualizadoEm;
    @Version
    Long versao;
}
