package br.com.sysagropec.identidade.domain.model;

import br.com.sysagropec.shared.domain.acesso.Perfil;
import br.com.sysagropec.shared.domain.RegraNegocioException;
import br.com.sysagropec.shared.domain.Verificar;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Usuário de uma organização. O acesso é concedido por propriedade, com um perfil em cada uma.
 * O administrador da organização acessa todas as propriedades e não recebe perfis individuais.
 */
public class Usuario {

    private final UUID id;
    private final UUID organizacaoId;
    private final String nome;
    private final String email;
    private String senhaHash;
    private final boolean administrador;
    private boolean ativo;
    private final Instant criadoEm;
    private final Map<UUID, Perfil> acessos;
    private final Long versao;

    private Usuario(UUID id, UUID organizacaoId, String nome, String email, String senhaHash, boolean administrador,
                    boolean ativo, Instant criadoEm, Map<UUID, Perfil> acessos, Long versao) {
        this.id = Objects.requireNonNull(id);
        this.organizacaoId = Objects.requireNonNull(organizacaoId);
        this.nome = nome;
        this.email = email;
        this.senhaHash = senhaHash;
        this.administrador = administrador;
        this.ativo = ativo;
        this.criadoEm = criadoEm;
        this.acessos = new LinkedHashMap<>(acessos);
        this.versao = versao;
    }

    /** Novo usuário sem senha: a senha é definida pelo próprio usuário a partir do convite. */
    public static Usuario novo(UUID id, UUID organizacaoId, String nome, String email, boolean administrador,
                               Instant agora) {
        return new Usuario(id, organizacaoId, Verificar.texto(nome, "Nome", 120), Email.normalizar(email), null,
                administrador, true, agora, Map.of(), null);
    }

    public static Usuario reconstituir(UUID id, UUID organizacaoId, String nome, String email, String senhaHash,
                                       boolean administrador, boolean ativo, Instant criadoEm,
                                       Map<UUID, Perfil> acessos, Long versao) {
        return new Usuario(id, organizacaoId, nome, email, senhaHash, administrador, ativo, criadoEm, acessos, versao);
    }

    public void definirSenhaHash(String hash) {
        this.senhaHash = Verificar.obrigatorio(hash, "Senha");
    }

    public void concederAcesso(UUID propriedadeId, Perfil perfil) {
        Verificar.obrigatorio(propriedadeId, "Propriedade");
        Verificar.obrigatorio(perfil, "Perfil");
        Verificar.regra(!administrador, "O administrador já tem acesso a todas as propriedades");
        Verificar.regra(ativo, "Usuário inativo");
        acessos.put(propriedadeId, perfil);
    }

    public void revogarAcesso(UUID propriedadeId) {
        if (acessos.remove(propriedadeId) == null) {
            throw new RegraNegocioException("O usuário não possui acesso a esta propriedade");
        }
    }

    public void desativar(UUID solicitanteId) {
        Verificar.regra(!id.equals(solicitanteId), "Você não pode desativar o próprio usuário");
        Verificar.regra(ativo, "Usuário já está inativo");
        this.ativo = false;
    }

    public boolean podeAutenticar() {
        return ativo && senhaHash != null;
    }

    public boolean aguardandoAtivacao() {
        return senhaHash == null;
    }

    public UUID id() { return id; }
    public UUID organizacaoId() { return organizacaoId; }
    public String nome() { return nome; }
    public String email() { return email; }
    public String senhaHash() { return senhaHash; }
    public boolean administrador() { return administrador; }
    public boolean ativo() { return ativo; }
    public Instant criadoEm() { return criadoEm; }
    public Map<UUID, Perfil> acessos() { return Collections.unmodifiableMap(acessos); }
    public Long versao() { return versao; }
}
