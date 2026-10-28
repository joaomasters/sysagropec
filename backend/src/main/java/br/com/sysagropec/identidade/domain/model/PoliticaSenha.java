package br.com.sysagropec.identidade.domain.model;

import br.com.sysagropec.shared.domain.Verificar;

public final class PoliticaSenha {

    public static final int TAMANHO_MINIMO = 10;
    public static final int TAMANHO_MAXIMO = 128;

    private PoliticaSenha() {
    }

    public static void validar(String senha, String email) {
        Verificar.obrigatorio(senha, "Senha");
        Verificar.regra(senha.length() >= TAMANHO_MINIMO,
                "A senha deve ter pelo menos " + TAMANHO_MINIMO + " caracteres");
        Verificar.regra(senha.length() <= TAMANHO_MAXIMO,
                "A senha deve ter no máximo " + TAMANHO_MAXIMO + " caracteres");
        Verificar.regra(!senha.isBlank(), "A senha não pode ser composta apenas de espaços");
        Verificar.regra(email == null || !senha.equalsIgnoreCase(email), "A senha não pode ser igual ao e-mail");
    }
}
