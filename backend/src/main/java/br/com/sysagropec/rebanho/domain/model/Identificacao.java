package br.com.sysagropec.rebanho.domain.model;

import br.com.sysagropec.rebanho.domain.model.Enums.TipoIdentificacao;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Identificação física ou eletrônica. Substituições preservam a anterior com data de fim. */
public final class Identificacao {

    private final UUID id;
    private final TipoIdentificacao tipo;
    private final String valor;
    private final Instant inicio;
    private final Instant fim;

    private Identificacao(UUID id, TipoIdentificacao tipo, String valor, Instant inicio, Instant fim) {
        this.id = Objects.requireNonNull(id);
        this.tipo = Objects.requireNonNull(tipo);
        this.valor = Objects.requireNonNull(valor);
        this.inicio = Objects.requireNonNull(inicio);
        this.fim = fim;
    }

    static Identificacao nova(TipoIdentificacao tipo, String valor, Instant inicio) {
        return new Identificacao(UUID.randomUUID(), tipo, valor, inicio, null);
    }

    public static Identificacao reconstituir(UUID id, TipoIdentificacao tipo, String valor, Instant inicio, Instant fim) {
        return new Identificacao(id, tipo, valor, inicio, fim);
    }

    public boolean ativa() {
        return fim == null;
    }

    public UUID id() { return id; }
    public TipoIdentificacao tipo() { return tipo; }
    public String valor() { return valor; }
    public Instant inicio() { return inicio; }
    public Instant fim() { return fim; }
}
