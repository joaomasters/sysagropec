package br.com.sysagropec.identidade.domain.model;

import br.com.sysagropec.shared.domain.RegraNegocioException;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Token de uso único para definir a senha. Apenas o hash é persistido;
 * o valor em claro existe somente no link enviado ao usuário.
 */
public class TokenSenha {

    public enum Finalidade {
        REDEFINICAO(Duration.ofMinutes(30)),
        CONVITE(Duration.ofHours(72));

        private final Duration validade;

        Finalidade(Duration validade) {
            this.validade = validade;
        }

        public Duration validade() {
            return validade;
        }
    }

    private final UUID id;
    private final UUID usuarioId;
    private final Finalidade finalidade;
    private final String tokenHash;
    private final Instant criadoEm;
    private final Instant expiraEm;
    private Instant usadoEm;

    private TokenSenha(UUID id, UUID usuarioId, Finalidade finalidade, String tokenHash, Instant criadoEm,
                       Instant expiraEm, Instant usadoEm) {
        this.id = Objects.requireNonNull(id);
        this.usuarioId = Objects.requireNonNull(usuarioId);
        this.finalidade = Objects.requireNonNull(finalidade);
        this.tokenHash = Objects.requireNonNull(tokenHash);
        this.criadoEm = criadoEm;
        this.expiraEm = expiraEm;
        this.usadoEm = usadoEm;
    }

    public static TokenSenha emitir(UUID usuarioId, Finalidade finalidade, String tokenHash, Instant agora) {
        return new TokenSenha(UUID.randomUUID(), usuarioId, finalidade, tokenHash, agora,
                agora.plus(finalidade.validade()), null);
    }

    public static TokenSenha reconstituir(UUID id, UUID usuarioId, Finalidade finalidade, String tokenHash,
                                          Instant criadoEm, Instant expiraEm, Instant usadoEm) {
        return new TokenSenha(id, usuarioId, finalidade, tokenHash, criadoEm, expiraEm, usadoEm);
    }

    public void usar(Instant agora) {
        if (usadoEm != null || !agora.isBefore(expiraEm)) {
            throw new RegraNegocioException("Link inválido ou expirado. Solicite um novo");
        }
        this.usadoEm = agora;
    }

    public UUID id() { return id; }
    public UUID usuarioId() { return usuarioId; }
    public Finalidade finalidade() { return finalidade; }
    public String tokenHash() { return tokenHash; }
    public Instant criadoEm() { return criadoEm; }
    public Instant expiraEm() { return expiraEm; }
    public Instant usadoEm() { return usadoEm; }
}
