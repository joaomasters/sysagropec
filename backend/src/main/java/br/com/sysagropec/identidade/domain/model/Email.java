package br.com.sysagropec.identidade.domain.model;

import br.com.sysagropec.shared.domain.RegraNegocioException;

import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

public final class Email {

    private static final Pattern FORMATO = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private Email() {
    }

    public static String normalizar(String valor) {
        return normalizarSeValido(valor).orElseThrow(() -> new RegraNegocioException("E-mail inválido"));
    }

    public static Optional<String> normalizarSeValido(String valor) {
        if (valor == null) {
            return Optional.empty();
        }
        String normalizado = valor.strip().toLowerCase(Locale.ROOT);
        if (normalizado.length() > 254 || !FORMATO.matcher(normalizado).matches()) {
            return Optional.empty();
        }
        return Optional.of(normalizado);
    }
}
