package br.com.sysagropec.rebanho.application.port.in;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Contrato público para outros módulos (pesagem, tarefas). Não verifica permissões:
 * o caso de uso chamador já autorizou o usuário. Buscas sempre restritas à organização.
 */
public interface RebanhoInternoQuery {

    Optional<AnimalInfo> animal(UUID organizacaoId, UUID animalId);

    Optional<LoteInfo> lote(UUID organizacaoId, UUID loteId);

    record AnimalInfo(UUID id, UUID propriedadeId, long numero, String brinco, boolean ativo, Instant entradaEm) {
    }

    record LoteInfo(UUID id, UUID propriedadeId, String codigo, boolean ativo) {
    }
}
