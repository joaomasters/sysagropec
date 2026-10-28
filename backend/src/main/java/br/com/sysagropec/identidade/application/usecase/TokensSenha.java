package br.com.sysagropec.identidade.application.usecase;

import br.com.sysagropec.shared.application.Hashes;

import java.security.SecureRandom;
import java.util.Base64;

/** Gera o valor em claro (enviado ao usuário) e o hash persistido de um token de senha. */
final class TokensSenha {

    private static final SecureRandom RANDOM = new SecureRandom();

    private TokensSenha() {
    }

    static String gerar() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    static String hash(String token) {
        return Hashes.sha256(token);
    }
}
