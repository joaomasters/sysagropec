package br.com.sysagropec.integracao;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.flywaydb.core.Flyway;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * As migrações sobem em banco novo (contexto da aplicação) e o schema confere com o mapeamento JPA
 * (ddl-auto=validate). Reexecutar o Flyway não aplica nada novo.
 */
class MigracoesIT extends IntegracaoBase {

    @Autowired
    Flyway flyway;

    @Test
    void migracoesAplicadasEValidas() {
        assertThat(flyway.info().applied()).hasSizeGreaterThanOrEqualTo(5);
        assertThat(flyway.info().pending()).isEmpty();
        assertThat(flyway.migrate().migrationsExecuted).isZero();
        flyway.validate();
    }
}
