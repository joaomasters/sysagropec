package br.com.sysagropec.identidade.application.port.in;

import java.util.UUID;

public interface AutenticacaoUseCase {

    /** @throws CredenciaisInvalidasException para usuário inexistente, inativo, sem senha ou senha incorreta */
    UsuarioAutenticado autenticar(String email, String senha);

    record UsuarioAutenticado(UUID usuarioId, UUID organizacaoId) {
    }

    /** Mensagem única para qualquer falha, sem revelar se o e-mail existe. */
    class CredenciaisInvalidasException extends RuntimeException {
        public CredenciaisInvalidasException() {
            super("E-mail ou senha inválidos");
        }
    }
}
