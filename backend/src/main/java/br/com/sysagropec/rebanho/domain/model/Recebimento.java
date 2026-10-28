package br.com.sysagropec.rebanho.domain.model;

import br.com.sysagropec.shared.domain.Momento;
import br.com.sysagropec.shared.domain.Verificar;

import java.time.Instant;
import java.util.UUID;

/** Entrada de um grupo de animais na propriedade, com procedência e documento de trânsito quando houver. */
public record Recebimento(UUID id, UUID organizacaoId, UUID propriedadeId, UUID instalacaoId, UUID loteId,
                          UUID movimentacaoId, Instant ocorridoEm, Instant registradoEm, String origemDescricao,
                          String documento, String observacao, int quantidade, UUID responsavelId) {

    public static final int MAXIMO_ANIMAIS = 500;

    public static Recebimento registrar(UUID id, UUID organizacaoId, UUID propriedadeId, UUID instalacaoId,
                                        UUID loteId, UUID movimentacaoId, Instant ocorridoEm, Instant agora,
                                        String origemDescricao, String documento, String observacao, int quantidade,
                                        UUID responsavelId) {
        Momento.exigirNaoFuturo(ocorridoEm, agora, "Data do recebimento");
        Verificar.regra(quantidade > 0, "Informe ao menos um animal");
        Verificar.regra(quantidade <= MAXIMO_ANIMAIS,
                "Um recebimento aceita no máximo " + MAXIMO_ANIMAIS + " animais");
        return new Recebimento(id, organizacaoId, propriedadeId, Verificar.obrigatorio(instalacaoId, "Instalação"),
                loteId, movimentacaoId, ocorridoEm, agora,
                Verificar.texto(origemDescricao, "Origem/procedência", 200),
                Verificar.textoOpcional(documento, "Documento", 60),
                Verificar.textoOpcional(observacao, "Observação", 500),
                quantidade, responsavelId);
    }
}
