package br.com.sysagropec.identidade.application.port.in;

import br.com.sysagropec.shared.domain.acesso.Permissao;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

/** Contrato público para outros módulos (ex.: validar e exibir o responsável por uma tarefa). */
public interface UsuariosInternosQuery {

    boolean possuiPermissao(UUID organizacaoId, UUID usuarioId, UUID propriedadeId, Permissao permissao);

    Map<UUID, String> nomes(UUID organizacaoId, Collection<UUID> usuarioIds);
}
