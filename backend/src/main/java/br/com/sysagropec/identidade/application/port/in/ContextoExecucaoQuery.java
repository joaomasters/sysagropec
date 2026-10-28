package br.com.sysagropec.identidade.application.port.in;

import br.com.sysagropec.shared.application.seguranca.ContextoExecucao;

import java.util.Optional;
import java.util.UUID;

/**
 * Monta o contexto de execução a cada requisição a partir do estado atual do usuário.
 * Desativação e revogação de acesso passam a valer na requisição seguinte, sem esperar a sessão expirar.
 */
public interface ContextoExecucaoQuery {

    Optional<ContextoExecucao> obter(UUID usuarioId);

    DadosSessao dadosSessao(ContextoExecucao ctx);

    record DadosSessao(String email, String organizacaoNome) {
    }
}
