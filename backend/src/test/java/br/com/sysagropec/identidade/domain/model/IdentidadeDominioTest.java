package br.com.sysagropec.identidade.domain.model;

import br.com.sysagropec.shared.application.seguranca.AcessoNegadoException;
import br.com.sysagropec.shared.application.seguranca.ContextoExecucao;
import br.com.sysagropec.shared.domain.NaoEncontradoException;
import br.com.sysagropec.shared.domain.RegraNegocioException;
import br.com.sysagropec.shared.domain.acesso.Perfil;
import br.com.sysagropec.shared.domain.acesso.Permissao;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IdentidadeDominioTest {

    static final Instant AGORA = Instant.parse("2026-10-20T12:00:00Z");

    @Test
    void tokenDeSenhaEhDeUsoUnicoEExpira() {
        TokenSenha t = TokenSenha.emitir(UUID.randomUUID(), TokenSenha.Finalidade.REDEFINICAO, "hash", AGORA);

        assertThat(t.expiraEm()).isEqualTo(AGORA.plus(Duration.ofMinutes(30)));
        t.usar(AGORA.plusSeconds(60));
        assertThatThrownBy(() -> t.usar(AGORA.plusSeconds(61))).isInstanceOf(RegraNegocioException.class);

        TokenSenha expirado = TokenSenha.emitir(UUID.randomUUID(), TokenSenha.Finalidade.REDEFINICAO, "h2", AGORA);
        assertThatThrownBy(() -> expirado.usar(AGORA.plus(Duration.ofMinutes(30))))
                .isInstanceOf(RegraNegocioException.class);
    }

    @Test
    void politicaDeSenha() {
        assertThatThrownBy(() -> PoliticaSenha.validar("curta", "a@b.c")).isInstanceOf(RegraNegocioException.class);
        assertThatThrownBy(() -> PoliticaSenha.validar("joao@fazenda.com", "joao@fazenda.com"))
                .isInstanceOf(RegraNegocioException.class);
        assertThatCode(() -> PoliticaSenha.validar("uma senha longa", "a@b.c")).doesNotThrowAnyException();
    }

    @Test
    void usuarioNaoPodeDesativarASiMesmoEAdminNaoRecebePerfil() {
        UUID id = UUID.randomUUID();
        Usuario admin = Usuario.novo(id, UUID.randomUUID(), "Admin", " Admin@Fazenda.COM ", true, AGORA);

        assertThat(admin.email()).isEqualTo("admin@fazenda.com");
        assertThat(admin.podeAutenticar()).isFalse();
        assertThatThrownBy(() -> admin.desativar(id)).isInstanceOf(RegraNegocioException.class);
        assertThatThrownBy(() -> admin.concederAcesso(UUID.randomUUID(), Perfil.GESTOR))
                .isInstanceOf(RegraNegocioException.class);
    }

    @Test
    void contextoNegaSemRevelarPropriedadesDeTerceiros() {
        UUID propriedade = UUID.randomUUID();
        ContextoExecucao operador = new ContextoExecucao(UUID.randomUUID(), UUID.randomUUID(), "Op", false,
                Map.of(propriedade, Perfil.OPERADOR.permissoes()));

        assertThatCode(() -> operador.exigir(Permissao.PESAGEM_REGISTRAR, propriedade)).doesNotThrowAnyException();
        assertThatThrownBy(() -> operador.exigir(Permissao.ANIMAL_RECEBER, propriedade))
                .isInstanceOf(AcessoNegadoException.class);
        assertThatThrownBy(() -> operador.exigir(Permissao.CONSULTAR, UUID.randomUUID()))
                .isInstanceOf(NaoEncontradoException.class);
        assertThatThrownBy(() -> operador.exigirNaOrganizacao(Permissao.PROPRIEDADE_GERIR))
                .isInstanceOf(AcessoNegadoException.class);
    }
}
