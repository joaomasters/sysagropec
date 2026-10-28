package br.com.sysagropec.tarefas.configuration;

import br.com.sysagropec.shared.application.port.out.AuditoriaPort;
import br.com.sysagropec.shared.application.port.out.RelogioPort;
import br.com.sysagropec.shared.configuration.Transacional;
import br.com.sysagropec.tarefas.application.port.in.TarefasResultadoUseCase;
import br.com.sysagropec.tarefas.application.port.in.TarefasUseCase;
import br.com.sysagropec.tarefas.application.port.out.TarefasPorts.AlvosPort;
import br.com.sysagropec.tarefas.application.port.out.TarefasPorts.PropriedadesPort;
import br.com.sysagropec.tarefas.application.port.out.TarefasPorts.TarefaRepositoryPort;
import br.com.sysagropec.tarefas.application.port.out.TarefasPorts.UsuariosPort;
import br.com.sysagropec.tarefas.application.usecase.TarefasService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration(proxyBeanMethods = false)
class TarefasConfiguration {

    private final TarefasService service;
    private final PlatformTransactionManager tm;

    TarefasConfiguration(TarefaRepositoryPort tarefas, AlvosPort alvos, UsuariosPort usuarios,
                         PropriedadesPort propriedades, AuditoriaPort auditoria, RelogioPort relogio,
                         PlatformTransactionManager tm) {
        this.service = new TarefasService(tarefas, alvos, usuarios, propriedades, auditoria, relogio);
        this.tm = tm;
    }

    @Bean
    TarefasUseCase tarefasUseCase() {
        return Transacional.escrita(TarefasUseCase.class, service, tm);
    }

    @Bean
    TarefasUseCase.Consultas tarefasConsultas() {
        return Transacional.leitura(TarefasUseCase.Consultas.class, service, tm);
    }

    /** Participa da transação do módulo chamador (ex.: registro de pesagem). */
    @Bean
    TarefasResultadoUseCase tarefasResultadoUseCase() {
        return Transacional.escrita(TarefasResultadoUseCase.class, service, tm);
    }
}
