package br.com.sysagropec.propriedades.adapter.out.persistence;

import br.com.sysagropec.propriedades.application.port.out.PropriedadeRepositoryPort;
import br.com.sysagropec.propriedades.domain.model.Instalacao;
import br.com.sysagropec.propriedades.domain.model.Propriedade;
import br.com.sysagropec.propriedades.domain.model.TipoInstalacao;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
class PropriedadePersistenceAdapter implements PropriedadeRepositoryPort {

    private final SpringDataPropriedades propriedades;
    private final SpringDataInstalacoes instalacoes;

    PropriedadePersistenceAdapter(SpringDataPropriedades propriedades, SpringDataInstalacoes instalacoes) {
        this.propriedades = propriedades;
        this.instalacoes = instalacoes;
    }

    @Override
    public void salvar(Propriedade p) {
        PropriedadeJpaEntity e = propriedades.findById(p.id()).orElseGet(PropriedadeJpaEntity::new);
        e.id = p.id();
        e.organizacaoId = p.organizacaoId();
        e.nome = p.nome();
        e.municipio = p.municipio();
        e.uf = p.uf();
        e.fusoHorario = p.fusoHorario().getId();
        e.ativa = p.ativa();
        e.criadoEm = p.criadoEm();
        e.atualizadoEm = java.time.Instant.now();
        propriedades.save(e);
    }

    @Override
    public Optional<Propriedade> buscar(UUID organizacaoId, UUID id) {
        return propriedades.findByOrganizacaoIdAndId(organizacaoId, id).map(PropriedadePersistenceAdapter::paraDominio);
    }

    @Override
    public List<Propriedade> listar(UUID organizacaoId, Collection<UUID> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        return propriedades.findByOrganizacaoIdAndIdIn(organizacaoId, ids).stream()
                .map(PropriedadePersistenceAdapter::paraDominio).toList();
    }

    @Override
    public List<Propriedade> listarAtivas(UUID organizacaoId) {
        return propriedades.findByOrganizacaoIdAndAtivaTrue(organizacaoId).stream()
                .map(PropriedadePersistenceAdapter::paraDominio).toList();
    }

    @Override
    public boolean existeNome(UUID organizacaoId, String nome) {
        return propriedades.existeNome(organizacaoId, nome);
    }

    @Override
    public void salvar(Instalacao i) {
        InstalacaoJpaEntity e = instalacoes.findById(i.id()).orElseGet(InstalacaoJpaEntity::new);
        e.id = i.id();
        e.organizacaoId = i.organizacaoId();
        e.propriedadeId = i.propriedadeId();
        e.tipo = i.tipo().name();
        e.codigo = i.codigo();
        e.descricao = i.descricao();
        e.capacidadeCabecas = i.capacidadeCabecas();
        e.ativa = i.ativa();
        e.criadoEm = i.criadoEm();
        e.atualizadoEm = java.time.Instant.now();
        instalacoes.save(e);
    }

    @Override
    public Optional<Instalacao> buscarInstalacao(UUID organizacaoId, UUID id) {
        return instalacoes.findByOrganizacaoIdAndId(organizacaoId, id).map(PropriedadePersistenceAdapter::paraDominio);
    }

    @Override
    public Optional<Instalacao> buscarInstalacaoComBloqueio(UUID organizacaoId, UUID id) {
        return instalacoes.buscarComBloqueio(organizacaoId, id).map(PropriedadePersistenceAdapter::paraDominio);
    }

    @Override
    public List<Instalacao> listarInstalacoes(UUID organizacaoId, UUID propriedadeId) {
        return instalacoes.findByOrganizacaoIdAndPropriedadeId(organizacaoId, propriedadeId).stream()
                .map(PropriedadePersistenceAdapter::paraDominio).toList();
    }

    @Override
    public boolean existeCodigoInstalacao(UUID propriedadeId, String codigo) {
        return instalacoes.existeCodigo(propriedadeId, codigo);
    }

    private static Propriedade paraDominio(PropriedadeJpaEntity e) {
        return Propriedade.reconstituir(e.id, e.organizacaoId, e.nome, e.municipio, e.uf, e.fusoHorario, e.ativa,
                e.criadoEm);
    }

    private static Instalacao paraDominio(InstalacaoJpaEntity e) {
        return Instalacao.reconstituir(e.id, e.organizacaoId, e.propriedadeId, TipoInstalacao.valueOf(e.tipo),
                e.codigo, e.descricao, e.capacidadeCabecas, e.ativa, e.criadoEm);
    }
}
