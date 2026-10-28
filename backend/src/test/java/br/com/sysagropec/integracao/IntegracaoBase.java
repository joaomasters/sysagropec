package br.com.sysagropec.integracao;

import br.com.sysagropec.TestcontainersConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Base dos testes de integração: aplicação completa contra PostgreSQL real (Testcontainers),
 * exercitada pela API HTTP com sessão e CSRF, como o frontend faz.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
abstract class IntegracaoBase {

    static final String SENHA = "senha-de-teste-123";

    @Autowired
    MockMvc mvc;
    @Autowired
    JsonMapper json;
    @Autowired
    JdbcClient jdbc;
    @Autowired
    PasswordEncoder encoder;

    record Usuario(UUID id, UUID organizacaoId, String email) {
    }

    /** Cria diretamente no banco uma organização com administrador já ativo. */
    Usuario novaOrganizacaoComAdmin() {
        UUID org = UUID.randomUUID();
        jdbc.sql("insert into organizacao (id, nome, criado_em) values (:id, :nome, now())")
                .param("id", org).param("nome", "Org " + org).update();
        return novoUsuario(org, true);
    }

    Usuario novoUsuario(UUID org, boolean administrador) {
        UUID id = UUID.randomUUID();
        String email = "u" + id.toString().substring(0, 8) + "@teste.local";
        jdbc.sql("""
                        insert into usuario (id, organizacao_id, nome, email, senha_hash, administrador, ativo,
                                             criado_em, atualizado_em)
                        values (:id, :org, :nome, :email, :hash, :admin, true, now(), now())
                        """)
                .param("id", id).param("org", org).param("nome", "Usuário " + email).param("email", email)
                .param("hash", encoder.encode(SENHA)).param("admin", administrador).update();
        return new Usuario(id, org, email);
    }

    MockHttpSession login(Usuario u) throws Exception {
        MockHttpSession sessao = new MockHttpSession();
        mvc.perform(post("/api/v1/auth/login").session(sessao).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("email", u.email(), "senha", SENHA))))
                .andExpect(status().isOk());
        return sessao;
    }

    ResultActions postar(MockHttpSession sessao, String url, Object corpo) throws Exception {
        return mvc.perform(com(post(url), sessao, corpo));
    }

    ResultActions colocar(MockHttpSession sessao, String url, Object corpo) throws Exception {
        return mvc.perform(com(put(url), sessao, corpo));
    }

    ResultActions obter(MockHttpSession sessao, String url) throws Exception {
        return mvc.perform(get(url).session(sessao));
    }

    private MockHttpServletRequestBuilder com(MockHttpServletRequestBuilder b, MockHttpSession sessao, Object corpo) {
        b.session(sessao).with(csrf()).contentType(MediaType.APPLICATION_JSON);
        if (corpo != null) {
            b.content(json.writeValueAsString(corpo));
        }
        return b;
    }

    JsonNode corpo(ResultActions resultado) throws Exception {
        MvcResult r = resultado.andReturn();
        return json.readTree(r.getResponse().getContentAsString());
    }

    UUID id(JsonNode no, String campo) {
        return UUID.fromString(no.get(campo).asString());
    }

    UUID criarPropriedade(MockHttpSession admin) throws Exception {
        return id(corpo(postar(admin, "/api/v1/propriedades",
                Map.of("nome", "Fazenda " + UUID.randomUUID(), "uf", "GO", "fusoHorario", "America/Sao_Paulo"))
                .andExpect(status().isCreated())), "id");
    }

    UUID criarCurral(MockHttpSession sessao, UUID propriedade, String codigo, int capacidade) throws Exception {
        return id(corpo(postar(sessao, "/api/v1/propriedades/" + propriedade + "/instalacoes",
                Map.of("tipo", "CURRAL", "codigo", codigo, "capacidadeCabecas", capacidade))
                .andExpect(status().isCreated())), "id");
    }

    static Map<String, Object> animal(String brinco) {
        return Map.of("brinco", brinco, "sexo", "MACHO", "categoria", "GARROTE", "raca", "Nelore");
    }

    static String instante(Instant i) {
        return i.toString();
    }

    static Timestamp ts(Instant i) {
        return Timestamp.from(i);
    }
}
