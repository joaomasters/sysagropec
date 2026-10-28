package br.com.sysagropec.propriedades.application.port.in;

import java.time.ZoneId;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Contrato público para outros módulos. Não verifica permissões: quem chama é um caso de uso
 * que já autorizou o usuário. Todas as buscas são restritas à organização informada.
 */
public interface PropriedadesInternasQuery {

    Set<UUID> idsAtivos(UUID organizacaoId);

    Optional<ZoneId> fusoHorario(UUID organizacaoId, UUID propriedadeId);

    Optional<InstalacaoInfo> instalacao(UUID organizacaoId, UUID instalacaoId);

    /** Bloqueia a instalação até o fim da transação, serializando alocações concorrentes (controle de lotação). */
    Optional<InstalacaoInfo> instalacaoComBloqueio(UUID organizacaoId, UUID instalacaoId);

    record InstalacaoInfo(UUID id, UUID propriedadeId, String codigo, Integer capacidadeCabecas, boolean ativa) {
    }
}
