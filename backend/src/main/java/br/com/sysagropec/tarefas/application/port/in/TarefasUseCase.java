package br.com.sysagropec.tarefas.application.port.in;

import br.com.sysagropec.shared.application.Pagina;
import br.com.sysagropec.shared.application.seguranca.ContextoExecucao;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface TarefasUseCase {

    UUID criar(ContextoExecucao ctx, CriarTarefaComando comando);

    void iniciar(ContextoExecucao ctx, UUID tarefaId);

    void concluir(ContextoExecucao ctx, UUID tarefaId, String observacao, Set<UUID> itensConcluidos);

    void impedir(ContextoExecucao ctx, UUID tarefaId, String justificativa);

    void cancelar(ContextoExecucao ctx, UUID tarefaId, String justificativa);

    record CriarTarefaComando(UUID propriedadeId, String tipo, String titulo, String descricao, String alvoTipo,
                              UUID alvoId, UUID responsavelId, Instant prazo, String prioridade,
                              List<ItemChecklistNovo> checklist) {
    }

    record ItemChecklistNovo(String descricao, boolean obrigatorio) {
    }

    interface Consultas {
        Pagina<TarefaResumo> listar(ContextoExecucao ctx, FiltroTarefas filtro);

        RotinasDoDia rotinasDeHoje(ContextoExecucao ctx, UUID propriedadeId);

        TarefaDetalhe detalhar(ContextoExecucao ctx, UUID tarefaId);
    }

    /** {@code status} vazio significa todos. {@code apenasAtrasadas} considera o horário atual. */
    record FiltroTarefas(UUID propriedadeId, Set<String> status, UUID responsavelId, String alvoTipo, UUID alvoId,
                         boolean apenasAtrasadas, Integer pagina, Integer tamanho) {
    }

    record RotinasDoDia(LocalDate data, String fusoHorario, List<TarefaResumo> abertas,
                        List<TarefaResumo> finalizadasHoje) {
    }

    record TarefaResumo(UUID id, UUID propriedadeId, String tipo, String titulo, String alvoTipo, UUID alvoId,
                        String alvoDescricao, UUID responsavelId, String responsavelNome, Instant prazo,
                        String prioridade, String status, boolean atrasada, int checklistTotal,
                        int checklistConcluidos, Instant finalizadaEm) {
    }

    record TarefaDetalhe(TarefaResumo resumo, String descricao, UUID criadaPor, Instant criadaEm, Instant iniciadaEm,
                         UUID finalizadaPor, String finalizadaPorNome, String observacaoConclusao,
                         String justificativa, List<ItemChecklistDetalhe> checklist,
                         List<ResultadoDetalhe> resultados) {
    }

    record ItemChecklistDetalhe(UUID id, int ordem, String descricao, boolean obrigatorio, boolean concluido,
                                Instant concluidoEm) {
    }

    record ResultadoDetalhe(String tipo, UUID id, Instant vinculadoEm) {
    }
}
