package br.com.sysagropec.shared.domain;

import java.util.Collection;

/** Verificações de invariantes reutilizadas pelos modelos de domínio. */
public final class Verificar {

    private Verificar() {
    }

    public static <T> T obrigatorio(T valor, String campo) {
        if (valor == null) {
            throw new RegraNegocioException(campo + " é obrigatório(a)");
        }
        return valor;
    }

    public static String texto(String valor, String campo, int tamanhoMaximo) {
        String normalizado = textoOpcional(valor, campo, tamanhoMaximo);
        if (normalizado == null) {
            throw new RegraNegocioException(campo + " é obrigatório(a)");
        }
        return normalizado;
    }

    public static String textoOpcional(String valor, String campo, int tamanhoMaximo) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        String normalizado = valor.strip();
        if (normalizado.length() > tamanhoMaximo) {
            throw new RegraNegocioException(campo + " deve ter no máximo " + tamanhoMaximo + " caracteres");
        }
        return normalizado;
    }

    public static <C extends Collection<?>> C naoVazia(C colecao, String campo) {
        if (colecao == null || colecao.isEmpty()) {
            throw new RegraNegocioException(campo + " deve ter ao menos um item");
        }
        return colecao;
    }

    public static void regra(boolean condicao, String mensagem) {
        if (!condicao) {
            throw new RegraNegocioException(mensagem);
        }
    }
}
