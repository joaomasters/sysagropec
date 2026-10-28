package br.com.sysagropec.identidade.adapter.out.persistence;

import br.com.sysagropec.identidade.application.port.out.IdentidadePorts.OrganizacaoRepositoryPort;
import br.com.sysagropec.identidade.application.port.out.IdentidadePorts.TokenSenhaRepositoryPort;
import br.com.sysagropec.identidade.application.port.out.IdentidadePorts.UsuarioRepositoryPort;
import br.com.sysagropec.identidade.domain.model.TokenSenha;
import br.com.sysagropec.identidade.domain.model.Usuario;
import br.com.sysagropec.shared.domain.ConflitoException;
import br.com.sysagropec.shared.domain.acesso.Perfil;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Component
class IdentidadePersistenceAdapter implements UsuarioRepositoryPort, OrganizacaoRepositoryPort,
        TokenSenhaRepositoryPort {

    private final SpringDataUsuarios usuarios;
    private final SpringDataTokensSenha tokens;
    private final JdbcClient jdbc;

    IdentidadePersistenceAdapter(SpringDataUsuarios usuarios, SpringDataTokensSenha tokens, JdbcClient jdbc) {
        this.usuarios = usuarios;
        this.tokens = tokens;
        this.jdbc = jdbc;
    }

    @Override
    public void salvar(Usuario u) {
        UsuarioJpaEntity e = usuarios.findById(u.id()).orElseGet(UsuarioJpaEntity::new);
        if (e.versao != null && !Objects.equals(e.versao, u.versao())) {
            throw new ConflitoException("O usuário foi alterado por outra operação. Atualize e tente novamente");
        }
        e.id = u.id();
        e.organizacaoId = u.organizacaoId();
        e.nome = u.nome();
        e.email = u.email();
        e.senhaHash = u.senhaHash();
        e.administrador = u.administrador();
        e.ativo = u.ativo();
        e.criadoEm = u.criadoEm();
        e.atualizadoEm = Instant.now();
        e.acessos.clear();
        u.acessos().forEach((propriedadeId, perfil) ->
                e.acessos.add(new UsuarioJpaEntity.AcessoEmbeddable(u.organizacaoId(), propriedadeId, perfil.name())));
        usuarios.saveAndFlush(e);
    }

    @Override
    public Optional<Usuario> buscar(UUID usuarioId) {
        return usuarios.findById(usuarioId).map(IdentidadePersistenceAdapter::paraDominio);
    }

    @Override
    public Optional<Usuario> buscar(UUID organizacaoId, UUID usuarioId) {
        return usuarios.findByOrganizacaoIdAndId(organizacaoId, usuarioId).map(IdentidadePersistenceAdapter::paraDominio);
    }

    @Override
    public Optional<Usuario> buscarPorEmail(String emailNormalizado) {
        return usuarios.findByEmail(emailNormalizado).map(IdentidadePersistenceAdapter::paraDominio);
    }

    @Override
    public List<Usuario> listar(UUID organizacaoId) {
        return usuarios.findByOrganizacaoId(organizacaoId).stream().map(IdentidadePersistenceAdapter::paraDominio)
                .toList();
    }

    @Override
    public boolean existeAlguma() {
        return jdbc.sql("select exists(select 1 from organizacao)").query(Boolean.class).single();
    }

    @Override
    public void criar(UUID id, String nome, Instant agora) {
        jdbc.sql("insert into organizacao (id, nome, criado_em) values (:id, :nome, :agora)")
                .param("id", id).param("nome", nome).param("agora", Timestamp.from(agora))
                .update();
    }

    @Override
    public Optional<String> nome(UUID organizacaoId) {
        return jdbc.sql("select nome from organizacao where id = :id").param("id", organizacaoId)
                .query(String.class).optional();
    }

    @Override
    public void salvar(TokenSenha t) {
        TokenSenhaJpaEntity e = tokens.findById(t.id()).orElseGet(TokenSenhaJpaEntity::new);
        e.id = t.id();
        e.usuarioId = t.usuarioId();
        e.finalidade = t.finalidade().name();
        e.tokenHash = t.tokenHash();
        e.criadoEm = t.criadoEm();
        e.expiraEm = t.expiraEm();
        e.usadoEm = t.usadoEm();
        tokens.saveAndFlush(e);
    }

    @Override
    public Optional<TokenSenha> buscarPorHash(String tokenHash) {
        return tokens.findByTokenHashAndInvalidadoEmIsNull(tokenHash)
                .map(e -> TokenSenha.reconstituir(e.id, e.usuarioId, TokenSenha.Finalidade.valueOf(e.finalidade),
                        e.tokenHash, e.criadoEm, e.expiraEm, e.usadoEm));
    }

    @Override
    public void invalidarPendentes(UUID usuarioId, Instant agora) {
        tokens.invalidarPendentes(usuarioId, agora);
    }

    private static Usuario paraDominio(UsuarioJpaEntity e) {
        Map<UUID, Perfil> acessos = new HashMap<>();
        e.acessos.forEach(a -> acessos.put(a.propriedadeId, Perfil.valueOf(a.perfil)));
        return Usuario.reconstituir(e.id, e.organizacaoId, e.nome, e.email, e.senhaHash, e.administrador, e.ativo,
                e.criadoEm, acessos, e.versao);
    }
}
