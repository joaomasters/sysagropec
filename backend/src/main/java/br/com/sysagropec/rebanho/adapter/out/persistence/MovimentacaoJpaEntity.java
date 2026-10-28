package br.com.sysagropec.rebanho.adapter.out.persistence;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Registro imutável: nenhum campo é atualizável. */
@Entity
@Table(name = "movimentacao")
class MovimentacaoJpaEntity {

    @Id
    UUID id;
    @Column(name = "organizacao_id", updatable = false)
    UUID organizacaoId;
    @Column(name = "propriedade_id", updatable = false)
    UUID propriedadeId;
    @Column(updatable = false)
    String tipo;
    @Column(updatable = false)
    String motivo;
    @Column(name = "ocorrido_em", updatable = false)
    Instant ocorridoEm;
    @Column(name = "registrado_em", updatable = false)
    Instant registradoEm;
    @Column(name = "responsavel_id", updatable = false)
    UUID responsavelId;

    @ElementCollection
    @CollectionTable(name = "movimentacao_item", joinColumns = @JoinColumn(name = "movimentacao_id"))
    List<Item> itens = new ArrayList<>();

    @Embeddable
    static class Item {
        @Column(name = "animal_id")
        UUID animalId;
        @Column(name = "lote_origem_id")
        UUID loteOrigemId;
        @Column(name = "lote_destino_id")
        UUID loteDestinoId;
        @Column(name = "instalacao_origem_id")
        UUID instalacaoOrigemId;
        @Column(name = "instalacao_destino_id")
        UUID instalacaoDestinoId;
    }
}
