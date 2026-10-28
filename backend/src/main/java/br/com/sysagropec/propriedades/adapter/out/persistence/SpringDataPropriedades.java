package br.com.sysagropec.propriedades.adapter.out.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface SpringDataPropriedades extends JpaRepository<PropriedadeJpaEntity, UUID> {

    Optional<PropriedadeJpaEntity> findByOrganizacaoIdAndId(UUID organizacaoId, UUID id);

    List<PropriedadeJpaEntity> findByOrganizacaoIdAndIdIn(UUID organizacaoId, Collection<UUID> ids);

    List<PropriedadeJpaEntity> findByOrganizacaoIdAndAtivaTrue(UUID organizacaoId);

    @Query("select count(p) > 0 from PropriedadeJpaEntity p where p.organizacaoId = ?1 and lower(p.nome) = lower(?2)")
    boolean existeNome(UUID organizacaoId, String nome);
}

interface SpringDataInstalacoes extends JpaRepository<InstalacaoJpaEntity, UUID> {

    Optional<InstalacaoJpaEntity> findByOrganizacaoIdAndId(UUID organizacaoId, UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from InstalacaoJpaEntity i where i.organizacaoId = ?1 and i.id = ?2")
    Optional<InstalacaoJpaEntity> buscarComBloqueio(UUID organizacaoId, UUID id);

    List<InstalacaoJpaEntity> findByOrganizacaoIdAndPropriedadeId(UUID organizacaoId, UUID propriedadeId);

    @Query("select count(i) > 0 from InstalacaoJpaEntity i where i.propriedadeId = ?1 and lower(i.codigo) = lower(?2)")
    boolean existeCodigo(UUID propriedadeId, String codigo);
}
