package br.com.sysagropec.rebanho.application.port.in;

import br.com.sysagropec.shared.application.seguranca.ContextoExecucao;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Operações que alteram o rebanho. Cada chamada é uma unidade transacional. */
public interface RebanhoUseCase {

    RecebimentoRegistrado receber(ContextoExecucao ctx, ReceberAnimaisComando comando);

    LoteFormado formarLote(ContextoExecucao ctx, FormarLoteComando comando);

    UUID movimentar(ContextoExecucao ctx, MovimentarAnimaisComando comando);

    void encerrarLote(ContextoExecucao ctx, UUID loteId, Instant ocorridoEm);

    record ReceberAnimaisComando(UUID propriedadeId, UUID instalacaoId, UUID loteId, Instant ocorridoEm,
                                 String origem, String origemDescricao, String documento, String observacao,
                                 String chaveIdempotencia, List<NovoAnimal> animais) {
    }

    record NovoAnimal(String brinco, String identificadorEletronico, String sexo, String categoria,
                      String finalidade, String raca, LocalDate dataNascimento, boolean nascimentoEstimado) {
    }

    record RecebimentoRegistrado(UUID recebimentoId, UUID movimentacaoId, List<AnimalRecebido> animais) {
    }

    record AnimalRecebido(UUID id, long numero, String brinco) {
    }

    record FormarLoteComando(UUID propriedadeId, String codigo, String fase, String finalidade, String observacao,
                             Instant ocorridoEm, List<UUID> animalIds, UUID instalacaoId, String motivo) {
    }

    record LoteFormado(UUID loteId, UUID movimentacaoId) {
    }

    /** {@code loteDestinoId}, {@code retirarDoLote} e {@code instalacaoDestinoId} podem ser combinados. */
    record MovimentarAnimaisComando(UUID propriedadeId, List<UUID> animalIds, UUID loteDestinoId,
                                    boolean retirarDoLote, UUID instalacaoDestinoId, String motivo,
                                    Instant ocorridoEm) {
    }
}
