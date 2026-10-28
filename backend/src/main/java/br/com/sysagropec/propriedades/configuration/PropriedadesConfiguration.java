package br.com.sysagropec.propriedades.configuration;

import br.com.sysagropec.propriedades.application.port.in.PropriedadesInternasQuery;
import br.com.sysagropec.propriedades.application.port.in.PropriedadesUseCase;
import br.com.sysagropec.propriedades.application.port.out.PropriedadeRepositoryPort;
import br.com.sysagropec.propriedades.application.usecase.PropriedadesService;
import br.com.sysagropec.shared.application.port.out.AuditoriaPort;
import br.com.sysagropec.shared.application.port.out.RelogioPort;
import br.com.sysagropec.shared.configuration.Transacional;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Composição do módulo. O serviço não é exposto como bean: apenas as portas de entrada,
 * já envolvidas pela fronteira transacional.
 */
@Configuration(proxyBeanMethods = false)
class PropriedadesConfiguration {

    private final PropriedadesService service;
    private final PlatformTransactionManager tm;

    PropriedadesConfiguration(PropriedadeRepositoryPort repositorio, AuditoriaPort auditoria, RelogioPort relogio,
                              PlatformTransactionManager tm) {
        this.service = new PropriedadesService(repositorio, auditoria, relogio);
        this.tm = tm;
    }

    @Bean
    PropriedadesUseCase propriedadesUseCase() {
        return Transacional.escrita(PropriedadesUseCase.class, service, tm);
    }

    @Bean
    PropriedadesUseCase.Consultas propriedadesConsultas() {
        return Transacional.leitura(PropriedadesUseCase.Consultas.class, service, tm);
    }

    /**
     * Participa da transação de quem chama (propagação REQUIRED), o que permite ao bloqueio da
     * instalação valer até o fim do caso de uso do módulo chamador.
     */
    @Bean
    PropriedadesInternasQuery propriedadesInternasQuery() {
        return Transacional.escrita(PropriedadesInternasQuery.class, service, tm);
    }
}
