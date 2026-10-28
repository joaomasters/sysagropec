package br.com.sysagropec.identidade.adapter.in.startup;

import br.com.sysagropec.identidade.application.port.in.InicializacaoUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Cria a primeira organização e o administrador a partir de variáveis de ambiente,
 * somente se o banco estiver vazio. A senha nunca é registrada em log.
 */
@Component
@Order(10)
class InicializacaoRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(InicializacaoRunner.class);

    private final InicializacaoUseCase inicializacao;
    private final Environment env;

    InicializacaoRunner(InicializacaoUseCase inicializacao, Environment env) {
        this.inicializacao = inicializacao;
        this.env = env;
    }

    @Override
    public void run(ApplicationArguments args) {
        String email = env.getProperty("sysagropec.inicializacao.admin-email", "");
        String senha = env.getProperty("sysagropec.inicializacao.admin-senha", "");
        if (email.isBlank() || senha.isBlank()) {
            return;
        }
        inicializacao.inicializarSeVazio(
                        env.getProperty("sysagropec.inicializacao.organizacao", "Minha organização"),
                        env.getProperty("sysagropec.inicializacao.admin-nome", "Administrador"),
                        email, senha)
                .ifPresentOrElse(
                        i -> log.info("Organização inicial criada (id={}) com administrador {}", i.organizacaoId(), email),
                        () -> log.info("Banco já inicializado; nenhum administrador criado"));
    }
}
