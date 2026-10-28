package br.com.sysagropec.shared.application.port.out;

import java.util.function.Supplier;

/**
 * Fronteira transacional explícita para casos de uso que precisam confirmar a gravação
 * antes de chamar um serviço externo lento (ex.: envio de e-mail).
 */
public interface TransacaoPort {
    <T> T executar(Supplier<T> operacao);
}
