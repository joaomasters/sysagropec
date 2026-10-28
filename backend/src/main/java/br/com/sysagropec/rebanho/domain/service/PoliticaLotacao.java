package br.com.sysagropec.rebanho.domain.service;

import br.com.sysagropec.shared.domain.RegraNegocioException;

/**
 * Lotação da instalação em cabeças, somando todos os lotes presentes (premissa: currais podem ser compartilhados).
 * Sem capacidade definida, a ocupação não é limitada.
 */
public final class PoliticaLotacao {

    private PoliticaLotacao() {
    }

    public static void verificar(String codigoInstalacao, Integer capacidade, long ocupacaoAtual, long entrando) {
        if (capacidade == null || entrando <= 0) {
            return;
        }
        if (ocupacaoAtual + entrando > capacidade) {
            throw new RegraNegocioException("Lotação excedida em " + codigoInstalacao + ": capacidade " + capacidade
                    + ", ocupação atual " + ocupacaoAtual + ", entrada de " + entrando);
        }
    }
}
