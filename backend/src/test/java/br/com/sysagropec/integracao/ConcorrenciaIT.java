package br.com.sysagropec.integracao;

import br.com.sysagropec.identidade.application.port.in.ContextoExecucaoQuery;
import br.com.sysagropec.rebanho.application.port.in.RebanhoUseCase;
import br.com.sysagropec.rebanho.application.port.in.RebanhoUseCase.AnimalRecebido;
import br.com.sysagropec.rebanho.application.port.in.RebanhoUseCase.NovoAnimal;
import br.com.sysagropec.shared.application.seguranca.ContextoExecucao;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpSession;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Operações simultâneas não podem produzir estados inválidos: a verificação prévia não basta,
 * o bloqueio da instalação e as restrições do banco devem garantir as regras.
 */
class ConcorrenciaIT extends IntegracaoBase {

    @Autowired
    RebanhoUseCase rebanho;
    @Autowired
    ContextoExecucaoQuery contextos;

    @Test
    void movimentacoesSimultaneasNaoExcedemALotacao() throws Exception {
        Usuario admin = novaOrganizacaoComAdmin();
        MockHttpSession sessao = login(admin);
        UUID propriedade = criarPropriedade(sessao);
        UUID origem = criarCurral(sessao, propriedade, "ORIGEM", 50);
        UUID destino = criarCurral(sessao, propriedade, "DESTINO", 3);
        ContextoExecucao ctx = contextos.obter(admin.id()).orElseThrow();

        var recebidos = rebanho.receber(ctx, new RebanhoUseCase.ReceberAnimaisComando(propriedade, origem, null,
                Instant.now().minusSeconds(3600), null, "F", null, null, null,
                List.of(novo("C-1"), novo("C-2"), novo("C-3"), novo("C-4")))).animais();
        List<UUID> ids = recebidos.stream().map(AnimalRecebido::id).toList();

        // Duas requisições, cada uma com 2 animais, para um curral de capacidade 3.
        List<Callable<UUID>> tarefas = List.of(
                () -> mover(ctx, propriedade, ids.subList(0, 2), destino),
                () -> mover(ctx, propriedade, ids.subList(2, 4), destino));
        List<Throwable> falhas = executarJuntas(tarefas);

        assertThat(falhas).hasSize(1);
        long ocupacao = jdbc.sql("select count(*) from animal_localizacao where instalacao_id = :d and fim is null")
                .param("d", destino).query(Long.class).single();
        assertThat(ocupacao).isEqualTo(2);
        assertThat(jdbc.sql("select count(*) from animal_localizacao where fim is null and animal_id in (:ids)")
                .param("ids", ids).query(Long.class).single()).isEqualTo(4);
    }

    @Test
    void recebimentosSimultaneosComOMesmoBrincoCriamApenasUmAnimal() throws Exception {
        Usuario admin = novaOrganizacaoComAdmin();
        MockHttpSession sessao = login(admin);
        UUID propriedade = criarPropriedade(sessao);
        UUID curral = criarCurral(sessao, propriedade, "C1", 50);
        ContextoExecucao ctx = contextos.obter(admin.id()).orElseThrow();

        Callable<UUID> receber = () -> rebanho.receber(ctx, new RebanhoUseCase.ReceberAnimaisComando(propriedade,
                curral, null, Instant.now().minusSeconds(60), null, "F", null, null, null,
                List.of(novo("DUP-1")))).recebimentoId();
        List<Throwable> falhas = executarJuntas(List.of(receber, receber, receber));

        assertThat(falhas).hasSize(2);
        assertThat(jdbc.sql("select count(*) from animal_identificacao where organizacao_id = :o and valor = 'DUP-1'")
                .param("o", admin.organizacaoId()).query(Long.class).single()).isEqualTo(1);
    }

    private UUID mover(ContextoExecucao ctx, UUID propriedade, List<UUID> animais, UUID destino) {
        return rebanho.movimentar(ctx, new RebanhoUseCase.MovimentarAnimaisComando(propriedade, animais, null, false,
                destino, "Teste de concorrência", Instant.now()));
    }

    private static NovoAnimal novo(String brinco) {
        return new NovoAnimal(brinco, null, "MACHO", "GARROTE", "ENGORDA", null, null, false);
    }

    private static List<Throwable> executarJuntas(List<Callable<UUID>> tarefas) throws Exception {
        CountDownLatch largada = new CountDownLatch(1);
        try (ExecutorService pool = Executors.newFixedThreadPool(tarefas.size())) {
            List<Future<UUID>> futuros = new ArrayList<>();
            for (Callable<UUID> t : tarefas) {
                futuros.add(pool.submit(() -> {
                    largada.await();
                    return t.call();
                }));
            }
            largada.countDown();
            List<Throwable> falhas = new ArrayList<>();
            for (Future<UUID> f : futuros) {
                try {
                    f.get();
                } catch (java.util.concurrent.ExecutionException e) {
                    falhas.add(e.getCause());
                }
            }
            return falhas;
        }
    }
}
