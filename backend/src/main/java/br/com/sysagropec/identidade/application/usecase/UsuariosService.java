package br.com.sysagropec.identidade.application.usecase;

import br.com.sysagropec.identidade.application.port.in.UsuariosUseCase;
import br.com.sysagropec.identidade.application.port.out.IdentidadePorts.PropriedadesPort;
import br.com.sysagropec.identidade.application.port.out.IdentidadePorts.TokenSenhaRepositoryPort;
import br.com.sysagropec.identidade.application.port.out.IdentidadePorts.UsuarioRepositoryPort;
import br.com.sysagropec.identidade.domain.model.Usuario;
import br.com.sysagropec.shared.application.port.out.AuditoriaPort;
import br.com.sysagropec.shared.application.port.out.AuditoriaPort.EventoAuditoria;
import br.com.sysagropec.shared.application.port.out.RelogioPort;
import br.com.sysagropec.shared.application.port.out.TransacaoPort;
import br.com.sysagropec.shared.application.seguranca.ContextoExecucao;
import br.com.sysagropec.shared.domain.ConflitoException;
import br.com.sysagropec.shared.domain.NaoEncontradoException;
import br.com.sysagropec.shared.domain.RegraNegocioException;
import br.com.sysagropec.shared.domain.acesso.Perfil;
import br.com.sysagropec.shared.domain.acesso.Permissao;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

public class UsuariosService implements UsuariosUseCase {

    private final UsuarioRepositoryPort usuarios;
    private final TokenSenhaRepositoryPort tokens;
    private final PropriedadesPort propriedades;
    private final SenhaService senhas;
    private final AuditoriaPort auditoria;
    private final RelogioPort relogio;
    private final TransacaoPort transacao;

    public UsuariosService(UsuarioRepositoryPort usuarios, TokenSenhaRepositoryPort tokens,
                           PropriedadesPort propriedades, SenhaService senhas, AuditoriaPort auditoria,
                           RelogioPort relogio, TransacaoPort transacao) {
        this.usuarios = usuarios;
        this.tokens = tokens;
        this.propriedades = propriedades;
        this.senhas = senhas;
        this.auditoria = auditoria;
        this.relogio = relogio;
        this.transacao = transacao;
    }

    @Override
    public List<UsuarioResumo> listar(ContextoExecucao ctx) {
        ctx.exigirNaOrganizacao(Permissao.USUARIO_GERIR);
        return transacao.executar(() -> usuarios.listar(ctx.organizacaoId())).stream()
                .sorted(Comparator.comparing(Usuario::nome, String.CASE_INSENSITIVE_ORDER))
                .map(UsuariosService::resumo)
                .toList();
    }

    @Override
    public UsuarioResumo criar(ContextoExecucao ctx, CriarUsuarioComando comando) {
        ctx.exigirNaOrganizacao(Permissao.USUARIO_GERIR);
        Usuario criado = transacao.executar(() -> {
            Instant agora = relogio.agora();
            Usuario u = Usuario.novo(UUID.randomUUID(), ctx.organizacaoId(), comando.nome(), comando.email(),
                    comando.administrador(), agora);
            if (usuarios.buscarPorEmail(u.email()).isPresent()) {
                throw new ConflitoException("Já existe um usuário com este e-mail");
            }
            usuarios.salvar(u);
            auditar(ctx, "USUARIO_CRIADO", u, Map.of("administrador", u.administrador()), agora);
            return u;
        });
        senhas.enviarConvite(criado);
        return resumo(criado);
    }

    @Override
    public UsuarioResumo concederAcesso(ContextoExecucao ctx, UUID usuarioId, UUID propriedadeId, String perfil) {
        Perfil p = perfil(perfil);
        return alterar(ctx, usuarioId, "ACESSO_CONCEDIDO", Map.of("propriedadeId", propriedadeId, "perfil", p.name()),
                u -> {
                    if (!propriedades.idsAtivos(ctx.organizacaoId()).contains(propriedadeId)) {
                        throw new NaoEncontradoException("Propriedade");
                    }
                    u.concederAcesso(propriedadeId, p);
                });
    }

    @Override
    public UsuarioResumo revogarAcesso(ContextoExecucao ctx, UUID usuarioId, UUID propriedadeId) {
        return alterar(ctx, usuarioId, "ACESSO_REVOGADO", Map.of("propriedadeId", propriedadeId),
                u -> u.revogarAcesso(propriedadeId));
    }

    @Override
    public UsuarioResumo desativar(ContextoExecucao ctx, UUID usuarioId) {
        return alterar(ctx, usuarioId, "USUARIO_DESATIVADO", Map.of(), u -> {
            u.desativar(ctx.usuarioId());
            tokens.invalidarPendentes(u.id(), relogio.agora());
        });
    }

    private UsuarioResumo alterar(ContextoExecucao ctx, UUID usuarioId, String acao, Map<String, Object> detalhes,
                                  Consumer<Usuario> alteracao) {
        ctx.exigirNaOrganizacao(Permissao.USUARIO_GERIR);
        return transacao.executar(() -> {
            Usuario u = usuarios.buscar(ctx.organizacaoId(), usuarioId)
                    .orElseThrow(() -> new NaoEncontradoException("Usuário"));
            alteracao.accept(u);
            usuarios.salvar(u);
            auditar(ctx, acao, u, detalhes, relogio.agora());
            return resumo(u);
        });
    }

    private void auditar(ContextoExecucao ctx, String acao, Usuario alvo, Map<String, Object> detalhes, Instant agora) {
        auditoria.registrar(new EventoAuditoria(ctx.organizacaoId(), ctx.usuarioId(), acao, "USUARIO", alvo.id(),
                detalhes, agora));
    }

    private static Perfil perfil(String valor) {
        try {
            return Perfil.valueOf(valor);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new RegraNegocioException("Perfil inválido");
        }
    }

    static UsuarioResumo resumo(Usuario u) {
        List<AcessoResumo> acessos = u.acessos().entrySet().stream()
                .map(e -> new AcessoResumo(e.getKey(), e.getValue().name()))
                .toList();
        return new UsuarioResumo(u.id(), u.nome(), u.email(), u.administrador(), u.ativo(), u.aguardandoAtivacao(),
                acessos);
    }
}
