package br.com.sysagropec.pesagem.configuration;

import br.com.sysagropec.pesagem.application.port.in.PesagemUseCase;
import br.com.sysagropec.pesagem.application.port.out.PesagemPorts.AnimaisPort;
import br.com.sysagropec.pesagem.application.port.out.PesagemPorts.PesagemRepositoryPort;
import br.com.sysagropec.pesagem.application.port.out.PesagemPorts.TarefasPort;
import br.com.sysagropec.pesagem.application.usecase.PesagemService;
import br.com.sysagropec.shared.application.port.out.AuditoriaPort;
import br.com.sysagropec.shared.application.port.out.IdempotenciaPort;
import br.com.sysagropec.shared.application.port.out.RelogioPort;
import br.com.sysagropec.shared.configuration.Transacional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration(proxyBeanMethods = false)
class PesagemConfiguration {

    private final PesagemService service;
    private final PlatformTransactionManager tm;

    PesagemConfiguration(PesagemRepositoryPort pesagens, AnimaisPort animais, TarefasPort tarefas,
                         IdempotenciaPort idempotencia, AuditoriaPort auditoria, RelogioPort relogio,
                         PlatformTransactionManager tm) {
        this.service = new PesagemService(pesagens, animais, tarefas, idempotencia, auditoria, relogio);
        this.tm = tm;
    }

    @Bean
    PesagemUseCase pesagemUseCase() {
        return Transacional.escrita(PesagemUseCase.class, service, tm);
    }

    @Bean
    PesagemUseCase.Consultas pesagemConsultas() {
        return Transacional.leitura(PesagemUseCase.Consultas.class, service, tm);
    }
}
