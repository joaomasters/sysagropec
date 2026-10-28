package br.com.sysagropec.rebanho.domain.model;

import br.com.sysagropec.rebanho.domain.model.Enums.TipoMovimentacao;
import br.com.sysagropec.shared.domain.Momento;
import br.com.sysagropec.shared.domain.Verificar;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Registro imutável de uma movimentação, com origem e destino de cada animal, motivo e responsável. */
public record Movimentacao(UUID id, UUID organizacaoId, UUID propriedadeId, TipoMovimentacao tipo, String motivo,
                           Instant ocorridoEm, Instant registradoEm, UUID responsavelId,
                           List<AlteracaoAlocacao> itens) {

    public static Movimentacao registrar(UUID id, UUID organizacaoId, UUID propriedadeId, TipoMovimentacao tipo,
                                         String motivo, Instant ocorridoEm, Instant agora, UUID responsavelId,
                                         List<AlteracaoAlocacao> itens) {
        Momento.exigirNaoFuturo(ocorridoEm, agora, "Data da movimentação");
        Verificar.naoVazia(itens, "Animais");
        return new Movimentacao(id, organizacaoId, propriedadeId, Verificar.obrigatorio(tipo, "Tipo"),
                Verificar.texto(motivo, "Motivo", 300), ocorridoEm, agora,
                Verificar.obrigatorio(responsavelId, "Responsável"), List.copyOf(itens));
    }
}
