package br.com.sysagropec.shared.domain;

/** Conflito com o estado persistido: duplicidade, versão desatualizada ou lançamento retroativo. */
public class ConflitoException extends RuntimeException {
    public ConflitoException(String mensagem) {
        super(mensagem);
    }
}
