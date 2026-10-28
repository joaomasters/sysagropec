package br.com.sysagropec.tarefas.adapter.out.persistence;

import br.com.sysagropec.shared.application.Pagina;
import br.com.sysagropec.shared.domain.ConflitoException;
import br.com.sysagropec.tarefas.application.port.out.TarefasPorts.ConsultaTarefas;
import br.com.sysagropec.tarefas.application.port.out.TarefasPorts.TarefaRepositoryPort;
import br.com.sysagropec.tarefas.domain.model.Tarefa;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Component
class TarefaPersistenceAdapter implements TarefaRepositoryPort {

    private final SpringDataTarefas repo;
    private final EntityManager em;

    TarefaPersistenceAdapter(SpringDataTarefas repo, EntityManager em) {
        this.repo = repo;
        this.em = em;
    }

    @Override
    public void salvar(Tarefa t) {
        TarefaJpaEntity e = repo.findById(t.id()).orElse(null);
        if (e == null) {
            e = new TarefaJpaEntity();
            e.id = t.id();
            e.organizacaoId = t.organizacaoId();
            e.propriedadeId = t.propriedadeId();
            e.tipo = t.tipo().name();
            e.alvoTipo = t.alvo().tipo().name();
            e.alvoId = t.alvo().id();
            e.criadaPor = t.criadaPor();
            e.criadaEm = t.criadaEm();
        } else if (!Objects.equals(e.versao, t.versao())) {
            throw new ConflitoException("A tarefa foi alterada por outra operação. Atualize e tente novamente");
        }
        e.titulo = t.titulo();
        e.descricao = t.descricao();
        e.responsavelId = t.responsavelId();
        e.prazo = t.prazo();
        e.prioridade = t.prioridade().name();
        e.status = t.status().name();
        e.iniciadaEm = t.iniciadaEm();
        e.iniciadaPor = t.iniciadaPor();
        e.finalizadaEm = t.finalizadaEm();
        e.finalizadaPor = t.finalizadaPor();
        e.observacaoConclusao = t.observacaoConclusao();
        e.justificativa = t.justificativa();
        e.atualizadoEm = Instant.now();

        for (Tarefa.ItemChecklist item : t.checklist()) {
            TarefaJpaEntity.ItemJpaEntity x = e.checklist.stream().filter(i -> i.id.equals(item.id())).findFirst()
                    .orElse(null);
            if (x == null) {
                x = new TarefaJpaEntity.ItemJpaEntity();
                x.id = item.id();
                x.ordem = item.ordem();
                x.descricao = item.descricao();
                x.obrigatorio = item.obrigatorio();
                e.checklist.add(x);
            }
            x.concluido = item.concluido();
            x.concluidoPor = item.concluidoPor();
            x.concluidoEm = item.concluidoEm();
        }
        for (Tarefa.Resultado r : t.resultados()) {
            TarefaJpaEntity.ResultadoEmbeddable x = new TarefaJpaEntity.ResultadoEmbeddable();
            x.tipo = r.tipo();
            x.resultadoId = r.id();
            x.vinculadoEm = r.vinculadoEm();
            e.resultados.add(x);
        }
        repo.saveAndFlush(e);
    }

    @Override
    public Optional<Tarefa> buscar(UUID organizacaoId, UUID tarefaId) {
        return repo.findByOrganizacaoIdAndId(organizacaoId, tarefaId).map(TarefaPersistenceAdapter::paraDominio);
    }

    @Override
    public Pagina<Tarefa> listar(ConsultaTarefas c) {
        CriteriaBuilder cb = em.getCriteriaBuilder();

        CriteriaQuery<Long> contagem = cb.createQuery(Long.class);
        Root<TarefaJpaEntity> rc = contagem.from(TarefaJpaEntity.class);
        contagem.select(cb.count(rc)).where(filtros(cb, rc, c));
        long total = em.createQuery(contagem).getSingleResult();

        CriteriaQuery<TarefaJpaEntity> consulta = cb.createQuery(TarefaJpaEntity.class);
        Root<TarefaJpaEntity> r = consulta.from(TarefaJpaEntity.class);
        consulta.select(r).where(filtros(cb, r, c)).orderBy(cb.asc(r.get("prazo")), cb.asc(r.get("titulo")));
        List<Tarefa> itens = em.createQuery(consulta)
                .setFirstResult(c.pagina() * c.tamanho())
                .setMaxResults(c.tamanho())
                .getResultList().stream()
                .map(TarefaPersistenceAdapter::paraDominio)
                .toList();
        return new Pagina<>(itens, c.pagina(), c.tamanho(), total);
    }

    private static Predicate[] filtros(CriteriaBuilder cb, Root<TarefaJpaEntity> r, ConsultaTarefas c) {
        List<Predicate> p = new ArrayList<>();
        p.add(cb.equal(r.get("organizacaoId"), c.organizacaoId()));
        p.add(cb.equal(r.get("propriedadeId"), c.propriedadeId()));
        if (c.status() != null && !c.status().isEmpty()) {
            p.add(r.get("status").in(c.status().stream().map(Enum::name).toList()));
        }
        if (c.responsavelId() != null) {
            p.add(cb.equal(r.get("responsavelId"), c.responsavelId()));
        }
        if (c.alvoTipo() != null) {
            p.add(cb.equal(r.get("alvoTipo"), c.alvoTipo().name()));
        }
        if (c.alvoId() != null) {
            p.add(cb.equal(r.get("alvoId"), c.alvoId()));
        }
        if (c.prazoAntes() != null) {
            p.add(cb.lessThan(r.get("prazo"), c.prazoAntes()));
        }
        if (c.finalizadaDesde() != null) {
            p.add(cb.greaterThanOrEqualTo(r.get("finalizadaEm"), c.finalizadaDesde()));
        }
        return p.toArray(Predicate[]::new);
    }

    private static Tarefa paraDominio(TarefaJpaEntity e) {
        return Tarefa.reconstituir(e.id, e.organizacaoId, e.propriedadeId, Tarefa.Tipo.valueOf(e.tipo), e.titulo,
                e.descricao, new Tarefa.Alvo(Tarefa.TipoAlvo.valueOf(e.alvoTipo), e.alvoId), e.responsavelId,
                e.prazo, Tarefa.Prioridade.valueOf(e.prioridade), Tarefa.Status.valueOf(e.status), e.criadaPor,
                e.criadaEm, e.iniciadaEm, e.iniciadaPor, e.finalizadaEm, e.finalizadaPor, e.observacaoConclusao,
                e.justificativa,
                e.checklist.stream()
                        .sorted(Comparator.comparingInt(i -> i.ordem))
                        .map(i -> Tarefa.ItemChecklist.reconstituir(i.id, i.ordem, i.descricao, i.obrigatorio,
                                i.concluido, i.concluidoPor, i.concluidoEm))
                        .toList(),
                e.resultados.stream()
                        .sorted(Comparator.comparing(x -> x.vinculadoEm))
                        .map(x -> new Tarefa.Resultado(x.tipo, x.resultadoId, x.vinculadoEm))
                        .toList(),
                e.versao);
    }
}

interface SpringDataTarefas extends JpaRepository<TarefaJpaEntity, UUID> {

    Optional<TarefaJpaEntity> findByOrganizacaoIdAndId(UUID organizacaoId, UUID id);
}
