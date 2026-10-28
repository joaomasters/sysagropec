package br.com.sysagropec.rebanho.adapter.out.persistence;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "animal")
class AnimalJpaEntity {

    @Id
    UUID id;
    @Column(name = "organizacao_id", nullable = false, updatable = false)
    UUID organizacaoId;
    @Column(name = "propriedade_id", nullable = false)
    UUID propriedadeId;
    @Column(updatable = false)
    long numero;
    String sexo;
    String categoria;
    String finalidade;
    String raca;
    @Column(name = "data_nascimento")
    LocalDate dataNascimento;
    @Column(name = "nascimento_estimado")
    boolean nascimentoEstimado;
    String origem;
    @Column(name = "origem_descricao")
    String origemDescricao;
    @Column(name = "recebimento_id", updatable = false)
    UUID recebimentoId;
    String situacao;
    @Column(name = "criado_em", updatable = false)
    Instant criadoEm;
    @Column(name = "atualizado_em")
    Instant atualizadoEm;
    @Version
    Long versao;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "animal_id", nullable = false, updatable = false)
    List<IdentificacaoJpaEntity> identificacoes = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "animal_id", nullable = false, updatable = false)
    List<VinculoLoteJpaEntity> vinculosLote = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "animal_id", nullable = false, updatable = false)
    List<LocalizacaoJpaEntity> localizacoes = new ArrayList<>();
}
