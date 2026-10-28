package br.com.sysagropec.identidade.adapter.in.web;

import java.io.Serial;
import java.io.Serializable;
import java.security.Principal;
import java.util.UUID;

/**
 * O que fica guardado na sessão: apenas identificadores. Permissões são recalculadas a cada requisição.
 * O nome do principal é o id do usuário, usado pelo Spring Session para localizar (e revogar) suas sessões.
 */
public record SessaoUsuario(UUID usuarioId, UUID organizacaoId) implements Principal, Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Override
    public String getName() {
        return usuarioId.toString();
    }
}
