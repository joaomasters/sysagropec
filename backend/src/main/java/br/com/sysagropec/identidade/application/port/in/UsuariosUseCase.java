package br.com.sysagropec.identidade.application.port.in;

import br.com.sysagropec.shared.application.seguranca.ContextoExecucao;

import java.util.List;
import java.util.UUID;

public interface UsuariosUseCase {

    List<UsuarioResumo> listar(ContextoExecucao ctx);

    /** Cria o usuário sem senha e envia um convite para que ele a defina. */
    UsuarioResumo criar(ContextoExecucao ctx, CriarUsuarioComando comando);

    UsuarioResumo concederAcesso(ContextoExecucao ctx, UUID usuarioId, UUID propriedadeId, String perfil);

    UsuarioResumo revogarAcesso(ContextoExecucao ctx, UUID usuarioId, UUID propriedadeId);

    UsuarioResumo desativar(ContextoExecucao ctx, UUID usuarioId);

    record CriarUsuarioComando(String nome, String email, boolean administrador) {
    }

    record UsuarioResumo(UUID id, String nome, String email, boolean administrador, boolean ativo,
                         boolean aguardandoAtivacao, List<AcessoResumo> acessos) {
    }

    record AcessoResumo(UUID propriedadeId, String perfil) {
    }
}
