package br.com.sysagropec.shared.domain;

/** Recurso inexistente ou fora do escopo autorizado (não revela qual dos dois). */
public class NaoEncontradoException extends RuntimeException {
    public NaoEncontradoException(String recurso) {
        super(recurso + " não encontrado(a)");
    }
}
