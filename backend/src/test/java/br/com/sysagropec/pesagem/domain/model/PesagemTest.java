package br.com.sysagropec.pesagem.domain.model;

import br.com.sysagropec.shared.domain.RegraNegocioException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PesagemTest {

    static final Instant AGORA = Instant.parse("2026-10-20T12:00:00Z");
    static final Instant ENTRADA = Instant.parse("2026-10-01T12:00:00Z");

    static Pesagem registrar(String peso, Pesagem.Natureza natureza, Pesagem.Metodo metodo, Instant quando) {
        return Pesagem.registrar(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                new BigDecimal(peso), natureza, metodo, Pesagem.Finalidade.ACOMPANHAMENTO, null, null, quando,
                ENTRADA, AGORA, UUID.randomUUID(), null);
    }

    @Test
    void registraPesoMedidoComDuasCasas() {
        Pesagem p = registrar("412.5", Pesagem.Natureza.MEDIDO, Pesagem.Metodo.BALANCA_INDIVIDUAL, AGORA);

        assertThat(p.pesoKg()).isEqualByComparingTo("412.50");
        assertThat(p.pesoKg().scale()).isEqualTo(2);
        assertThat(p.registradoEm()).isEqualTo(AGORA);
    }

    @Test
    void metodoDefineANaturezaExigida() {
        assertThatThrownBy(() -> registrar("400", Pesagem.Natureza.MEDIDO, Pesagem.Metodo.VISUAL, AGORA))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("ESTIMADO");
        assertThatThrownBy(() -> registrar("400", Pesagem.Natureza.ESTIMADO, Pesagem.Metodo.BALANCA_INDIVIDUAL, AGORA))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("MEDIDO");
        assertThat(registrar("400", Pesagem.Natureza.ESTIMADO, Pesagem.Metodo.OUTRO, AGORA).natureza())
                .isEqualTo(Pesagem.Natureza.ESTIMADO);
    }

    @Test
    void pesoForaDosLimitesEhRejeitado() {
        assertThatThrownBy(() -> registrar("0", Pesagem.Natureza.MEDIDO, Pesagem.Metodo.BALANCA_INDIVIDUAL, AGORA))
                .isInstanceOf(RegraNegocioException.class);
        assertThatThrownBy(() -> registrar("2000.01", Pesagem.Natureza.MEDIDO, Pesagem.Metodo.BALANCA_INDIVIDUAL, AGORA))
                .isInstanceOf(RegraNegocioException.class);
        assertThatThrownBy(() -> registrar("400.123", Pesagem.Natureza.MEDIDO, Pesagem.Metodo.BALANCA_INDIVIDUAL, AGORA))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("2 casas");
    }

    @Test
    void naoAceitaDataFuturaNemAnteriorAEntrada() {
        assertThatThrownBy(() -> registrar("400", Pesagem.Natureza.MEDIDO, Pesagem.Metodo.BALANCA_INDIVIDUAL,
                AGORA.plusSeconds(3600)))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("futuro");
        assertThatThrownBy(() -> registrar("400", Pesagem.Natureza.MEDIDO, Pesagem.Metodo.BALANCA_INDIVIDUAL,
                ENTRADA.minusSeconds(1)))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("entrada");
    }

    @Test
    void cancelamentoExigeMotivoEOcorreUmaVez() {
        Pesagem p = registrar("400", Pesagem.Natureza.MEDIDO, Pesagem.Metodo.BALANCA_INDIVIDUAL, AGORA);

        assertThatThrownBy(() -> p.cancelar(UUID.randomUUID(), " ", AGORA)).isInstanceOf(RegraNegocioException.class);
        p.cancelar(UUID.randomUUID(), "Balança descalibrada", AGORA);
        assertThat(p.cancelada()).isTrue();
        assertThatThrownBy(() -> p.cancelar(UUID.randomUUID(), "De novo", AGORA))
                .isInstanceOf(RegraNegocioException.class);
    }
}
