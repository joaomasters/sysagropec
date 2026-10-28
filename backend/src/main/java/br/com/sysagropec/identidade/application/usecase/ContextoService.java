package br.com.sysagropec.identidade.application.usecase;

import br.com.sysagropec.identidade.application.port.in.ContextoExecucaoQuery;
import br.com.sysagropec.identidade.application.port.in.UsuariosInternosQuery;
import br.com.sysagropec.identidade.application.port.out.IdentidadePorts.OrganizacaoRepositoryPort;
import br.com.sysagropec.identidade.application.port.out.IdentidadePorts.PropriedadesPort;
import br.com.sysagropec.identidade.application.port.out.IdentidadePorts.UsuarioRepositoryPort;
import br.com.sysagropec.identidade.domain.model.Usuario;
import br.com.sysagropec.shared.application.seguranca.ContextoExecucao;
import br.com.sysagropec.shared.domain.NaoEncontradoException;
import br.com.sysagropec.shared.domain.acesso.Perfil;
import br.com.sysagropec.shared.domain.acesso.Permissao;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class ContextoService implements ContextoExecucaoQuery, UsuariosInternosQuery {

    private final UsuarioRepositoryPort usuarios;
    private final OrganizacaoRepositoryPort organizacoes;
    private final PropriedadesPort propriedades;

    public ContextoService(UsuarioRepositoryPort usuarios, OrganizacaoRepositoryPort organizacoes,
                           PropriedadesPort propriedades) {
        this.usuarios = usuarios;
        this.organizacoes = organizacoes;
        this.propriedades = propriedades;
    }

    @Override
    public Optional<ContextoExecucao> obter(UUID usuarioId) {
        return usuarios.buscar(usuarioId).filter(Usuario::podeAutenticar).map(this::montar);
    }

    @Override
    public DadosSessao dadosSessao(ContextoExecucao ctx) {
        Usuario u = usuarios.buscar(ctx.organizacaoId(), ctx.usuarioId())
                .orElseThrow(() -> new NaoEncontradoException("Usuário"));
        return new DadosSessao(u.email(), organizacoes.nome(ctx.organizacaoId()).orElse(""));
    }

    @Override
    public boolean possuiPermissao(UUID organizacaoId, UUID usuarioId, UUID propriedadeId, Permissao permissao) {
        return usuarios.buscar(organizacaoId, usuarioId)
                .filter(Usuario::ativo)
                .map(this::montar)
                .map(ctx -> ctx.pode(permissao, propriedadeId))
                .orElse(false);
    }

    @Override
    public Map<UUID, String> nomes(UUID organizacaoId, Collection<UUID> usuarioIds) {
        Map<UUID, String> nomes = new HashMap<>();
        usuarios.listar(organizacaoId).stream()
                .filter(u -> usuarioIds.contains(u.id()))
                .forEach(u -> nomes.put(u.id(), u.nome()));
        return nomes;
    }

    /** Propriedades inativas não concedem acesso, mesmo que o vínculo do usuário ainda exista. */
    private ContextoExecucao montar(Usuario u) {
        Set<UUID> ativas = propriedades.idsAtivos(u.organizacaoId());
        Map<UUID, Set<Permissao>> permissoes = new HashMap<>();
        if (u.administrador()) {
            Set<Permissao> todas = Perfil.todasDePropriedade();
            ativas.forEach(id -> permissoes.put(id, todas));
        } else {
            u.acessos().forEach((propriedadeId, perfil) -> {
                if (ativas.contains(propriedadeId)) {
                    permissoes.put(propriedadeId, perfil.permissoes());
                }
            });
        }
        return new ContextoExecucao(u.id(), u.organizacaoId(), u.nome(), u.administrador(), permissoes);
    }
}
