package br.com.sysagropec.rebanho.configuration;

import br.com.sysagropec.rebanho.application.port.in.RebanhoConsultas;
import br.com.sysagropec.rebanho.application.port.in.RebanhoInternoQuery;
import br.com.sysagropec.rebanho.application.port.in.RebanhoUseCase;
import br.com.sysagropec.rebanho.application.port.out.RebanhoPorts.AnimalRepositoryPort;
import br.com.sysagropec.rebanho.application.port.out.RebanhoPorts.LoteRepositoryPort;
import br.com.sysagropec.rebanho.application.port.out.RebanhoPorts.MovimentacaoRepositoryPort;
import br.com.sysagropec.rebanho.application.port.out.RebanhoPorts.PropriedadesPort;
import br.com.sysagropec.rebanho.application.port.out.RebanhoPorts.RebanhoLeituraPort;
import br.com.sysagropec.rebanho.application.usecase.RebanhoConsultasService;
import br.com.sysagropec.rebanho.application.usecase.RebanhoService;
import br.com.sysagropec.shared.application.port.out.AuditoriaPort;
import br.com.sysagropec.shared.application.port.out.IdempotenciaPort;
import br.com.sysagropec.shared.application.port.out.RelogioPort;
import br.com.sysagropec.shared.configuration.Transacional;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration(proxyBeanMethods = false)
class RebanhoConfiguration {

    @Bean
    RebanhoUseCase rebanhoUseCase(AnimalRepositoryPort animais, LoteRepositoryPort lotes,
                                  MovimentacaoRepositoryPort movimentacoes, PropriedadesPort propriedades,
                                  IdempotenciaPort idempotencia, AuditoriaPort auditoria, RelogioPort relogio,
                                  PlatformTransactionManager tm) {
        return Transacional.escrita(RebanhoUseCase.class, new RebanhoService(animais, lotes, movimentacoes,
                propriedades, idempotencia, auditoria, relogio), tm);
    }

    @Bean
    RebanhoConsultas rebanhoConsultas(RebanhoLeituraPort leitura, AnimalRepositoryPort animais,
                                      LoteRepositoryPort lotes, PlatformTransactionManager tm) {
        return Transacional.leitura(RebanhoConsultas.class, new RebanhoConsultasService(leitura, animais, lotes), tm);
    }

    @Bean
    RebanhoInternoQuery rebanhoInternoQuery(RebanhoLeituraPort leitura, AnimalRepositoryPort animais,
                                            LoteRepositoryPort lotes, PlatformTransactionManager tm) {
        return Transacional.escrita(RebanhoInternoQuery.class, new RebanhoConsultasService(leitura, animais, lotes),
                tm);
    }
}
