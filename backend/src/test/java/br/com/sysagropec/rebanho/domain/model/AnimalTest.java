package br.com.sysagropec.rebanho.domain.model;

import br.com.sysagropec.rebanho.domain.model.Enums.Categoria;
import br.com.sysagropec.rebanho.domain.model.Enums.Finalidade;
import br.com.sysagropec.rebanho.domain.model.Enums.OrigemAnimal;
import br.com.sysagropec.rebanho.domain.model.Enums.Sexo;
import br.com.sysagropec.rebanho.domain.model.Enums.TipoIdentificacao;
import br.com.sysagropec.shared.domain.ConflitoException;
import br.com.sysagropec.shared.domain.RegraNegocioException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AnimalTest {

    static final Instant ENTRADA = Instant.parse("2026-10-01T12:00:00Z");
    static final UUID CURRAL_1 = UUID.randomUUID();
    static final UUID CURRAL_2 = UUID.randomUUID();
    static final UUID LOTE_A = UUID.randomUUID();
    static final UUID LOTE_B = UUID.randomUUID();

    static Animal.DadosEntrada dados(String brinco) {
        return new Animal.DadosEntrada(brinco, null, Sexo.MACHO, Categoria.GARROTE, Finalidade.ENGORDA, "Nelore",
                LocalDate.of(2025, 3, 1), true, OrigemAnimal.COMPRA, "Fornecedor");
    }

    static Animal novo(UUID loteId) {
        return Animal.registrarEntrada(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 1, dados("B-1"),
                UUID.randomUUID(), UUID.randomUUID(), CURRAL_1, loteId, ENTRADA, LocalDate.of(2026, 10, 1), ENTRADA);
    }

    @Test
    void entradaAbreLocalizacaoLoteEIdentificacao() {
        Animal a = novo(LOTE_A);

        assertThat(a.instalacaoAtual()).contains(CURRAL_1);
        assertThat(a.loteAtual()).contains(LOTE_A);
        assertThat(a.identificacaoAtiva(TipoIdentificacao.BRINCO)).contains("B-1");
        assertThat(a.ativo()).isTrue();
    }

    @Test
    void categoriaDeveSerCompativelComSexo() {
        var dados = new Animal.DadosEntrada("B-2", null, Sexo.FEMEA, Categoria.BOI, Finalidade.ENGORDA, null, null,
                false, OrigemAnimal.COMPRA, null);

        assertThatThrownBy(() -> Animal.registrarEntrada(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 1,
                dados, null, UUID.randomUUID(), CURRAL_1, null, ENTRADA, LocalDate.of(2026, 10, 1), ENTRADA))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("incompatível");
    }

    @Test
    void nascimentoNaoPodeSerPosteriorAEntrada() {
        var dados = new Animal.DadosEntrada("B-3", null, Sexo.MACHO, Categoria.BEZERRO, Finalidade.RECRIA, null,
                LocalDate.of(2026, 10, 2), false, OrigemAnimal.COMPRA, null);

        assertThatThrownBy(() -> Animal.registrarEntrada(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 1,
                dados, null, UUID.randomUUID(), CURRAL_1, null, ENTRADA, LocalDate.of(2026, 10, 1), ENTRADA))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("nascimento");
    }

    @Test
    void realocarEncerraVinculosAnterioresSemApagarHistorico() {
        Animal a = novo(LOTE_A);
        Instant quando = ENTRADA.plusSeconds(3600);

        AlteracaoAlocacao alteracao = a.realocar(new Animal.Destino(LOTE_B, false, CURRAL_2), UUID.randomUUID(), quando);

        assertThat(alteracao.loteOrigemId()).isEqualTo(LOTE_A);
        assertThat(alteracao.loteDestinoId()).isEqualTo(LOTE_B);
        assertThat(alteracao.instalacaoDestinoId()).isEqualTo(CURRAL_2);
        assertThat(a.loteAtual()).contains(LOTE_B);
        assertThat(a.instalacaoAtual()).contains(CURRAL_2);
        assertThat(a.vinculosLote()).hasSize(2);
        assertThat(a.vinculosLote().getFirst().fim()).isEqualTo(quando);
        assertThat(a.localizacoes()).hasSize(2).filteredOn(Vinculo::aberto).hasSize(1);
    }

    @Test
    void mudarSomenteDeLoteMantemLocalizacao() {
        Animal a = novo(null);

        a.realocar(new Animal.Destino(LOTE_A, false, null), UUID.randomUUID(), ENTRADA.plusSeconds(60));

        assertThat(a.loteAtual()).contains(LOTE_A);
        assertThat(a.localizacoes()).hasSize(1);
        assertThat(a.instalacaoAtual()).contains(CURRAL_1);
    }

    @Test
    void retirarDoLoteEncerraVinculoSemAbrirOutro() {
        Animal a = novo(LOTE_A);

        a.realocar(new Animal.Destino(null, true, null), UUID.randomUUID(), ENTRADA.plusSeconds(60));

        assertThat(a.loteAtual()).isEmpty();
        assertThat(a.vinculosLote()).singleElement().satisfies(v -> assertThat(v.aberto()).isFalse());
    }

    @Test
    void movimentacaoRetroativaEhRejeitada() {
        Animal a = novo(LOTE_A);
        a.realocar(new Animal.Destino(LOTE_B, false, null), UUID.randomUUID(), ENTRADA.plusSeconds(7200));

        assertThatThrownBy(() -> a.realocar(new Animal.Destino(null, false, CURRAL_2), UUID.randomUUID(),
                ENTRADA.plusSeconds(3600)))
                .isInstanceOf(ConflitoException.class)
                .hasMessageContaining("retroativos");
    }

    @Test
    void movimentacaoParaODestinoAtualEhRejeitada() {
        Animal a = novo(LOTE_A);

        assertThatThrownBy(() -> a.realocar(new Animal.Destino(LOTE_A, false, CURRAL_1), UUID.randomUUID(),
                ENTRADA.plusSeconds(60)))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("já está no destino");
    }

    @Test
    void destinoExigeAlgumaAlteracao() {
        assertThatThrownBy(() -> new Animal.Destino(null, false, null)).isInstanceOf(RegraNegocioException.class);
        assertThatThrownBy(() -> new Animal.Destino(LOTE_A, true, null)).isInstanceOf(RegraNegocioException.class);
    }
}
