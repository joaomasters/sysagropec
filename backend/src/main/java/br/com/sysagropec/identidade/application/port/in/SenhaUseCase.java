package br.com.sysagropec.identidade.application.port.in;

public interface SenhaUseCase {

    /** Sempre conclui sem erro, exista ou não o e-mail, para não permitir enumeração de usuários. */
    void solicitarRedefinicao(String email);

    void definirSenha(String token, String novaSenha);
}
