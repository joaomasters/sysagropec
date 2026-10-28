package br.com.sysagropec.shared.adapter.out.persistence;

import br.com.sysagropec.shared.application.port.out.IdempotenciaPort;
import br.com.sysagropec.shared.domain.ConflitoException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
class IdempotenciaJdbcAdapter implements IdempotenciaPort {

    private final JdbcClient jdbc;

    IdempotenciaJdbcAdapter(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<UUID> resultadoAnterior(UUID organizacaoId, String escopo, String chave, String hashRequisicao) {
        return jdbc.sql("""
                        select hash_requisicao, resultado_id from idempotencia
                        where organizacao_id = :org and escopo = :escopo and chave = :chave
                        """)
                .param("org", organizacaoId)
                .param("escopo", escopo)
                .param("chave", chave)
                .query((rs, n) -> {
                    if (!rs.getString("hash_requisicao").equals(hashRequisicao)) {
                        throw new ConflitoException("A chave de idempotência já foi usada com outro conteúdo");
                    }
                    return rs.getObject("resultado_id", UUID.class);
                })
                .optional();
    }

    /** A chave primária impede que duas requisições simultâneas com a mesma chave sejam confirmadas. */
    @Override
    public void registrar(UUID organizacaoId, String escopo, String chave, String hashRequisicao, UUID resultadoId) {
        jdbc.sql("""
                        insert into idempotencia (organizacao_id, escopo, chave, hash_requisicao, resultado_id, criado_em)
                        values (:org, :escopo, :chave, :hash, :resultado, now())
                        """)
                .param("org", organizacaoId)
                .param("escopo", escopo)
                .param("chave", chave)
                .param("hash", hashRequisicao)
                .param("resultado", resultadoId)
                .update();
    }
}
