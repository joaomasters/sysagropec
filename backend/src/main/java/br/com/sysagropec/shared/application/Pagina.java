package br.com.sysagropec.shared.application;

import java.util.List;
import java.util.function.Function;

public record Pagina<T>(List<T> itens, int pagina, int tamanho, long total) {

    public static final int TAMANHO_MAXIMO = 200;

    public Pagina {
        itens = List.copyOf(itens);
    }

    public <R> Pagina<R> map(Function<T, R> conversor) {
        return new Pagina<>(itens.stream().map(conversor).toList(), pagina, tamanho, total);
    }

    public static int tamanhoValido(Integer tamanho) {
        if (tamanho == null || tamanho < 1) {
            return 25;
        }
        return Math.min(tamanho, TAMANHO_MAXIMO);
    }

    public static int paginaValida(Integer pagina) {
        return pagina == null || pagina < 0 ? 0 : pagina;
    }
}
