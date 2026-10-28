package br.com.sysagropec.shared.domain;

import java.time.Duration;
import java.time.Instant;

/** Regras sobre o horário do acontecimento informado pelo usuário (distinto do horário de lançamento). */
public final class Momento {

    /** Tolerância para diferenças de relógio entre o dispositivo do usuário e o servidor. */
    public static final Duration TOLERANCIA_RELOGIO = Duration.ofMinutes(5);

    private Momento() {
    }

    public static Instant exigirNaoFuturo(Instant ocorridoEm, Instant agora, String campo) {
        Verificar.obrigatorio(ocorridoEm, campo);
        Verificar.regra(!ocorridoEm.isAfter(agora.plus(TOLERANCIA_RELOGIO)), campo + " não pode estar no futuro");
        return ocorridoEm;
    }
}
