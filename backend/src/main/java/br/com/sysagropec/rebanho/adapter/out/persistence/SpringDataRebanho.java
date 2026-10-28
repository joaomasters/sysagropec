package br.com.sysagropec.rebanho.adapter.out.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface SpringDataAnimais extends JpaRepository<AnimalJpaEntity, UUID> {

    Optional<AnimalJpaEntity> findByOrganizacaoIdAndId(UUID organizacaoId, UUID id);

    List<AnimalJpaEntity> findByOrganizacaoIdAndIdIn(UUID organizacaoId, Collection<UUID> ids);

    List<AnimalJpaEntity> findByOrganizacaoIdAndRecebimentoId(UUID organizacaoId, UUID recebimentoId);

    @Query("""
            select lower(i.valor) from IdentificacaoJpaEntity i
            where i.organizacaoId = ?1 and i.tipo = ?2 and i.fim is null and lower(i.valor) in ?3
            """)
    List<String> identificacoesEmUso(UUID organizacaoId, String tipo, Collection<String> valoresMinusculos);

    @Query("select count(l) from LocalizacaoJpaEntity l where l.instalacaoId = ?1 and l.fim is null")
    long contarNaInstalacao(UUID instalacaoId);

    @Query("select count(v) from VinculoLoteJpaEntity v where v.loteId = ?1 and v.fim is null")
    long contarNoLote(UUID loteId);
}

interface SpringDataLotes extends JpaRepository<LoteJpaEntity, UUID> {

    Optional<LoteJpaEntity> findByOrganizacaoIdAndId(UUID organizacaoId, UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select l from LoteJpaEntity l where l.organizacaoId = ?1 and l.id = ?2")
    Optional<LoteJpaEntity> buscarComBloqueio(UUID organizacaoId, UUID id);

    @Query("""
            select count(l) > 0 from LoteJpaEntity l
            where l.propriedadeId = ?1 and lower(l.codigo) = lower(?2) and l.status = 'ATIVO'
            """)
    boolean existeCodigoAtivo(UUID propriedadeId, String codigo);
}

interface SpringDataMovimentacoes extends JpaRepository<MovimentacaoJpaEntity, UUID> {
}

interface SpringDataRecebimentos extends JpaRepository<RecebimentoJpaEntity, UUID> {

    Optional<RecebimentoJpaEntity> findByOrganizacaoIdAndId(UUID organizacaoId, UUID id);
}
