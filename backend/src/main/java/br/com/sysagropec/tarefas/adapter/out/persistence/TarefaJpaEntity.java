package br.com.sysagropec.tarefas.adapter.out.persistence;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "tarefa")
class TarefaJpaEntity {

    @Id
    UUID id;
    @Column(name = "organizacao_id", updatable = false)
    UUID organizacaoId;
    @Column(name = "propriedade_id", updatable = false)
    UUID propriedadeId;
    @Column(updatable = false)
    String tipo;
    String titulo;
    String descricao;
    @Column(name = "alvo_tipo", updatable = false)
    String alvoTipo;
    @Column(name = "alvo_id", updatable = false)
    UUID alvoId;
    @Column(name = "responsavel_id")
    UUID responsavelId;
    Instant prazo;
    String prioridade;
    String status;
    @Column(name = "criada_por", updatable = false)
    UUID criadaPor;
    @Column(name = "criada_em", updatable = false)
    Instant criadaEm;
    @Column(name = "iniciada_em")
    Instant iniciadaEm;
    @Column(name = "iniciada_por")
    UUID iniciadaPor;
    @Column(name = "finalizada_em")
    Instant finalizadaEm;
    @Column(name = "finalizada_por")
    UUID finalizadaPor;
    @Column(name = "observacao_conclusao")
    String observacaoConclusao;
    String justificativa;
    @Column(name = "atualizado_em")
    Instant atualizadoEm;
    @Version
    Long versao;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "tarefa_id", nullable = false, updatable = false)
    @OrderBy("ordem")
    List<ItemJpaEntity> checklist = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "tarefa_resultado", joinColumns = @JoinColumn(name = "tarefa_id"))
    Set<ResultadoEmbeddable> resultados = new HashSet<>();

    @Entity
    @Table(name = "tarefa_checklist_item")
    static class ItemJpaEntity {
        @Id
        UUID id;
        @Column(updatable = false)
        int ordem;
        @Column(updatable = false)
        String descricao;
        @Column(updatable = false)
        boolean obrigatorio;
        boolean concluido;
        @Column(name = "concluido_por")
        UUID concluidoPor;
        @Column(name = "concluido_em")
        Instant concluidoEm;
    }

    @Embeddable
    static class ResultadoEmbeddable {
        String tipo;
        @Column(name = "resultado_id")
        UUID resultadoId;
        @Column(name = "vinculado_em")
        Instant vinculadoEm;

        @Override
        public boolean equals(Object o) {
            return o instanceof ResultadoEmbeddable r && tipo.equals(r.tipo) && resultadoId.equals(r.resultadoId);
        }

        @Override
        public int hashCode() {
            return tipo.hashCode() * 31 + resultadoId.hashCode();
        }
    }
}
