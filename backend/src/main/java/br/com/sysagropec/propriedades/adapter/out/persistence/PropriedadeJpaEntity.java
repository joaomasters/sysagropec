package br.com.sysagropec.propriedades.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "propriedade")
class PropriedadeJpaEntity {

    @Id
    UUID id;
    @Column(name = "organizacao_id", nullable = false, updatable = false)
    UUID organizacaoId;
    String nome;
    String municipio;
    String uf;
    @Column(name = "fuso_horario")
    String fusoHorario;
    boolean ativa;
    @Column(name = "criado_em", updatable = false)
    Instant criadoEm;
    @Column(name = "atualizado_em")
    Instant atualizadoEm;
    @Version
    Long versao;
}
