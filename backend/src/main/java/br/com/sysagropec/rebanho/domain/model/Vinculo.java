package br.com.sysagropec.rebanho.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Período em que o animal esteve associado a um lote ou a uma instalação.
 * Vínculos não são apagados: ao mudar, o atual é encerrado e um novo é aberto.
 */
public final class Vinculo {

    private final UUID id;
    private final UUID alvoId;
    private final UUID movimentacaoId;
    private final Instant inicio;
    private Instant fim;

    private Vinculo(UUID id, UUID alvoId, UUID movimentacaoId, Instant inicio, Instant fim) {
        this.id = Objects.requireNonNull(id);
        this.alvoId = Objects.requireNonNull(alvoId);
        this.movimentacaoId = Objects.requireNonNull(movimentacaoId);
        this.inicio = Objects.requireNonNull(inicio);
        this.fim = fim;
    }

    static Vinculo abrir(UUID alvoId, UUID movimentacaoId, Instant inicio) {
        return new Vinculo(UUID.randomUUID(), alvoId, movimentacaoId, inicio, null);
    }

    public static Vinculo reconstituir(UUID id, UUID alvoId, UUID movimentacaoId, Instant inicio, Instant fim) {
        return new Vinculo(id, alvoId, movimentacaoId, inicio, fim);
    }

    void encerrar(Instant quando) {
        if (fim != null) {
            throw new IllegalStateException("Vínculo já encerrado");
        }
        this.fim = quando;
    }

    public boolean aberto() {
        return fim == null;
    }

    public UUID id() { return id; }
    public UUID alvoId() { return alvoId; }
    public UUID movimentacaoId() { return movimentacaoId; }
    public Instant inicio() { return inicio; }
    public Instant fim() { return fim; }
}
