package br.com.sysagropec.shared.adapter.out.persistence;

import br.com.sysagropec.shared.application.port.out.AuditoriaPort;
import org.slf4j.MDC;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.sql.Timestamp;
import java.util.Map;
import java.util.UUID;

@Component
class AuditoriaJdbcAdapter implements AuditoriaPort {

    private final JdbcClient jdbc;
    private final JsonMapper json;

    AuditoriaJdbcAdapter(JdbcClient jdbc, JsonMapper json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    @Override
    public void registrar(EventoAuditoria evento) {
        Map<String, Object> detalhes = evento.detalhes() == null ? Map.of() : evento.detalhes();
        jdbc.sql("""
                        insert into auditoria_evento
                            (id, organizacao_id, usuario_id, acao, recurso_tipo, recurso_id, detalhes, ocorrido_em, request_id)
                        values (:id, :org, :usuario, :acao, :tipo, :recurso, cast(:detalhes as jsonb), :quando, :requestId)
                        """)
                .param("id", UUID.randomUUID())
                .param("org", evento.organizacaoId())
                .param("usuario", evento.usuarioId())
                .param("acao", evento.acao())
                .param("tipo", evento.recursoTipo())
                .param("recurso", evento.recursoId())
                .param("detalhes", json.writeValueAsString(detalhes))
                .param("quando", Timestamp.from(evento.ocorridoEm()))
                .param("requestId", MDC.get("requestId"))
                .update();
    }
}
