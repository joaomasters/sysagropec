package br.com.sysagropec.identidade.application.usecase;

import br.com.sysagropec.identidade.application.port.in.SenhaUseCase;
import br.com.sysagropec.identidade.application.port.out.IdentidadePorts.NotificacaoPort;
import br.com.sysagropec.identidade.application.port.out.IdentidadePorts.SenhaHasherPort;
import br.com.sysagropec.identidade.application.port.out.IdentidadePorts.TokenSenhaRepositoryPort;
import br.com.sysagropec.identidade.application.port.out.IdentidadePorts.UsuarioRepositoryPort;
import br.com.sysagropec.identidade.domain.model.Email;
import br.com.sysagropec.identidade.domain.model.PoliticaSenha;
import br.com.sysagropec.identidade.domain.model.TokenSenha;
import br.com.sysagropec.identidade.domain.model.Usuario;
import br.com.sysagropec.shared.application.port.out.AuditoriaPort;
import br.com.sysagropec.shared.application.port.out.AuditoriaPort.EventoAuditoria;
import br.com.sysagropec.shared.application.port.out.RelogioPort;
import br.com.sysagropec.shared.application.port.out.TransacaoPort;
import br.com.sysagropec.shared.domain.RegraNegocioException;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

/**
 * Redefinição de senha e ativação por convite. O e-mail é enviado fora da transação,
 * depois que o token foi confirmado no banco.
 */
public class SenhaService implements SenhaUseCase {

    private static final System.Logger LOG = System.getLogger(SenhaService.class.getName());

    private final UsuarioRepositoryPort usuarios;
    private final TokenSenhaRepositoryPort tokens;
    private final SenhaHasherPort hasher;
    private final NotificacaoPort notificacao;
    private final AuditoriaPort auditoria;
    private final RelogioPort relogio;
    private final TransacaoPort transacao;
    private final String urlFrontend;

    public SenhaService(UsuarioRepositoryPort usuarios, TokenSenhaRepositoryPort tokens, SenhaHasherPort hasher,
                        NotificacaoPort notificacao, AuditoriaPort auditoria, RelogioPort relogio,
                        TransacaoPort transacao, String urlFrontend) {
        this.usuarios = usuarios;
        this.tokens = tokens;
        this.hasher = hasher;
        this.notificacao = notificacao;
        this.auditoria = auditoria;
        this.relogio = relogio;
        this.transacao = transacao;
        this.urlFrontend = urlFrontend;
    }

    @Override
    public void solicitarRedefinicao(String email) {
        Optional<Usuario> usuario = Email.normalizarSeValido(email)
                .flatMap(e -> transacao.executar(() -> usuarios.buscarPorEmail(e)))
                .filter(Usuario::ativo);
        if (usuario.isEmpty()) {
            return;
        }
        Usuario u = usuario.get();
        String token = emitir(u, TokenSenha.Finalidade.REDEFINICAO, "SENHA_REDEFINICAO_SOLICITADA");
        enviar(u.email(), "SysAgropec - redefinição de senha", """
                Olá, %s.

                Recebemos um pedido para redefinir sua senha no SysAgropec.
                Use o link abaixo em até 30 minutos:

                %s

                Se você não fez este pedido, ignore esta mensagem.
                """.formatted(u.nome(), link(token)));
    }

    /** Emite o convite de um usuário recém-criado. Chamado pelo cadastro de usuários. */
    void enviarConvite(Usuario u) {
        String token = emitir(u, TokenSenha.Finalidade.CONVITE, "CONVITE_EMITIDO");
        enviar(u.email(), "SysAgropec - convite de acesso", """
                Olá, %s.

                Você recebeu acesso ao SysAgropec. Defina sua senha em até 72 horas pelo link:

                %s
                """.formatted(u.nome(), link(token)));
    }

    @Override
    public void definirSenha(String token, String novaSenha) {
        transacao.executar(() -> {
            Instant agora = relogio.agora();
            TokenSenha t = tokens.buscarPorHash(TokensSenha.hash(token == null ? "" : token))
                    .orElseThrow(() -> new RegraNegocioException("Link inválido ou expirado. Solicite um novo"));
            t.usar(agora);
            Usuario u = usuarios.buscar(t.usuarioId())
                    .filter(Usuario::ativo)
                    .orElseThrow(() -> new RegraNegocioException("Link inválido ou expirado. Solicite um novo"));
            PoliticaSenha.validar(novaSenha, u.email());
            u.definirSenhaHash(hasher.gerarHash(novaSenha));
            usuarios.salvar(u);
            tokens.salvar(t);
            tokens.invalidarPendentes(u.id(), agora);
            auditoria.registrar(new EventoAuditoria(u.organizacaoId(), u.id(), "SENHA_DEFINIDA", "USUARIO", u.id(),
                    Map.of("finalidade", t.finalidade().name()), agora));
            return null;
        });
    }

    private String emitir(Usuario u, TokenSenha.Finalidade finalidade, String acao) {
        String token = TokensSenha.gerar();
        transacao.executar(() -> {
            Instant agora = relogio.agora();
            tokens.invalidarPendentes(u.id(), agora);
            tokens.salvar(TokenSenha.emitir(u.id(), finalidade, TokensSenha.hash(token), agora));
            auditoria.registrar(new EventoAuditoria(u.organizacaoId(), u.id(), acao, "USUARIO", u.id(), Map.of(),
                    agora));
            return null;
        });
        return token;
    }

    private void enviar(String destinatario, String assunto, String corpo) {
        try {
            notificacao.enviarEmail(destinatario, assunto, corpo);
        } catch (RuntimeException e) {
            // O token continua válido; o usuário pode solicitar novamente. Não propaga para não revelar o e-mail.
            LOG.log(System.Logger.Level.WARNING, "Falha ao enviar e-mail: " + e.getMessage());
        }
    }

    private String link(String token) {
        return urlFrontend + "/definir-senha?token=" + token;
    }
}
