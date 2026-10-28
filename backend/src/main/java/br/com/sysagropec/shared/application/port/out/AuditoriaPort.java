package br.com.sysagropec.shared.application.port.out;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/** Registro de ações críticas. Grava na mesma transação da operação auditada. */
public interface AuditoriaPort {

    void registrar(EventoAuditoria evento);

    record EventoAuditoria(
            UUID organizacaoId,
            UUID usuarioId,
            String acao,
            String recursoTipo,
            UUID recursoId,
            Map<String, Object> detalhes,
            Instant ocorridoEm) {
    }
}
