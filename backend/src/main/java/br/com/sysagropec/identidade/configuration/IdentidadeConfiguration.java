package br.com.sysagropec.identidade.configuration;

import br.com.sysagropec.identidade.application.port.in.AutenticacaoUseCase;
import br.com.sysagropec.identidade.application.port.in.ContextoExecucaoQuery;
import br.com.sysagropec.identidade.application.port.in.InicializacaoUseCase;
import br.com.sysagropec.identidade.application.port.in.SenhaUseCase;
import br.com.sysagropec.identidade.application.port.in.UsuariosInternosQuery;
import br.com.sysagropec.identidade.application.port.in.UsuariosUseCase;
import br.com.sysagropec.identidade.application.port.out.IdentidadePorts.NotificacaoPort;
import br.com.sysagropec.identidade.application.port.out.IdentidadePorts.OrganizacaoRepositoryPort;
import br.com.sysagropec.identidade.application.port.out.IdentidadePorts.PropriedadesPort;
import br.com.sysagropec.identidade.application.port.out.IdentidadePorts.SenhaHasherPort;
import br.com.sysagropec.identidade.application.port.out.IdentidadePorts.TokenSenhaRepositoryPort;
import br.com.sysagropec.identidade.application.port.out.IdentidadePorts.UsuarioRepositoryPort;
import br.com.sysagropec.identidade.application.usecase.AutenticacaoService;
import br.com.sysagropec.identidade.application.usecase.ContextoService;
import br.com.sysagropec.identidade.application.usecase.InicializacaoService;
import br.com.sysagropec.identidade.application.usecase.SenhaService;
import br.com.sysagropec.identidade.application.usecase.UsuariosService;
import br.com.sysagropec.shared.application.port.out.AuditoriaPort;
import br.com.sysagropec.shared.application.port.out.RelogioPort;
import br.com.sysagropec.shared.application.port.out.TransacaoPort;
import br.com.sysagropec.shared.configuration.Transacional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Autenticação, senha e usuários controlam as próprias transações (via TransacaoPort) porque
 * auditam falhas e enviam e-mails fora da transação. As consultas usam o decorador transacional.
 */
@Configuration(proxyBeanMethods = false)
class IdentidadeConfiguration {

    @Bean
    ContextoExecucaoQuery contextoExecucaoQuery(UsuarioRepositoryPort usuarios, OrganizacaoRepositoryPort organizacoes,
                                                PropriedadesPort propriedades, PlatformTransactionManager tm) {
        return Transacional.leitura(ContextoExecucaoQuery.class,
                new ContextoService(usuarios, organizacoes, propriedades), tm);
    }

    @Bean
    UsuariosInternosQuery usuariosInternosQuery(UsuarioRepositoryPort usuarios, OrganizacaoRepositoryPort organizacoes,
                                                PropriedadesPort propriedades, PlatformTransactionManager tm) {
        return Transacional.escrita(UsuariosInternosQuery.class,
                new ContextoService(usuarios, organizacoes, propriedades), tm);
    }

    @Bean
    AutenticacaoUseCase autenticacaoUseCase(UsuarioRepositoryPort usuarios, SenhaHasherPort hasher,
                                            AuditoriaPort auditoria, RelogioPort relogio, TransacaoPort transacao) {
        return new AutenticacaoService(usuarios, hasher, auditoria, relogio, transacao);
    }

    @Bean
    SenhaService senhaService(UsuarioRepositoryPort usuarios, TokenSenhaRepositoryPort tokens, SenhaHasherPort hasher,
                              NotificacaoPort notificacao, AuditoriaPort auditoria, RelogioPort relogio,
                              TransacaoPort transacao, @Value("${sysagropec.url-frontend}") String urlFrontend) {
        return new SenhaService(usuarios, tokens, hasher, notificacao, auditoria, relogio, transacao, urlFrontend);
    }

    @Bean
    UsuariosUseCase usuariosUseCase(UsuarioRepositoryPort usuarios, TokenSenhaRepositoryPort tokens,
                                    PropriedadesPort propriedades, SenhaService senhas, AuditoriaPort auditoria,
                                    RelogioPort relogio, TransacaoPort transacao) {
        return new UsuariosService(usuarios, tokens, propriedades, senhas, auditoria, relogio, transacao);
    }

    @Bean
    InicializacaoUseCase inicializacaoUseCase(OrganizacaoRepositoryPort organizacoes, UsuarioRepositoryPort usuarios,
                                              SenhaHasherPort hasher, AuditoriaPort auditoria, RelogioPort relogio,
                                              PlatformTransactionManager tm) {
        return Transacional.escrita(InicializacaoUseCase.class,
                new InicializacaoService(organizacoes, usuarios, hasher, auditoria, relogio), tm);
    }
}
