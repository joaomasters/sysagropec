package br.com.sysagropec.shared.configuration;

import br.com.sysagropec.shared.application.port.out.RelogioPort;
import br.com.sysagropec.shared.application.port.out.TransacaoPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Configuration(proxyBeanMethods = false)
public class SharedConfiguration {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    /** Microssegundos: a mesma precisão do timestamptz do PostgreSQL, evitando diferenças após o round-trip. */
    @Bean
    RelogioPort relogioPort(Clock clock) {
        return () -> Instant.now(clock).truncatedTo(ChronoUnit.MICROS);
    }

    @Bean
    TransacaoPort transacaoPort(PlatformTransactionManager tm) {
        TransactionTemplate template = new TransactionTemplate(tm);
        return new TransacaoPort() {
            @Override
            public <T> T executar(java.util.function.Supplier<T> operacao) {
                return template.execute(status -> operacao.get());
            }
        };
    }
}
