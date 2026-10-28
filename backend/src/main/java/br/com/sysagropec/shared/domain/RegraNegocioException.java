package br.com.sysagropec.shared.domain;

/** Violação de regra de negócio: a requisição é bem formada, mas a operação não é permitida no estado atual. */
public class RegraNegocioException extends RuntimeException {
    public RegraNegocioException(String mensagem) {
        super(mensagem);
    }
}
