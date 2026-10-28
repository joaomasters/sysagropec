package br.com.sysagropec.rebanho.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "animal_identificacao")
class IdentificacaoJpaEntity {

    @Id
    UUID id;
    @Column(name = "organizacao_id", nullable = false, updatable = false)
    UUID organizacaoId;
    @Column(updatable = false)
    String tipo;
    @Column(updatable = false)
    String valor;
    @Column(updatable = false)
    Instant inicio;
    Instant fim;
}
