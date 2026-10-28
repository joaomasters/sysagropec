package br.com.sysagropec.shared.application.port.out;

import java.util.Optional;
import java.util.UUID;

/**
 * Garante que uma operação reenviada com a mesma chave não seja aplicada duas vezes.
 * Mesma chave com conteúdo diferente é conflito.
 */
public interface IdempotenciaPort {

    Optional<UUID> resultadoAnterior(UUID organizacaoId, String escopo, String chave, String hashRequisicao);

    void registrar(UUID organizacaoId, String escopo, String chave, String hashRequisicao, UUID resultadoId);
}
