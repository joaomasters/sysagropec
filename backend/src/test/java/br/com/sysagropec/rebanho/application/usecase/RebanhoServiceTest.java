package br.com.sysagropec.rebanho.application.usecase;

import br.com.sysagropec.rebanho.application.port.in.RebanhoUseCase;
import br.com.sysagropec.rebanho.application.port.in.RebanhoUseCase.NovoAnimal;
import br.com.sysagropec.rebanho.application.port.in.RebanhoUseCase.ReceberAnimaisComando;
import br.com.sysagropec.rebanho.application.port.out.RebanhoPorts.AnimalRepositoryPort;
import br.com.sysagropec.rebanho.application.port.out.RebanhoPorts.LoteRepositoryPort;
import br.com.sysagropec.rebanho.application.port.out.RebanhoPorts.MovimentacaoRepositoryPort;
import br.com.sysagropec.rebanho.application.port.out.RebanhoPorts.PropriedadesPort;
import br.com.sysagropec.rebanho.domain.model.Animal;
import br.com.sysagropec.rebanho.domain.model.Enums.TipoIdentificacao;
import br.com.sysagropec.rebanho.domain.model.Lote;
import br.com.sysagropec.rebanho.domain.model.Movimentacao;
import br.com.sysagropec.rebanho.domain.model.Recebimento;
import br.com.sysagropec.shared.application.port.out.AuditoriaPort;
import br.com.sysagropec.shared.application.port.out.IdempotenciaPort;
import br.com.sysagropec.shared.application.seguranca.AcessoNegadoException;
import br.com.sysagropec.shared.application.seguranca.ContextoExecucao;
import br.com.sysagropec.shared.domain.ConflitoException;
import br.com.sysagropec.shared.domain.NaoEncontradoException;
import br.com.sysagropec.shared.domain.RegraNegocioException;
import br.com.sysagropec.shared.domain.acesso.Perfil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Caso de uso exercitado com adaptadores em memória: sem Spring e sem banco. */
class RebanhoServiceTest {

    static final Instant AGORA = Instant.parse("2026-10-20T12:00:00Z");
    static final UUID ORG = UUID.randomUUID();
    static final UUID PROPRIEDADE = UUID.randomUUID();
    static final UUID CURRAL = UUID.randomUUID();
    static final UUID CURRAL_PEQUENO = UUID.randomUUID();

    final Animais animais = new Animais();
    final Lotes lotes = new Lotes();
    final List<Movimentacao> movimentacoes = new ArrayList<>();
    final List<Recebimento> recebimentos = new ArrayList<>();
    final Map<String, UUID> chaves = new HashMap<>();
    final List<AuditoriaPort.EventoAuditoria> auditoria = new ArrayList<>();

    RebanhoService service;
    ContextoExecucao gestor;

    @BeforeEach
    void preparar() {
        PropriedadesPort propriedades = new PropriedadesPort() {
            @Override
            public Optional<InstalacaoAlocavel> instalacaoComBloqueio(UUID org, UUID id) {
                if (id.equals(CURRAL)) {
                    return Optional.of(new InstalacaoAlocavel(CURRAL, PROPRIEDADE, "C1", 100, true));
                }
                if (id.equals(CURRAL_PEQUENO)) {
                    return Optional.of(new InstalacaoAlocavel(CURRAL_PEQUENO, PROPRIEDADE, "C2", 2, true));
                }
                return Optional.empty();
            }

            @Override
            public ZoneId fusoHorario(UUID org, UUID propriedadeId) {
                return ZoneId.of("America/Sao_Paulo");
            }
        };
        MovimentacaoRepositoryPort registros = new MovimentacaoRepositoryPort() {
            @Override
            public void salvar(Movimentacao m) {
                movimentacoes.add(m);
            }

            @Override
            public void salvar(Recebimento r) {
                recebimentos.add(r);
            }

            @Override
            public Optional<Recebimento> buscarRecebimento(UUID org, UUID id) {
                return recebimentos.stream().filter(r -> r.id().equals(id)).findFirst();
            }
        };
        IdempotenciaPort idempotencia = new IdempotenciaPort() {
            @Override
            public Optional<UUID> resultadoAnterior(UUID org, String escopo, String chave, String hash) {
                return Optional.ofNullable(chaves.get(escopo + chave + hash));
            }

            @Override
            public void registrar(UUID org, String escopo, String chave, String hash, UUID resultado) {
                chaves.put(escopo + chave + hash, resultado);
            }
        };
        service = new RebanhoService(animais, lotes, registros, propriedades, idempotencia, auditoria::add,
                () -> AGORA);
        gestor = new ContextoExecucao(UUID.randomUUID(), ORG, "Gestor", false,
                Map.of(PROPRIEDADE, Perfil.GESTOR.permissoes()));
    }

    ReceberAnimaisComando recebimento(UUID curral, String chave, String... brincos) {
        List<NovoAnimal> novos = java.util.Arrays.stream(brincos)
                .map(b -> new NovoAnimal(b, null, "MACHO", "GARROTE", "ENGORDA", "Nelore", null, false))
                .toList();
        return new ReceberAnimaisComando(PROPRIEDADE, curral, null, AGORA.minusSeconds(3600), "COMPRA",
                "Fornecedor X", "GTA-1", null, chave, novos);
    }

    @Test
    void recebimentoCriaAnimaisNumeradosMovimentacaoEAuditoria() {
        var resultado = service.receber(gestor, recebimento(CURRAL, null, "A1", "A2", "A3"));

        assertThat(resultado.animais()).extracting(RebanhoUseCase.AnimalRecebido::numero).containsExactly(1L, 2L, 3L);
        assertThat(animais.dados.values()).allSatisfy(a -> assertThat(a.instalacaoAtual()).contains(CURRAL));
        assertThat(movimentacoes).singleElement().satisfies(m -> assertThat(m.itens()).hasSize(3));
        assertThat(recebimentos).singleElement().satisfies(r -> assertThat(r.quantidade()).isEqualTo(3));
        assertThat(auditoria).extracting(AuditoriaPort.EventoAuditoria::acao).containsExactly("ANIMAIS_RECEBIDOS");
    }

    @Test
    void brincoRepetidoOuEmUsoEhRejeitado() {
        assertThatThrownBy(() -> service.receber(gestor, recebimento(CURRAL, null, "A1", "a1")))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("repetida");

        service.receber(gestor, recebimento(CURRAL, null, "A1"));
        assertThatThrownBy(() -> service.receber(gestor, recebimento(CURRAL, null, "A1")))
                .isInstanceOf(ConflitoException.class)
                .hasMessageContaining("a1");
    }

    @Test
    void lotacaoDoCurralEhRespeitada() {
        assertThatThrownBy(() -> service.receber(gestor, recebimento(CURRAL_PEQUENO, null, "A1", "A2", "A3")))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("Lotação excedida");
        assertThat(animais.dados).isEmpty();
    }

    @Test
    void reenvioComMesmaChaveDevolveORecebimentoOriginal() {
        var primeiro = service.receber(gestor, recebimento(CURRAL, "chave-1", "A1", "A2"));
        var segundo = service.receber(gestor, recebimento(CURRAL, "chave-1", "A1", "A2"));

        assertThat(segundo.recebimentoId()).isEqualTo(primeiro.recebimentoId());
        assertThat(animais.dados).hasSize(2);
    }

    @Test
    void operadorNaoRecebeAnimaisEUsuarioSemAcessoNaoVeAPropriedade() {
        ContextoExecucao operador = new ContextoExecucao(UUID.randomUUID(), ORG, "Op", false,
                Map.of(PROPRIEDADE, Perfil.OPERADOR.permissoes()));
        ContextoExecucao outro = new ContextoExecucao(UUID.randomUUID(), ORG, "Outro", false, Map.of());

        assertThatThrownBy(() -> service.receber(operador, recebimento(CURRAL, null, "A1")))
                .isInstanceOf(AcessoNegadoException.class);
        assertThatThrownBy(() -> service.receber(outro, recebimento(CURRAL, null, "A1")))
                .isInstanceOf(NaoEncontradoException.class);
    }

    @Test
    void formacaoDeLoteMoveOsAnimaisERegistraAMovimentacao() {
        var recebidos = service.receber(gestor, recebimento(CURRAL, null, "A1", "A2"));
        List<UUID> ids = recebidos.animais().stream().map(RebanhoUseCase.AnimalRecebido::id).toList();

        var formado = service.formarLote(gestor, new RebanhoUseCase.FormarLoteComando(PROPRIEDADE, "L1",
                "ADAPTACAO", "ENGORDA", null, AGORA.minusSeconds(60), ids, CURRAL_PEQUENO, null));

        assertThat(ids).allSatisfy(id -> {
            assertThat(animais.dados.get(id).loteAtual()).contains(formado.loteId());
            assertThat(animais.dados.get(id).instalacaoAtual()).contains(CURRAL_PEQUENO);
        });
        assertThat(movimentacoes).hasSize(2);
        assertThat(movimentacoes.getLast().motivo()).isEqualTo("Formação do lote L1");
    }

    @Test
    void movimentacaoComFalhaEmUmAnimalNaoAlteraNenhumRegistro() {
        var recebidos = service.receber(gestor, recebimento(CURRAL, null, "A1"));
        UUID existente = recebidos.animais().getFirst().id();

        assertThatThrownBy(() -> service.movimentar(gestor, new RebanhoUseCase.MovimentarAnimaisComando(PROPRIEDADE,
                List.of(existente, UUID.randomUUID()), null, false, CURRAL_PEQUENO, "Teste", AGORA)))
                .isInstanceOf(NaoEncontradoException.class);
        assertThat(animais.dados.get(existente).instalacaoAtual()).contains(CURRAL);
        assertThat(movimentacoes).hasSize(1);
    }

    static class Animais implements AnimalRepositoryPort {
        final Map<UUID, Animal> dados = new HashMap<>();
        long sequencia;

        @Override
        public void salvar(Animal animal) {
            dados.put(animal.id(), animal);
        }

        @Override
        public Optional<Animal> buscar(UUID org, UUID id) {
            return Optional.ofNullable(dados.get(id)).filter(a -> a.organizacaoId().equals(org));
        }

        @Override
        public List<Animal> buscarVarios(UUID org, Collection<UUID> ids) {
            return ids.stream().map(dados::get).filter(Objects::nonNull).toList();
        }

        @Override
        public List<Animal> listarPorRecebimento(UUID org, UUID recebimentoId) {
            return dados.values().stream().filter(a -> recebimentoId.equals(a.recebimentoId())).toList();
        }

        @Override
        public Set<String> identificacoesEmUso(UUID org, TipoIdentificacao tipo, Collection<String> valores) {
            return dados.values().stream()
                    .flatMap(a -> a.identificacaoAtiva(tipo).stream())
                    .map(v -> v.toLowerCase(Locale.ROOT))
                    .filter(valores::contains)
                    .collect(Collectors.toSet());
        }

        @Override
        public long contarNaInstalacao(UUID instalacaoId) {
            return dados.values().stream().filter(a -> a.instalacaoAtual().map(instalacaoId::equals).orElse(false))
                    .count();
        }

        @Override
        public long contarNoLote(UUID loteId) {
            return dados.values().stream().filter(a -> a.loteAtual().map(loteId::equals).orElse(false)).count();
        }

        @Override
        public long reservarNumeros(UUID org, int quantidade) {
            long primeiro = sequencia + 1;
            sequencia += quantidade;
            return primeiro;
        }
    }

    static class Lotes implements LoteRepositoryPort {
        final Map<UUID, Lote> dados = new HashMap<>();

        @Override
        public void salvar(Lote lote) {
            dados.put(lote.id(), lote);
        }

        @Override
        public Optional<Lote> buscarLote(UUID org, UUID id) {
            return Optional.ofNullable(dados.get(id));
        }

        @Override
        public Optional<Lote> buscarLoteComBloqueio(UUID org, UUID id) {
            return buscarLote(org, id);
        }

        @Override
        public boolean existeCodigoAtivo(UUID propriedadeId, String codigo) {
            return dados.values().stream().anyMatch(l -> l.codigo().equalsIgnoreCase(codigo));
        }
    }
}
