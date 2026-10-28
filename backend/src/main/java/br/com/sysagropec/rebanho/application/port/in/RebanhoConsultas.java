package br.com.sysagropec.rebanho.application.port.in;

import br.com.sysagropec.shared.application.Pagina;
import br.com.sysagropec.shared.application.seguranca.ContextoExecucao;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface RebanhoConsultas {

    Pagina<AnimalResumo> listarAnimais(ContextoExecucao ctx, FiltroAnimais filtro);

    AnimalDetalhe detalharAnimal(ContextoExecucao ctx, UUID animalId);

    List<LoteResumo> listarLotes(ContextoExecucao ctx, UUID propriedadeId, boolean incluirEncerrados);

    record FiltroAnimais(UUID propriedadeId, String busca, UUID loteId, UUID instalacaoId, String categoria,
                         String situacao, Boolean semLote, Integer pagina, Integer tamanho) {
    }

    record AnimalResumo(UUID id, long numero, String brinco, String identificadorEletronico, String sexo,
                        String categoria, String finalidade, String raca, String situacao, LocalDate dataNascimento,
                        boolean nascimentoEstimado, UUID loteId, String loteCodigo, UUID instalacaoId,
                        Instant entradaEm) {
    }

    record AnimalDetalhe(UUID id, UUID propriedadeId, long numero, String sexo, String categoria, String finalidade,
                         String raca, String situacao, LocalDate dataNascimento, boolean nascimentoEstimado,
                         String origem, String origemDescricao, UUID recebimentoId, Instant entradaEm,
                         List<IdentificacaoHistorico> identificacoes, List<PeriodoLote> lotes,
                         List<PeriodoLocalizacao> localizacoes) {
    }

    record IdentificacaoHistorico(String tipo, String valor, Instant inicio, Instant fim) {
    }

    record PeriodoLote(UUID loteId, String loteCodigo, Instant inicio, Instant fim) {
    }

    record PeriodoLocalizacao(UUID instalacaoId, Instant inicio, Instant fim) {
    }

    record LoteResumo(UUID id, UUID propriedadeId, String codigo, String fase, String finalidade, String status,
                      Instant inicio, Instant fim, String observacao, long quantidadeAnimais) {
    }
}
