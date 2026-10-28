package br.com.sysagropec.identidade.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "token_senha")
class TokenSenhaJpaEntity {

    @Id
    UUID id;
    @Column(name = "usuario_id", nullable = false, updatable = false)
    UUID usuarioId;
    String finalidade;
    @Column(name = "token_hash", updatable = false)
    String tokenHash;
    @Column(name = "criado_em", updatable = false)
    Instant criadoEm;
    @Column(name = "expira_em", updatable = false)
    Instant expiraEm;
    @Column(name = "usado_em")
    Instant usadoEm;
    @Column(name = "invalidado_em")
    Instant invalidadoEm;
}
