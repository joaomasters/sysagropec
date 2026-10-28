package br.com.sysagropec.consultas.configuration;

import br.com.sysagropec.consultas.application.port.in.ConsultasQuery;
import br.com.sysagropec.consultas.application.port.out.LeituraConsolidadaPort;
import br.com.sysagropec.consultas.application.usecase.ConsultasService;
import br.com.sysagropec.shared.application.port.out.RelogioPort;
import br.com.sysagropec.shared.configuration.Transacional;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration(proxyBeanMethods = false)
class ConsultasConfiguration {

    /** Somente leitura: uma transação read-only garante um retrato consistente entre as várias consultas. */
    @Bean
    ConsultasQuery consultasQuery(LeituraConsolidadaPort leitura, RelogioPort relogio, PlatformTransactionManager tm) {
        return Transacional.leitura(ConsultasQuery.class, new ConsultasService(leitura, relogio), tm);
    }
}
