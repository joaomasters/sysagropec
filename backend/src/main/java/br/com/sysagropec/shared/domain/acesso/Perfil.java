package br.com.sysagropec.shared.domain.acesso;

import java.util.EnumSet;
import java.util.Set;

import static br.com.sysagropec.shared.domain.acesso.Permissao.*;

/**
 * Perfis atribuídos por propriedade. O administrador não é um perfil por propriedade:
 * é um atributo do usuário na organização e recebe todas as permissões em todas as propriedades.
 */
public enum Perfil {
    GESTOR(EnumSet.of(CONSULTAR, INSTALACAO_GERIR, ANIMAL_RECEBER, ANIMAL_MOVIMENTAR, LOTE_GERIR,
            PESAGEM_REGISTRAR, PESAGEM_CANCELAR, TAREFA_GERIR, TAREFA_EXECUTAR)),
    VETERINARIO(EnumSet.of(CONSULTAR, PESAGEM_REGISTRAR, TAREFA_GERIR, TAREFA_EXECUTAR)),
    NUTRICIONISTA(EnumSet.of(CONSULTAR, PESAGEM_REGISTRAR, TAREFA_GERIR, TAREFA_EXECUTAR)),
    FINANCEIRO(EnumSet.of(CONSULTAR)),
    OPERADOR(EnumSet.of(CONSULTAR, ANIMAL_MOVIMENTAR, PESAGEM_REGISTRAR, TAREFA_EXECUTAR)),
    CONSULTA(EnumSet.of(CONSULTAR));

    private final Set<Permissao> permissoes;

    Perfil(Set<Permissao> permissoes) {
        this.permissoes = Set.copyOf(permissoes);
    }

    public Set<Permissao> permissoes() {
        return permissoes;
    }

    public static Set<Permissao> todasDePropriedade() {
        EnumSet<Permissao> todas = EnumSet.noneOf(Permissao.class);
        for (Permissao p : Permissao.values()) {
            if (p.escopo() == Permissao.Escopo.PROPRIEDADE) {
                todas.add(p);
            }
        }
        return todas;
    }
}
