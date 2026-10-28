package br.com.sysagropec.pesagem.adapter.out.persistence;

import br.com.sysagropec.pesagem.application.port.out.PesagemPorts.PesagemRepositoryPort;
import br.com.sysagropec.pesagem.domain.model.Pesagem;
import br.com.sysagropec.shared.domain.ConflitoException;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Component
class PesagemPersistenceAdapter implements PesagemRepositoryPort {

    private final SpringDataPesagens repo;

    PesagemPersistenceAdapter(SpringDataPesagens repo) {
        this.repo = repo;
    }

    @Override
    public void salvar(Pesagem p) {
        PesagemJpaEntity e = repo.findById(p.id()).orElse(null);
        if (e == null) {
            e = new PesagemJpaEntity();
            e.id = p.id();
            e.organizacaoId = p.organizacaoId();
            e.propriedadeId = p.propriedadeId();
            e.animalId = p.animalId();
            e.pesoKg = p.pesoKg();
            e.natureza = p.natureza().name();
            e.metodo = p.metodo().name();
            e.finalidade = p.finalidade().name();
            e.equipamento = p.equipamento();
            e.observacao = p.observacao();
            e.ocorridoEm = p.ocorridoEm();
            e.registradoEm = p.registradoEm();
            e.responsavelId = p.responsavelId();
            e.tarefaId = p.tarefaId();
        } else if (!Objects.equals(e.versao, p.versao())) {
            throw new ConflitoException("A pesagem foi alterada por outra operação. Atualize e tente novamente");
        }
        e.canceladaEm = p.canceladaEm();
        e.canceladaPor = p.canceladaPor();
        e.motivoCancelamento = p.motivoCancelamento();
        repo.saveAndFlush(e);
    }

    @Override
    public Optional<Pesagem> buscar(UUID organizacaoId, UUID pesagemId) {
        return repo.findByOrganizacaoIdAndId(organizacaoId, pesagemId).map(PesagemPersistenceAdapter::paraDominio);
    }

    @Override
    public List<Pesagem> listarPorAnimal(UUID organizacaoId, UUID animalId) {
        return repo.findByOrganizacaoIdAndAnimalId(organizacaoId, animalId).stream()
                .map(PesagemPersistenceAdapter::paraDominio).toList();
    }

    @Override
    public boolean existeEntradaValida(UUID animalId) {
        return repo.existeEntradaValida(animalId);
    }

    private static Pesagem paraDominio(PesagemJpaEntity e) {
        return Pesagem.reconstituir(e.id, e.organizacaoId, e.propriedadeId, e.animalId, e.pesoKg,
                Pesagem.Natureza.valueOf(e.natureza), Pesagem.Metodo.valueOf(e.metodo),
                Pesagem.Finalidade.valueOf(e.finalidade), e.equipamento, e.observacao, e.ocorridoEm, e.registradoEm,
                e.responsavelId, e.tarefaId, e.canceladaEm, e.canceladaPor, e.motivoCancelamento, e.versao);
    }
}

interface SpringDataPesagens extends JpaRepository<PesagemJpaEntity, UUID> {

    Optional<PesagemJpaEntity> findByOrganizacaoIdAndId(UUID organizacaoId, UUID id);

    List<PesagemJpaEntity> findByOrganizacaoIdAndAnimalId(UUID organizacaoId, UUID animalId);

    @Query("""
            select count(p) > 0 from PesagemJpaEntity p
            where p.animalId = ?1 and p.finalidade = 'ENTRADA' and p.canceladaEm is null
            """)
    boolean existeEntradaValida(UUID animalId);
}
