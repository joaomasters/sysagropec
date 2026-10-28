package br.com.sysagropec.identidade.application.usecase;

import br.com.sysagropec.identidade.application.port.in.AutenticacaoUseCase;
import br.com.sysagropec.identidade.application.port.out.IdentidadePorts.SenhaHasherPort;
import br.com.sysagropec.identidade.application.port.out.IdentidadePorts.UsuarioRepositoryPort;
import br.com.sysagropec.identidade.domain.model.Email;
import br.com.sysagropec.identidade.domain.model.Usuario;
import br.com.sysagropec.shared.application.port.out.AuditoriaPort;
import br.com.sysagropec.shared.application.port.out.AuditoriaPort.EventoAuditoria;
import br.com.sysagropec.shared.application.port.out.RelogioPort;
import br.com.sysagropec.shared.application.port.out.TransacaoPort;

import java.util.Map;
import java.util.Optional;

/**
 * Usa transações explícitas: a falha de login precisa ser auditada mesmo quando a operação
 * termina em exceção, o que um decorador com rollback descartaria.
 */
public class AutenticacaoService implements AutenticacaoUseCase {

    private final UsuarioRepositoryPort usuarios;
    private final SenhaHasherPort hasher;
    private final AuditoriaPort auditoria;
    private final RelogioPort relogio;
    private final TransacaoPort transacao;

    public AutenticacaoService(UsuarioRepositoryPort usuarios, SenhaHasherPort hasher, AuditoriaPort auditoria,
                               RelogioPort relogio, TransacaoPort transacao) {
        this.usuarios = usuarios;
        this.hasher = hasher;
        this.auditoria = auditoria;
        this.relogio = relogio;
        this.transacao = transacao;
    }

    @Override
    public UsuarioAutenticado autenticar(String email, String senha) {
        Optional<Usuario> usuario = Email.normalizarSeValido(email)
                .flatMap(e -> transacao.executar(() -> usuarios.buscarPorEmail(e)));
        String hash = usuario.filter(Usuario::podeAutenticar).map(Usuario::senhaHash).orElse(null);
        boolean confere = hasher.confere(senha == null ? "" : senha, hash);

        if (!confere || hash == null) {
            usuario.ifPresent(u -> registrar(u, "LOGIN_FALHOU", Map.of("motivo",
                    u.podeAutenticar() ? "senha_incorreta" : "usuario_inativo_ou_sem_senha")));
            throw new CredenciaisInvalidasException();
        }
        Usuario u = usuario.get();
        registrar(u, "LOGIN", Map.of());
        return new UsuarioAutenticado(u.id(), u.organizacaoId());
    }

    private void registrar(Usuario u, String acao, Map<String, Object> detalhes) {
        transacao.executar(() -> {
            auditoria.registrar(new EventoAuditoria(u.organizacaoId(), u.id(), acao, "USUARIO", u.id(), detalhes,
                    relogio.agora()));
            return null;
        });
    }
}
