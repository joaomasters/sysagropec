package br.com.sysagropec.shared.configuration;

import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.lang.reflect.UndeclaredThrowableException;

/**
 * Decorador que torna cada chamada a uma porta de entrada uma unidade transacional.
 * Mantém {@code @Transactional} fora do domínio e da aplicação: o caso de uso não sabe que é transacional.
 */
public final class Transacional {

    private Transacional() {
    }

    public static <T> T escrita(Class<T> porta, T alvo, PlatformTransactionManager tm) {
        return envolver(porta, alvo, tm, false);
    }

    public static <T> T leitura(Class<T> porta, T alvo, PlatformTransactionManager tm) {
        return envolver(porta, alvo, tm, true);
    }

    private static <T> T envolver(Class<T> porta, T alvo, PlatformTransactionManager tm, boolean somenteLeitura) {
        if (!porta.isInterface()) {
            throw new IllegalArgumentException(porta + " deve ser uma interface");
        }
        TransactionTemplate template = new TransactionTemplate(tm);
        template.setReadOnly(somenteLeitura);
        Object proxy = Proxy.newProxyInstance(porta.getClassLoader(), new Class<?>[]{porta}, (p, metodo, args) -> {
            if (metodo.getDeclaringClass() == Object.class) {
                return metodo.invoke(alvo, args);
            }
            return template.execute(status -> {
                try {
                    return metodo.invoke(alvo, args);
                } catch (InvocationTargetException e) {
                    Throwable causa = e.getCause();
                    if (causa instanceof RuntimeException re) {
                        throw re;
                    }
                    if (causa instanceof Error err) {
                        throw err;
                    }
                    throw new UndeclaredThrowableException(causa);
                } catch (IllegalAccessException e) {
                    throw new IllegalStateException(e);
                }
            });
        });
        return porta.cast(proxy);
    }
}
