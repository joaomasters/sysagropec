package br.com.sysagropec.shared.application.seguranca;

import br.com.sysagropec.shared.domain.NaoEncontradoException;
import br.com.sysagropec.shared.domain.acesso.Permissao;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Quem executa a operação, montado no servidor a partir da sessão autenticada.
 * Nunca é derivado de identificadores enviados pelo cliente.
 */
public record ContextoExecucao(
        UUID usuarioId,
        UUID organizacaoId,
        String nomeUsuario,
        boolean administrador,
        Map<UUID, Set<Permissao>> permissoesPorPropriedade) {

    public ContextoExecucao {
        Objects.requireNonNull(usuarioId);
        Objects.requireNonNull(organizacaoId);
        permissoesPorPropriedade = Map.copyOf(permissoesPorPropriedade);
    }

    /**
     * Exige a permissão na propriedade. Sem nenhum acesso à propriedade, responde como inexistente
     * para não revelar recursos de terceiros; com acesso mas sem a operação, nega explicitamente.
     */
    public void exigir(Permissao permissao, UUID propriedadeId) {
        Set<Permissao> permissoes = propriedadeId == null ? null : permissoesPorPropriedade.get(propriedadeId);
        if (permissoes == null || permissoes.isEmpty()) {
            throw new NaoEncontradoException("Propriedade");
        }
        if (!permissoes.contains(permissao)) {
            throw new AcessoNegadoException("Seu perfil não permite esta operação nesta propriedade");
        }
    }

    public void exigirNaOrganizacao(Permissao permissao) {
        if (permissao.escopo() != Permissao.Escopo.ORGANIZACAO || !administrador) {
            throw new AcessoNegadoException("Operação restrita ao administrador da organização");
        }
    }

    public boolean pode(Permissao permissao, UUID propriedadeId) {
        return permissoesPorPropriedade.getOrDefault(propriedadeId, Set.of()).contains(permissao);
    }

    public Set<UUID> propriedadesCom(Permissao permissao) {
        return permissoesPorPropriedade.entrySet().stream()
                .filter(e -> e.getValue().contains(permissao))
                .map(Map.Entry::getKey)
                .collect(Collectors.toUnmodifiableSet());
    }
}
