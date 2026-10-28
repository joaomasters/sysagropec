package br.com.sysagropec.identidade.application.port.out;

import br.com.sysagropec.identidade.domain.model.TokenSenha;
import br.com.sysagropec.identidade.domain.model.Usuario;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Portas de saída do módulo identidade, agrupadas por serem pequenas e usadas em conjunto. */
public final class IdentidadePorts {

    private IdentidadePorts() {
    }

    public interface UsuarioRepositoryPort {
        void salvar(Usuario usuario);

        Optional<Usuario> buscar(UUID usuarioId);

        Optional<Usuario> buscar(UUID organizacaoId, UUID usuarioId);

        Optional<Usuario> buscarPorEmail(String emailNormalizado);

        List<Usuario> listar(UUID organizacaoId);
    }

    public interface OrganizacaoRepositoryPort {
        boolean existeAlguma();

        void criar(UUID id, String nome, Instant agora);

        Optional<String> nome(UUID organizacaoId);
    }

    public interface TokenSenhaRepositoryPort {
        void salvar(TokenSenha token);

        /** Ignora tokens invalidados. */
        Optional<TokenSenha> buscarPorHash(String tokenHash);

        void invalidarPendentes(UUID usuarioId, Instant agora);
    }

    public interface SenhaHasherPort {
        String gerarHash(String senha);

        /** Com hash nulo, executa uma verificação equivalente e retorna false (tempo de resposta uniforme). */
        boolean confere(String senha, String hash);
    }

    public interface NotificacaoPort {
        void enviarEmail(String destinatario, String assunto, String corpo);
    }

    public interface PropriedadesPort {
        Set<UUID> idsAtivos(UUID organizacaoId);
    }
}
