package br.com.sysagropec.tarefas.domain.model;

import br.com.sysagropec.shared.domain.RegraNegocioException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TarefaTest {

    static final Instant AGORA = Instant.parse("2026-10-20T12:00:00Z");
    static final UUID USUARIO = UUID.randomUUID();
    static final UUID ANIMAL = UUID.randomUUID();

    static Tarefa tarefa(Tarefa.Tipo tipo, Tarefa.Alvo alvo, List<Tarefa.NovoItem> itens) {
        return Tarefa.criar(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), tipo, "Tarefa", null, alvo,
                null, AGORA.plusSeconds(3600), null, itens, USUARIO, AGORA);
    }

    @Test
    void conclusaoExigeItensObrigatoriosDoChecklist() {
        Tarefa t = tarefa(Tarefa.Tipo.LIMPEZA, new Tarefa.Alvo(Tarefa.TipoAlvo.INSTALACAO, UUID.randomUUID()),
                List.of(new Tarefa.NovoItem("Raspar piso", true), new Tarefa.NovoItem("Foto", false)));
        UUID obrigatorio = t.checklist().getFirst().id();

        assertThatThrownBy(() -> t.concluir(USUARIO, AGORA, null, Set.of()))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("Raspar piso");
        t.concluir(USUARIO, AGORA, "ok", Set.of(obrigatorio));

        assertThat(t.status()).isEqualTo(Tarefa.Status.CONCLUIDA);
        assertThat(t.finalizadaPor()).isEqualTo(USUARIO);
        assertThat(t.checklist().getFirst().concluido()).isTrue();
        assertThat(t.checklist().get(1).concluido()).isFalse();
    }

    @Test
    void tarefaDePesagemSoConcluiComPesagemVinculada() {
        Tarefa t = tarefa(Tarefa.Tipo.PESAGEM, new Tarefa.Alvo(Tarefa.TipoAlvo.LOTE, UUID.randomUUID()), List.of());

        assertThatThrownBy(() -> t.concluir(USUARIO, AGORA, null, Set.of()))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("pesagem");

        boolean concluiu = t.vincularResultado("PESAGEM", UUID.randomUUID(),
                new Tarefa.Alvo(Tarefa.TipoAlvo.ANIMAL, ANIMAL), USUARIO, AGORA);
        assertThat(concluiu).isFalse();
        assertThat(t.status()).isEqualTo(Tarefa.Status.EM_ANDAMENTO);

        t.concluir(USUARIO, AGORA, null, Set.of());
        assertThat(t.status()).isEqualTo(Tarefa.Status.CONCLUIDA);
    }

    @Test
    void pesagemDoProprioAnimalConcluiATarefa() {
        Tarefa.Alvo alvo = new Tarefa.Alvo(Tarefa.TipoAlvo.ANIMAL, ANIMAL);
        Tarefa t = tarefa(Tarefa.Tipo.PESAGEM, alvo, List.of());
        UUID pesagem = UUID.randomUUID();

        assertThat(t.vincularResultado("PESAGEM", pesagem, alvo, USUARIO, AGORA)).isTrue();
        assertThat(t.status()).isEqualTo(Tarefa.Status.CONCLUIDA);
        assertThat(t.resultados()).singleElement().satisfies(r -> assertThat(r.id()).isEqualTo(pesagem));
        assertThatThrownBy(() -> t.vincularResultado("PESAGEM", UUID.randomUUID(), alvo, USUARIO, AGORA))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("finalizada");
    }

    @Test
    void pesagemDeOutroAnimalNaoAtendeTarefaIndividual() {
        Tarefa t = tarefa(Tarefa.Tipo.PESAGEM, new Tarefa.Alvo(Tarefa.TipoAlvo.ANIMAL, ANIMAL), List.of());

        assertThatThrownBy(() -> t.vincularResultado("PESAGEM", UUID.randomUUID(),
                new Tarefa.Alvo(Tarefa.TipoAlvo.ANIMAL, UUID.randomUUID()), USUARIO, AGORA))
                .isInstanceOf(RegraNegocioException.class);
    }

    @Test
    void tarefaQueNaoEhDePesagemNaoAceitaPesagem() {
        Tarefa t = tarefa(Tarefa.Tipo.TRATO, new Tarefa.Alvo(Tarefa.TipoAlvo.LOTE, UUID.randomUUID()), List.of());

        assertThatThrownBy(() -> t.vincularResultado("PESAGEM", UUID.randomUUID(),
                new Tarefa.Alvo(Tarefa.TipoAlvo.ANIMAL, ANIMAL), USUARIO, AGORA))
                .isInstanceOf(RegraNegocioException.class);
    }

    @Test
    void impedimentoECancelamentoExigemJustificativa() {
        Tarefa t = tarefa(Tarefa.Tipo.TRATO, new Tarefa.Alvo(Tarefa.TipoAlvo.LOTE, UUID.randomUUID()), List.of());

        assertThatThrownBy(() -> t.impedir(USUARIO, AGORA, "")).isInstanceOf(RegraNegocioException.class);
        t.impedir(USUARIO, AGORA, "Vagão quebrado");
        assertThat(t.status()).isEqualTo(Tarefa.Status.IMPEDIDA);
        assertThatThrownBy(() -> t.cancelar(USUARIO, AGORA, "x")).isInstanceOf(RegraNegocioException.class);
    }

    @Test
    void atrasoEhCalculadoSemAlterarOEstado() {
        Tarefa t = tarefa(Tarefa.Tipo.TRATO, new Tarefa.Alvo(Tarefa.TipoAlvo.LOTE, UUID.randomUUID()), List.of());

        assertThat(t.atrasada(AGORA)).isFalse();
        assertThat(t.atrasada(AGORA.plusSeconds(7200))).isTrue();
        assertThat(t.status()).isEqualTo(Tarefa.Status.PENDENTE);
        t.impedir(USUARIO, AGORA, "Chuva");
        assertThat(t.atrasada(AGORA.plusSeconds(7200))).isFalse();
    }
}
