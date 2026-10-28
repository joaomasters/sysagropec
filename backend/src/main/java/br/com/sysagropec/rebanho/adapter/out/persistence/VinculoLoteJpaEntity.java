package br.com.sysagropec.rebanho.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "animal_lote_vinculo")
class VinculoLoteJpaEntity {

    @Id
    UUID id;
    @Column(name = "organizacao_id", nullable = false, updatable = false)
    UUID organizacaoId;
    @Column(name = "lote_id", nullable = false, updatable = false)
    UUID loteId;
    @Column(name = "movimentacao_id", nullable = false, updatable = false)
    UUID movimentacaoId;
    @Column(updatable = false)
    Instant inicio;
    Instant fim;
}
