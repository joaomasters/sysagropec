package br.com.sysagropec.shared.domain.acesso;

/** Operações autorizáveis. As de escopo ORGANIZACAO são exclusivas do administrador. */
public enum Permissao {
    CONSULTAR(Escopo.PROPRIEDADE),
    INSTALACAO_GERIR(Escopo.PROPRIEDADE),
    ANIMAL_RECEBER(Escopo.PROPRIEDADE),
    ANIMAL_MOVIMENTAR(Escopo.PROPRIEDADE),
    LOTE_GERIR(Escopo.PROPRIEDADE),
    PESAGEM_REGISTRAR(Escopo.PROPRIEDADE),
    PESAGEM_CANCELAR(Escopo.PROPRIEDADE),
    TAREFA_GERIR(Escopo.PROPRIEDADE),
    TAREFA_EXECUTAR(Escopo.PROPRIEDADE),
    PROPRIEDADE_GERIR(Escopo.ORGANIZACAO),
    USUARIO_GERIR(Escopo.ORGANIZACAO);

    public enum Escopo { PROPRIEDADE, ORGANIZACAO }

    private final Escopo escopo;

    Permissao(Escopo escopo) {
        this.escopo = escopo;
    }

    public Escopo escopo() {
        return escopo;
    }
}
