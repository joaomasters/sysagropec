package br.com.sysagropec.integracao;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import tools.jackson.databind.JsonNode;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Critério de aceite da primeira entrega: usuário autorizado cria propriedade e curral, recebe animais,
 * forma lote, registra pesagem, cria e conclui tarefa e consulta o histórico — tudo persistido no PostgreSQL.
 */
class FluxoPrimeiraEntregaIT extends IntegracaoBase {

    @Test
    void fluxoCompletoDoConfinamento() throws Exception {
        MockHttpSession admin = login(novaOrganizacaoComAdmin());
        UUID propriedade = criarPropriedade(admin);
        // O administrador passa a ter acesso à nova propriedade na requisição seguinte.
        UUID curral1 = criarCurral(admin, propriedade, "C1", 10);
        UUID curral2 = criarCurral(admin, propriedade, "C2", 10);

        Instant entrada = Instant.now().minus(2, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS);
        Map<String, Object> recebimento = Map.of("instalacaoId", curral1, "ocorridoEm", instante(entrada),
                "origemDescricao", "Fornecedor A", "documento", "GTA-123",
                "animais", List.of(animal("BR-1"), animal("BR-2"), animal("BR-3")));
        JsonNode recebido = corpo(mvc.perform(post("/api/v1/propriedades/" + propriedade + "/recebimentos")
                        .session(admin).with(csrf()).header("Idempotency-Key", "receb-1")
                        .contentType("application/json").content(json.writeValueAsString(recebimento)))
                .andExpect(status().isCreated()));
        List<UUID> animais = new ArrayList<>();
        recebido.get("animais").forEach(a -> animais.add(UUID.fromString(a.get("id").asString())));
        assertThat(animais).hasSize(3);

        // Reenvio com a mesma chave não duplica.
        JsonNode reenvio = corpo(mvc.perform(post("/api/v1/propriedades/" + propriedade + "/recebimentos")
                        .session(admin).with(csrf()).header("Idempotency-Key", "receb-1")
                        .contentType("application/json").content(json.writeValueAsString(recebimento)))
                .andExpect(status().isCreated()));
        assertThat(reenvio.get("recebimentoId").asString()).isEqualTo(recebido.get("recebimentoId").asString());

        UUID lote = id(corpo(postar(admin, "/api/v1/propriedades/" + propriedade + "/lotes", Map.of(
                "codigo", "L-01", "fase", "ADAPTACAO", "ocorridoEm", instante(entrada.plusSeconds(3600)),
                "animalIds", animais, "instalacaoId", curral2))
                .andExpect(status().isCreated())), "loteId");

        UUID tarefa = id(corpo(postar(admin, "/api/v1/propriedades/" + propriedade + "/tarefas", Map.of(
                "tipo", "PESAGEM", "titulo", "Pesar BR-1", "alvoTipo", "ANIMAL", "alvoId", animais.getFirst(),
                "prazo", instante(Instant.now().plusSeconds(3600))))
                .andExpect(status().isCreated())), "id");

        // Concluir manualmente sem pesagem não é permitido.
        postar(admin, "/api/v1/tarefas/" + tarefa + "/conclusao", Map.of())
                .andExpect(status().isUnprocessableContent());

        postar(admin, "/api/v1/animais/" + animais.getFirst() + "/pesagens", Map.of(
                "pesoKg", 352.5, "finalidade", "ENTRADA", "ocorridoEm", instante(entrada.plusSeconds(1800)),
                "tarefaId", tarefa))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tarefaConcluida").value(true));

        obter(admin, "/api/v1/tarefas/" + tarefa)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resumo.status").value("CONCLUIDA"))
                .andExpect(jsonPath("$.resultados.length()").value(1));

        JsonNode historico = corpo(obter(admin, "/api/v1/animais/" + animais.getFirst() + "/linha-do-tempo")
                .andExpect(status().isOk()));
        List<String> tipos = new ArrayList<>();
        historico.get("eventos").forEach(e -> tipos.add(e.get("tipo").asString()));
        assertThat(tipos).contains("RECEBIMENTO", "FORMACAO_LOTE", "PESAGEM", "TAREFA_CONCLUIDA");

        obter(admin, "/api/v1/animais/" + animais.getFirst())
                .andExpect(jsonPath("$.lotes[0].loteCodigo").value("L-01"))
                .andExpect(jsonPath("$.localizacoes.length()").value(2));

        obter(admin, "/api/v1/propriedades/" + propriedade + "/dashboard")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.animaisAtivos").value(3))
                .andExpect(jsonPath("$.animaisSemLote").value(0))
                .andExpect(jsonPath("$.pesagens.medidas30Dias").value(1));

        // Estado persistido coerente: vínculos abertos únicos e movimentações registradas.
        assertThat(jdbc.sql("select count(*) from animal_localizacao where instalacao_id = :c and fim is null")
                .param("c", curral2).query(Long.class).single()).isEqualTo(3);
        assertThat(jdbc.sql("select count(*) from animal_lote_vinculo where lote_id = :l and fim is null")
                .param("l", lote).query(Long.class).single()).isEqualTo(3);
        assertThat(jdbc.sql("select count(*) from movimentacao where propriedade_id = :p")
                .param("p", propriedade).query(Long.class).single()).isEqualTo(2);
    }

    @Test
    void movimentacaoRetroativaEBrincoDuplicadoSaoConflitos() throws Exception {
        MockHttpSession admin = login(novaOrganizacaoComAdmin());
        UUID propriedade = criarPropriedade(admin);
        UUID curral = criarCurral(admin, propriedade, "C1", 5);
        Instant entrada = Instant.now().minus(1, ChronoUnit.DAYS);

        JsonNode recebido = corpo(postar(admin, "/api/v1/propriedades/" + propriedade + "/recebimentos", Map.of(
                "instalacaoId", curral, "ocorridoEm", instante(entrada), "origemDescricao", "F",
                "animais", List.of(animal("X-1")))).andExpect(status().isCreated()));
        UUID animal = UUID.fromString(recebido.get("animais").get(0).get("id").asString());

        postar(admin, "/api/v1/propriedades/" + propriedade + "/recebimentos", Map.of(
                "instalacaoId", curral, "ocorridoEm", instante(entrada), "origemDescricao", "F",
                "animais", List.of(animal("x-1")))).andExpect(status().isConflict());

        UUID curral2 = criarCurral(admin, propriedade, "C2", 5);
        postar(admin, "/api/v1/propriedades/" + propriedade + "/movimentacoes", Map.of(
                "animalIds", List.of(animal), "instalacaoDestinoId", curral2, "motivo", "Teste",
                "ocorridoEm", instante(entrada.minusSeconds(60)))).andExpect(status().isConflict());

        postar(admin, "/api/v1/propriedades/" + propriedade + "/movimentacoes", Map.of(
                "animalIds", List.of(animal), "instalacaoDestinoId", curral2, "motivo", "Teste",
                "ocorridoEm", instante(Instant.now().plusSeconds(3600)))).andExpect(status().isUnprocessableContent());
    }
}
