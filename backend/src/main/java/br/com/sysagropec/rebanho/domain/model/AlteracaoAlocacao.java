package br.com.sysagropec.rebanho.domain.model;

import java.util.Objects;
import java.util.UUID;

/** Estado de lote e instalação de um animal antes e depois de uma movimentação. */
public record AlteracaoAlocacao(UUID animalId, UUID loteOrigemId, UUID loteDestinoId, UUID instalacaoOrigemId,
                                UUID instalacaoDestinoId) {

    public boolean mudouLote() {
        return !Objects.equals(loteOrigemId, loteDestinoId);
    }

    public boolean mudouInstalacao() {
        return !Objects.equals(instalacaoOrigemId, instalacaoDestinoId);
    }
}
