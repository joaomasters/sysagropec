package br.com.sysagropec.identidade.adapter.out.persistence;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "usuario")
class UsuarioJpaEntity {

    @Id
    UUID id;
    @Column(name = "organizacao_id", nullable = false, updatable = false)
    UUID organizacaoId;
    String nome;
    String email;
    @Column(name = "senha_hash")
    String senhaHash;
    boolean administrador;
    boolean ativo;
    @Column(name = "criado_em", updatable = false)
    Instant criadoEm;
    @Column(name = "atualizado_em")
    Instant atualizadoEm;
    @Version
    Long versao;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "acesso_propriedade", joinColumns = @JoinColumn(name = "usuario_id"))
    Set<AcessoEmbeddable> acessos = new HashSet<>();

    @Embeddable
    static class AcessoEmbeddable {
        @Column(name = "organizacao_id")
        UUID organizacaoId;
        @Column(name = "propriedade_id")
        UUID propriedadeId;
        String perfil;

        AcessoEmbeddable() {
        }

        AcessoEmbeddable(UUID organizacaoId, UUID propriedadeId, String perfil) {
            this.organizacaoId = organizacaoId;
            this.propriedadeId = propriedadeId;
            this.perfil = perfil;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof AcessoEmbeddable a && propriedadeId.equals(a.propriedadeId) && perfil.equals(a.perfil);
        }

        @Override
        public int hashCode() {
            return propriedadeId.hashCode() * 31 + perfil.hashCode();
        }
    }
}
