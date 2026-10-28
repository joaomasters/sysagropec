package br.com.sysagropec.demo;

import br.com.sysagropec.identidade.application.port.in.ContextoExecucaoQuery;
import br.com.sysagropec.identidade.application.port.in.InicializacaoUseCase;
import br.com.sysagropec.pesagem.application.port.in.PesagemUseCase;
import br.com.sysagropec.propriedades.application.port.in.PropriedadesUseCase;
import br.com.sysagropec.propriedades.application.port.in.PropriedadesUseCase.InstalacaoResumo;
import br.com.sysagropec.rebanho.application.port.in.RebanhoUseCase;
import br.com.sysagropec.rebanho.application.port.in.RebanhoUseCase.AnimalRecebido;
import br.com.sysagropec.shared.application.seguranca.ContextoExecucao;
import br.com.sysagropec.tarefas.application.port.in.TarefasUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Dados de demonstração, ativados apenas no perfil "demo" e somente em banco vazio.
 * Os nomes deixam explícito que são fictícios. Tudo é criado pelos casos de uso reais.
 */
@Component
@Profile("demo")
@Order(20)
class DadosDemonstracaoRunner implements ApplicationRunner {

    static final String EMAIL_ADMIN = "admin@demo.sysagropec.local";
    static final String SENHA_DEMO = "demonstracao-2026";

    private static final Logger log = LoggerFactory.getLogger(DadosDemonstracaoRunner.class);

    private final InicializacaoUseCase inicializacao;
    private final ContextoExecucaoQuery contextos;

    private final PropriedadesUseCase propriedades;
    private final RebanhoUseCase rebanho;
    private final PesagemUseCase pesagens;
    private final TarefasUseCase tarefas;

    DadosDemonstracaoRunner(InicializacaoUseCase inicializacao, ContextoExecucaoQuery contextos,
                            PropriedadesUseCase propriedades, RebanhoUseCase rebanho,
                            PesagemUseCase pesagens, TarefasUseCase tarefas) {
        this.inicializacao = inicializacao;
        this.contextos = contextos;
        this.propriedades = propriedades;
        this.rebanho = rebanho;
        this.pesagens = pesagens;
        this.tarefas = tarefas;
    }

    @Override
    public void run(ApplicationArguments args) {
        Optional<InicializacaoUseCase.Inicializacao> inicial = inicializacao.inicializarSeVazio(
                "Organização de demonstração (dados fictícios)", "Administrador Demonstração", EMAIL_ADMIN,
                SENHA_DEMO);
        if (inicial.isEmpty()) {
            log.info("Banco já possui dados; demonstração não carregada");
            return;
        }
        UUID adminId = inicial.get().administradorId();
        ContextoExecucao admin = contextos.obter(adminId).orElseThrow();

        var fazenda = propriedades.cadastrar(admin, new PropriedadesUseCase.CadastrarPropriedadeComando(
                "Fazenda Demonstração (fictícia)", "Exemplo", "GO", "America/Sao_Paulo"));
        admin = contextos.obter(adminId).orElseThrow();
        List<InstalacaoResumo> currais = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            currais.add(propriedades.cadastrarInstalacao(admin, new PropriedadesUseCase.CadastrarInstalacaoComando(
                    fazenda.id(), "CURRAL", "C" + i, "Curral de demonstração " + i, 40)));
        }

        Instant entrada = Instant.now().minus(Duration.ofDays(20)).truncatedTo(ChronoUnit.HOURS);
        List<RebanhoUseCase.NovoAnimal> animais = new ArrayList<>();
        for (int i = 1; i <= 12; i++) {
            animais.add(new RebanhoUseCase.NovoAnimal("DEMO-" + String.format("%03d", i), null, "MACHO", "GARROTE",
                    "ENGORDA", i % 2 == 0 ? "Nelore" : "Angus x Nelore",
                    LocalDate.now().minusMonths(18), true));
        }
        var recebimento = rebanho.receber(admin, new RebanhoUseCase.ReceberAnimaisComando(fazenda.id(),
                currais.getFirst().id(), null, entrada, "COMPRA", "Fornecedor fictício de demonstração", "DEMO-GTA-1",
                "Recebimento de demonstração", null, animais));
        List<UUID> ids = recebimento.animais().stream().map(AnimalRecebido::id).toList();

        var lote = rebanho.formarLote(admin, new RebanhoUseCase.FormarLoteComando(fazenda.id(), "L-DEMO-01",
                "ADAPTACAO", "ENGORDA", "Lote de demonstração", entrada.plus(Duration.ofHours(2)), ids,
                currais.get(1).id(), "Formação do lote de demonstração"));

        for (int i = 0; i < ids.size(); i++) {
            pesagens.registrar(admin, new PesagemUseCase.RegistrarPesagemComando(ids.get(i),
                    new BigDecimal(300 + i * 7), "MEDIDO", "BALANCA_INDIVIDUAL", "ENTRADA", "Balança de demonstração",
                    null, entrada.plus(Duration.ofHours(1)), null, null));
        }

        Instant hoje = Instant.now();
        tarefas.criar(admin, new TarefasUseCase.CriarTarefaComando(fazenda.id(), "LEITURA_COCHO",
                "Leitura de cocho - manhã", null, "LOTE", lote.loteId(), null, hoje.plus(Duration.ofHours(2)), "ALTA",
                List.of(new TarefasUseCase.ItemChecklistNovo("Registrar sobra observada", true))));
        tarefas.criar(admin, new TarefasUseCase.CriarTarefaComando(fazenda.id(), "INSPECAO_AGUA",
                "Inspeção de bebedouros", null, "INSTALACAO", currais.get(1).id(), null,
                hoje.minus(Duration.ofHours(3)), "MEDIA", List.of()));
        tarefas.criar(admin, new TarefasUseCase.CriarTarefaComando(fazenda.id(), "PESAGEM",
                "Pesagem de acompanhamento DEMO-001", null, "ANIMAL", ids.getFirst(), null,
                hoje.plus(Duration.ofHours(4)), "MEDIA", List.of()));

        log.info("Dados de demonstração carregados. Login: {} (senha documentada no README)", EMAIL_ADMIN);
    }
}
