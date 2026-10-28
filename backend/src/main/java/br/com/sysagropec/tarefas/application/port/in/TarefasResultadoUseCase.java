package br.com.sysagropec.tarefas.application.port.in;

import br.com.sysagropec.shared.application.seguranca.ContextoExecucao;

import java.util.UUID;

/**
 * Contrato público para módulos técnicos (ex.: pesagem) vincularem o registro produzido à tarefa,
 * na mesma transação do registro. Verifica a permissão de execução da tarefa.
 */
public interface TarefasResultadoUseCase {

    /** @return true se a tarefa foi concluída por este registro */
    boolean registrarResultado(ContextoExecucao ctx, UUID tarefaId, Resultado resultado);

    record Resultado(String tipo, UUID id, String alvoTipo, UUID alvoId) {
    }
}
