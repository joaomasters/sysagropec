package br.com.sysagropec.integracao;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import tools.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Isolamento entre organizações, entre propriedades e por perfil, verificado pela API. */
class IsolamentoAcessoIT extends IntegracaoBase {

    @Test
    void organizacaoNaoEnxergaRecursosDeOutra() throws Exception {
        MockHttpSession adminA = login(novaOrganizacaoComAdmin());
        UUID propriedadeA = criarPropriedade(adminA);
        UUID curralA = criarCurral(adminA, propriedadeA, "C1", 10);
        JsonNode recebido = corpo(postar(adminA, "/api/v1/propriedades/" + propriedadeA + "/recebimentos", Map.of(
                "instalacaoId", curralA, "ocorridoEm", instante(Instant.now().minusSeconds(600)),
                "origemDescricao", "F", "animais", List.of(animal("ISO-1")))).andExpect(status().isCreated()));
        String animalA = recebido.get("animais").get(0).get("id").asString();

        MockHttpSession adminB = login(novaOrganizacaoComAdmin());

        obter(adminB, "/api/v1/propriedades").andExpect(jsonPath("$.length()").value(0));
        obter(adminB, "/api/v1/propriedades/" + propriedadeA + "/animais").andExpect(status().isNotFound());
        obter(adminB, "/api/v1/animais/" + animalA).andExpect(status().isNotFound());
        obter(adminB, "/api/v1/animais/" + animalA + "/linha-do-tempo").andExpect(status().isNotFound());
        obter(adminB, "/api/v1/propriedades/" + propriedadeA + "/dashboard").andExpect(status().isNotFound());
        postar(adminB, "/api/v1/animais/" + animalA + "/pesagens",
                Map.of("pesoKg", 300, "ocorridoEm", instante(Instant.now()))).andExpect(status().isNotFound());
        // Identificador de propriedade de outra organização enviado pelo cliente é ignorado.
        postar(adminB, "/api/v1/propriedades/" + propriedadeA + "/instalacoes",
                Map.of("tipo", "CURRAL", "codigo", "X")).andExpect(status().isNotFound());
    }

    @Test
    void perfisLimitamAsOperacoesEAcessoEhPorPropriedade() throws Exception {
        Usuario admin = novaOrganizacaoComAdmin();
        MockHttpSession sessaoAdmin = login(admin);
        UUID propriedade1 = criarPropriedade(sessaoAdmin);
        UUID propriedade2 = criarPropriedade(sessaoAdmin);
        UUID curral = criarCurral(sessaoAdmin, propriedade1, "C1", 10);
        JsonNode recebido = corpo(postar(sessaoAdmin, "/api/v1/propriedades/" + propriedade1 + "/recebimentos",
                Map.of("instalacaoId", curral, "ocorridoEm", instante(Instant.now().minusSeconds(600)),
                        "origemDescricao", "F", "animais", List.of(animal("P-1")))).andExpect(status().isCreated()));
        String animal = recebido.get("animais").get(0).get("id").asString();

        Usuario operador = novoUsuario(admin.organizacaoId(), false);
        colocar(sessaoAdmin, "/api/v1/usuarios/" + operador.id() + "/acessos/" + propriedade1,
                Map.of("perfil", "OPERADOR")).andExpect(status().isOk());
        MockHttpSession sessaoOperador = login(operador);

        postar(sessaoOperador, "/api/v1/animais/" + animal + "/pesagens",
                Map.of("pesoKg", 310, "ocorridoEm", instante(Instant.now()))).andExpect(status().isCreated());
        postar(sessaoOperador, "/api/v1/propriedades/" + propriedade1 + "/recebimentos", Map.of(
                "instalacaoId", curral, "ocorridoEm", instante(Instant.now()), "origemDescricao", "F",
                "animais", List.of(animal("P-2")))).andExpect(status().isForbidden());
        postar(sessaoOperador, "/api/v1/propriedades", Map.of("nome", "Nova")).andExpect(status().isForbidden());
        obter(sessaoOperador, "/api/v1/usuarios").andExpect(status().isForbidden());
        obter(sessaoOperador, "/api/v1/propriedades/" + propriedade2 + "/animais").andExpect(status().isNotFound());
        obter(sessaoOperador, "/api/v1/propriedades").andExpect(jsonPath("$.length()").value(1));

        // Revogação vale na requisição seguinte, sem esperar a sessão expirar.
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .delete("/api/v1/usuarios/" + operador.id() + "/acessos/" + propriedade1)
                        .session(sessaoAdmin)
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isOk());
        obter(sessaoOperador, "/api/v1/propriedades/" + propriedade1 + "/animais").andExpect(status().isNotFound());

        // Usuário desativado perde a sessão.
        postar(sessaoAdmin, "/api/v1/usuarios/" + operador.id() + "/desativacao", null).andExpect(status().isOk());
        obter(sessaoOperador, "/api/v1/sessao").andExpect(status().isUnauthorized());
    }

    @Test
    void semSessaoOuSemCsrfARequisicaoEhRecusada() throws Exception {
        mvc.perform(get("/api/v1/propriedades")).andExpect(status().isUnauthorized());
        MockHttpSession sessao = login(novaOrganizacaoComAdmin());
        mvc.perform(post("/api/v1/propriedades").session(sessao).contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"Sem CSRF\"}")).andExpect(status().isForbidden());
    }

    @Test
    void loginComSenhaErradaNaoRevelaSeOUsuarioExiste() throws Exception {
        Usuario u = novaOrganizacaoComAdmin();
        for (String email : List.of(u.email(), "inexistente@teste.local")) {
            mvc.perform(post("/api/v1/auth/login")
                            .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json.writeValueAsString(Map.of("email", email, "senha", "errada-123456"))))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.detail").value("E-mail ou senha inválidos"));
        }
    }
}
