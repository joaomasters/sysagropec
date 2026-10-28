package br.com.sysagropec.rebanho.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "recebimento")
class RecebimentoJpaEntity {

    @Id
    UUID id;
    @Column(name = "organizacao_id", updatable = false)
    UUID organizacaoId;
    @Column(name = "propriedade_id", updatable = false)
    UUID propriedadeId;
    @Column(name = "instalacao_id", updatable = false)
    UUID instalacaoId;
    @Column(name = "lote_id", updatable = false)
    UUID loteId;
    @Column(name = "movimentacao_id", updatable = false)
    UUID movimentacaoId;
    @Column(name = "ocorrido_em", updatable = false)
    Instant ocorridoEm;
    @Column(name = "registrado_em", updatable = false)
    Instant registradoEm;
    @Column(name = "origem_descricao", updatable = false)
    String origemDescricao;
    @Column(updatable = false)
    String documento;
    @Column(updatable = false)
    String observacao;
    @Column(updatable = false)
    int quantidade;
    @Column(name = "responsavel_id", updatable = false)
    UUID responsavelId;
}
