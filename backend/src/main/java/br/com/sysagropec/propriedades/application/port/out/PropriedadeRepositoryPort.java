package br.com.sysagropec.propriedades.application.port.out;

import br.com.sysagropec.propriedades.domain.model.Instalacao;
import br.com.sysagropec.propriedades.domain.model.Propriedade;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PropriedadeRepositoryPort {

    void salvar(Propriedade propriedade);

    Optional<Propriedade> buscar(UUID organizacaoId, UUID id);

    List<Propriedade> listar(UUID organizacaoId, Collection<UUID> ids);

    List<Propriedade> listarAtivas(UUID organizacaoId);

    boolean existeNome(UUID organizacaoId, String nome);

    void salvar(Instalacao instalacao);

    Optional<Instalacao> buscarInstalacao(UUID organizacaoId, UUID id);

    Optional<Instalacao> buscarInstalacaoComBloqueio(UUID organizacaoId, UUID id);

    List<Instalacao> listarInstalacoes(UUID organizacaoId, UUID propriedadeId);

    boolean existeCodigoInstalacao(UUID propriedadeId, String codigo);
}
