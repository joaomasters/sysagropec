package br.com.sysagropec.pesagem.application.port.in;

import br.com.sysagropec.shared.application.seguranca.ContextoExecucao;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface PesagemUseCase {

    /**
     * Registra a pesagem. Com {@code tarefaId}, vincula a pesagem à tarefa na mesma transação;
     * a tarefa de pesagem do próprio animal é concluída automaticamente.
     */
    PesagemRegistrada registrar(ContextoExecucao ctx, RegistrarPesagemComando comando);

    void cancelar(ContextoExecucao ctx, UUID pesagemId, String motivo);

    record RegistrarPesagemComando(UUID animalId, BigDecimal pesoKg, String natureza, String metodo,
                                   String finalidade, String equipamento, String observacao, Instant ocorridoEm,
                                   UUID tarefaId, String chaveIdempotencia) {
    }

    record PesagemRegistrada(UUID pesagemId, boolean tarefaConcluida) {
    }

    interface Consultas {
        List<PesagemResumo> listarPorAnimal(ContextoExecucao ctx, UUID animalId);
    }

    record PesagemResumo(UUID id, UUID animalId, BigDecimal pesoKg, String natureza, String metodo,
                         String finalidade, String equipamento, String observacao, Instant ocorridoEm,
                         Instant registradoEm, UUID responsavelId, UUID tarefaId, boolean cancelada,
                         Instant canceladaEm, String motivoCancelamento) {
    }
}
