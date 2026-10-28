package br.com.sysagropec.rebanho.domain.model;

import br.com.sysagropec.rebanho.domain.model.Enums.FaseLote;
import br.com.sysagropec.rebanho.domain.model.Enums.Finalidade;
import br.com.sysagropec.rebanho.domain.model.Enums.StatusLote;
import br.com.sysagropec.rebanho.domain.service.PoliticaLotacao;
import br.com.sysagropec.shared.domain.RegraNegocioException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LoteTest {

    static final Instant INICIO = Instant.parse("2026-10-01T10:00:00Z");

    Lote lote() {
        return Lote.abrir(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), " L-01 ", FaseLote.ADAPTACAO,
                Finalidade.ENGORDA, null, INICIO, INICIO);
    }

    @Test
    void codigoEhNormalizado() {
        assertThat(lote().codigo()).isEqualTo("L-01");
    }

    @Test
    void encerramentoExigeLoteVazio() {
        Lote l = lote();

        assertThatThrownBy(() -> l.encerrar(INICIO.plusSeconds(10), 3))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("Retire os 3 animais");
        l.encerrar(INICIO.plusSeconds(10), 0);
        assertThat(l.status()).isEqualTo(StatusLote.ENCERRADO);
        assertThat(l.fim()).isEqualTo(INICIO.plusSeconds(10));
    }

    @Test
    void loteEncerradoOuAnteriorAoInicioNaoRecebeAnimais() {
        Lote l = lote();

        assertThatThrownBy(() -> l.exigirAptoAReceber(INICIO.minusSeconds(1))).isInstanceOf(RegraNegocioException.class);
        l.encerrar(INICIO, 0);
        assertThatThrownBy(() -> l.exigirAptoAReceber(INICIO.plusSeconds(1)))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("encerrado");
    }

    @Test
    void lotacaoConsideraOcupacaoAtualEEntrada() {
        assertThatCode(() -> PoliticaLotacao.verificar("C1", 10, 7, 3)).doesNotThrowAnyException();
        assertThatThrownBy(() -> PoliticaLotacao.verificar("C1", 10, 8, 3))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("capacidade 10");
        assertThatCode(() -> PoliticaLotacao.verificar("P1", null, 1000, 50)).doesNotThrowAnyException();
    }
}
