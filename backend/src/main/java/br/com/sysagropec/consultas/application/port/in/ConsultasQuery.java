package br.com.sysagropec.consultas.application.port.in;

import br.com.sysagropec.shared.application.seguranca.ContextoExecucao;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Modelos de leitura que agregam dados de vários módulos. Apenas leitura;
 * nenhum indicador é estimado: ausência de dados é representada explicitamente.
 */
public interface ConsultasQuery {

    LinhaDoTempo linhaDoTempo(ContextoExecucao ctx, UUID animalId);

    Dashboard dashboard(ContextoExecucao ctx, UUID propriedadeId);

    record LinhaDoTempo(UUID animalId, List<Evento> eventos) {
    }

    /** {@code ocorridoEm}: quando aconteceu; {@code registradoEm}: quando foi lançado no sistema. */
    record Evento(String tipo, Instant ocorridoEm, Instant registradoEm, String titulo, String descricao,
                  String responsavel, UUID referenciaId) {
    }

    record Dashboard(LocalDate data, String fusoHorario, long animaisAtivos, long animaisSemLote,
                     Map<String, Long> animaisPorCategoria, List<Ocupacao> ocupacao, List<LoteAtivo> lotes,
                     ResumoTarefas tarefas, ResumoPesagens pesagens) {
    }

    /** {@code capacidade} nula: não definida (taxa de ocupação não calculável). */
    record Ocupacao(UUID instalacaoId, String codigo, String tipo, Integer capacidade, long ocupacao) {
    }

    record LoteAtivo(UUID id, String codigo, String fase, long animais) {
    }

    record ResumoTarefas(long abertasAteHoje, long atrasadas, long finalizadasHoje) {
    }

    record ResumoPesagens(long ultimos30Dias, long medidas30Dias, long estimadas30Dias,
                          List<PesagemRecente> recentes) {
    }

    record PesagemRecente(UUID pesagemId, UUID animalId, long animalNumero, String brinco, BigDecimal pesoKg,
                          String natureza, Instant ocorridoEm) {
    }
}
