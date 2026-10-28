package br.com.sysagropec.identidade.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface SpringDataUsuarios extends JpaRepository<UsuarioJpaEntity, UUID> {

    Optional<UsuarioJpaEntity> findByEmail(String email);

    Optional<UsuarioJpaEntity> findByOrganizacaoIdAndId(UUID organizacaoId, UUID id);

    List<UsuarioJpaEntity> findByOrganizacaoId(UUID organizacaoId);
}

interface SpringDataTokensSenha extends JpaRepository<TokenSenhaJpaEntity, UUID> {

    Optional<TokenSenhaJpaEntity> findByTokenHashAndInvalidadoEmIsNull(String tokenHash);

    @Modifying(flushAutomatically = true, clearAutomatically = false)
    @Query("""
            update TokenSenhaJpaEntity t set t.invalidadoEm = ?2
            where t.usuarioId = ?1 and t.usadoEm is null and t.invalidadoEm is null
            """)
    int invalidarPendentes(UUID usuarioId, Instant agora);
}
