package br.com.sysagropec.rebanho.domain.model;

/** Classificações do rebanho. Valores iniciais; a lista deve ser validada com o produtor. */
public final class Enums {

    private Enums() {
    }

    public enum Sexo { MACHO, FEMEA }

    public enum Categoria {
        BEZERRO(Sexo.MACHO), BEZERRA(Sexo.FEMEA),
        GARROTE(Sexo.MACHO), NOVILHA(Sexo.FEMEA),
        BOI(Sexo.MACHO), VACA(Sexo.FEMEA),
        TOURO(Sexo.MACHO);

        private final Sexo sexo;

        Categoria(Sexo sexo) {
            this.sexo = sexo;
        }

        public boolean compativelCom(Sexo sexo) {
            return this.sexo == sexo;
        }
    }

    public enum Finalidade { ENGORDA, RECRIA, REPRODUCAO }

    public enum SituacaoAnimal { ATIVO, VENDIDO, ABATIDO, MORTO, BAIXADO }

    public enum OrigemAnimal { COMPRA, NASCIMENTO_PROPRIO, TRANSFERENCIA, OUTRA }

    public enum TipoIdentificacao { BRINCO, ELETRONICO }

    public enum FaseLote { RECEPCAO, ADAPTACAO, CRESCIMENTO, TERMINACAO, RECRIA, OUTRA }

    public enum StatusLote { ATIVO, ENCERRADO }

    public enum TipoMovimentacao { RECEBIMENTO, FORMACAO_LOTE, TRANSFERENCIA_INTERNA }
}
