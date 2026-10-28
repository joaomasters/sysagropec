package br.com.sysagropec.rebanho.adapter.out.persistence;

import br.com.sysagropec.rebanho.application.port.out.RebanhoPorts.AnimalRepositoryPort;
import br.com.sysagropec.rebanho.application.port.out.RebanhoPorts.LoteRepositoryPort;
import br.com.sysagropec.rebanho.application.port.out.RebanhoPorts.MovimentacaoRepositoryPort;
import br.com.sysagropec.rebanho.domain.model.AlteracaoAlocacao;
import br.com.sysagropec.rebanho.domain.model.Animal;
import br.com.sysagropec.rebanho.domain.model.Enums.Categoria;
import br.com.sysagropec.rebanho.domain.model.Enums.FaseLote;
import br.com.sysagropec.rebanho.domain.model.Enums.Finalidade;
import br.com.sysagropec.rebanho.domain.model.Enums.OrigemAnimal;
import br.com.sysagropec.rebanho.domain.model.Enums.Sexo;
import br.com.sysagropec.rebanho.domain.model.Enums.SituacaoAnimal;
import br.com.sysagropec.rebanho.domain.model.Enums.StatusLote;
import br.com.sysagropec.rebanho.domain.model.Enums.TipoIdentificacao;
import br.com.sysagropec.rebanho.domain.model.Identificacao;
import br.com.sysagropec.rebanho.domain.model.Lote;
import br.com.sysagropec.rebanho.domain.model.Movimentacao;
import br.com.sysagropec.rebanho.domain.model.Recebimento;
import br.com.sysagropec.rebanho.domain.model.Vinculo;
import br.com.sysagropec.shared.domain.ConflitoException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Mapeamento explícito entre o modelo de domínio e as entidades JPA.
 * Grava com flush imediato para que violações de unicidade surjam dentro do caso de uso.
 */
@Component
class RebanhoPersistenceAdapter implements AnimalRepositoryPort, LoteRepositoryPort, MovimentacaoRepositoryPort {

    private final SpringDataAnimais animais;
    private final SpringDataLotes lotes;
    private final SpringDataMovimentacoes movimentacoes;
    private final SpringDataRecebimentos recebimentos;
    private final JdbcClient jdbc;

    RebanhoPersistenceAdapter(SpringDataAnimais animais, SpringDataLotes lotes, SpringDataMovimentacoes movimentacoes,
                              SpringDataRecebimentos recebimentos, JdbcClient jdbc) {
        this.animais = animais;
        this.lotes = lotes;
        this.movimentacoes = movimentacoes;
        this.recebimentos = recebimentos;
        this.jdbc = jdbc;
    }

    // ---- Animal ----

    @Override
    public void salvar(Animal a) {
        AnimalJpaEntity e = animais.findById(a.id()).orElse(null);
        if (e == null) {
            e = new AnimalJpaEntity();
            e.id = a.id();
            e.organizacaoId = a.organizacaoId();
            e.numero = a.numero();
            e.recebimentoId = a.recebimentoId();
            e.criadoEm = a.criadoEm();
        } else if (!Objects.equals(e.versao, a.versao())) {
            throw new ConflitoException("O animal Nº " + a.numero()
                    + " foi alterado por outra operação. Atualize e tente novamente");
        }
        e.propriedadeId = a.propriedadeId();
        e.sexo = a.sexo().name();
        e.categoria = a.categoria().name();
        e.finalidade = a.finalidade().name();
        e.raca = a.raca();
        e.dataNascimento = a.dataNascimento();
        e.nascimentoEstimado = a.nascimentoEstimado();
        e.origem = a.origem().name();
        e.origemDescricao = a.origemDescricao();
        e.situacao = a.situacao().name();
        // Sempre altera a raiz: garante incremento de versão mesmo quando só os vínculos mudam.
        e.atualizadoEm = Instant.now();

        UUID org = a.organizacaoId();
        List<Runnable> inclusoes = new ArrayList<>();
        inclusoes.addAll(sincronizar(e.identificacoes, a.identificacoes(), x -> x.id, Identificacao::id, (x, i) -> x.fim = i.fim(),
                i -> {
                    IdentificacaoJpaEntity x = new IdentificacaoJpaEntity();
                    x.id = i.id();
                    x.organizacaoId = org;
                    x.tipo = i.tipo().name();
                    x.valor = i.valor();
                    x.inicio = i.inicio();
                    x.fim = i.fim();
                    return x;
                }));
        inclusoes.addAll(sincronizar(e.vinculosLote, a.vinculosLote(), x -> x.id, Vinculo::id, (x, v) -> x.fim = v.fim(),
                v -> {
                    VinculoLoteJpaEntity x = new VinculoLoteJpaEntity();
                    x.id = v.id();
                    x.organizacaoId = org;
                    x.loteId = v.alvoId();
                    x.movimentacaoId = v.movimentacaoId();
                    x.inicio = v.inicio();
                    x.fim = v.fim();
                    return x;
                }));
        inclusoes.addAll(sincronizar(e.localizacoes, a.localizacoes(), x -> x.id, Vinculo::id, (x, v) -> x.fim = v.fim(),
                v -> {
                    LocalizacaoJpaEntity x = new LocalizacaoJpaEntity();
                    x.id = v.id();
                    x.organizacaoId = org;
                    x.instalacaoId = v.alvoId();
                    x.movimentacaoId = v.movimentacaoId();
                    x.inicio = v.inicio();
                    x.fim = v.fim();
                    return x;
                }));
        // Duas etapas: o Hibernate executa inserções antes de atualizações no flush. Encerrar os vínculos
        // atuais primeiro evita violar os índices únicos parciais de "vínculo aberto" ao abrir os novos.
        animais.saveAndFlush(e);
        if (!inclusoes.isEmpty()) {
            inclusoes.forEach(Runnable::run);
            animais.saveAndFlush(e);
        }
    }

    /**
     * Atualiza os filhos existentes (apenas o fim muda) e devolve as inclusões dos novos, para serem aplicadas
     * depois das atualizações. O histórico nunca é removido.
     */
    private static <E, D> List<Runnable> sincronizar(List<E> entidades, List<D> dominio, Function<E, UUID> idEntidade,
                                           Function<D, UUID> idDominio, BiConsumer<E, D> atualizar,
                                           Function<D, E> criar) {
        List<Runnable> inclusoes = new ArrayList<>();
        for (D d : dominio) {
            E existente = entidades.stream().filter(x -> idEntidade.apply(x).equals(idDominio.apply(d))).findFirst()
                    .orElse(null);
            if (existente == null) {
                inclusoes.add(() -> entidades.add(criar.apply(d)));
            } else {
                atualizar.accept(existente, d);
            }
        }
        return inclusoes;
    }

    @Override
    public Optional<Animal> buscar(UUID organizacaoId, UUID animalId) {
        return animais.findByOrganizacaoIdAndId(organizacaoId, animalId).map(RebanhoPersistenceAdapter::paraDominio);
    }

    @Override
    public List<Animal> buscarVarios(UUID organizacaoId, Collection<UUID> animalIds) {
        if (animalIds.isEmpty()) {
            return List.of();
        }
        return animais.findByOrganizacaoIdAndIdIn(organizacaoId, animalIds).stream()
                .map(RebanhoPersistenceAdapter::paraDominio).toList();
    }

    @Override
    public List<Animal> listarPorRecebimento(UUID organizacaoId, UUID recebimentoId) {
        return animais.findByOrganizacaoIdAndRecebimentoId(organizacaoId, recebimentoId).stream()
                .map(RebanhoPersistenceAdapter::paraDominio).toList();
    }

    @Override
    public Set<String> identificacoesEmUso(UUID organizacaoId, TipoIdentificacao tipo, Collection<String> valores) {
        if (valores.isEmpty()) {
            return Set.of();
        }
        return new HashSet<>(animais.identificacoesEmUso(organizacaoId, tipo.name(), valores));
    }

    @Override
    public long contarNaInstalacao(UUID instalacaoId) {
        return animais.contarNaInstalacao(instalacaoId);
    }

    @Override
    public long contarNoLote(UUID loteId) {
        return animais.contarNoLote(loteId);
    }

    @Override
    public long reservarNumeros(UUID organizacaoId, int quantidade) {
        long ultimo = jdbc.sql("""
                        insert into sequencia_animal (organizacao_id, ultimo) values (:org, :n)
                        on conflict (organizacao_id) do update set ultimo = sequencia_animal.ultimo + :n
                        returning ultimo
                        """)
                .param("org", organizacaoId)
                .param("n", quantidade)
                .query(Long.class)
                .single();
        return ultimo - quantidade + 1;
    }

    private static Animal paraDominio(AnimalJpaEntity e) {
        return Animal.reconstituir(e.id, e.organizacaoId, e.propriedadeId, e.numero, Sexo.valueOf(e.sexo),
                Categoria.valueOf(e.categoria), Finalidade.valueOf(e.finalidade), e.raca, e.dataNascimento,
                e.nascimentoEstimado, OrigemAnimal.valueOf(e.origem), e.origemDescricao, e.recebimentoId,
                SituacaoAnimal.valueOf(e.situacao), e.criadoEm,
                e.identificacoes.stream().map(i -> Identificacao.reconstituir(i.id, TipoIdentificacao.valueOf(i.tipo),
                        i.valor, i.inicio, i.fim)).toList(),
                e.vinculosLote.stream().map(v -> Vinculo.reconstituir(v.id, v.loteId, v.movimentacaoId, v.inicio,
                        v.fim)).toList(),
                e.localizacoes.stream().map(v -> Vinculo.reconstituir(v.id, v.instalacaoId, v.movimentacaoId,
                        v.inicio, v.fim)).toList(),
                e.versao);
    }

    // ---- Lote ----

    @Override
    public void salvar(Lote l) {
        LoteJpaEntity e = lotes.findById(l.id()).orElse(null);
        if (e == null) {
            e = new LoteJpaEntity();
            e.id = l.id();
            e.organizacaoId = l.organizacaoId();
            e.propriedadeId = l.propriedadeId();
            e.criadoEm = l.criadoEm();
        } else if (!Objects.equals(e.versao, l.versao())) {
            throw new ConflitoException("O lote foi alterado por outra operação. Atualize e tente novamente");
        }
        e.codigo = l.codigo();
        e.fase = l.fase().name();
        e.finalidade = l.finalidade().name();
        e.status = l.status().name();
        e.inicio = l.inicio();
        e.fim = l.fim();
        e.observacao = l.observacao();
        e.atualizadoEm = Instant.now();
        lotes.saveAndFlush(e);
    }

    @Override
    public Optional<Lote> buscarLote(UUID organizacaoId, UUID loteId) {
        return lotes.findByOrganizacaoIdAndId(organizacaoId, loteId).map(RebanhoPersistenceAdapter::paraDominio);
    }

    @Override
    public Optional<Lote> buscarLoteComBloqueio(UUID organizacaoId, UUID loteId) {
        return lotes.buscarComBloqueio(organizacaoId, loteId).map(RebanhoPersistenceAdapter::paraDominio);
    }

    @Override
    public boolean existeCodigoAtivo(UUID propriedadeId, String codigo) {
        return lotes.existeCodigoAtivo(propriedadeId, codigo);
    }

    private static Lote paraDominio(LoteJpaEntity e) {
        return Lote.reconstituir(e.id, e.organizacaoId, e.propriedadeId, e.codigo, FaseLote.valueOf(e.fase),
                Finalidade.valueOf(e.finalidade), StatusLote.valueOf(e.status), e.inicio, e.fim, e.observacao,
                e.criadoEm, e.versao);
    }

    // ---- Movimentação e recebimento ----

    @Override
    public void salvar(Movimentacao m) {
        MovimentacaoJpaEntity e = new MovimentacaoJpaEntity();
        e.id = m.id();
        e.organizacaoId = m.organizacaoId();
        e.propriedadeId = m.propriedadeId();
        e.tipo = m.tipo().name();
        e.motivo = m.motivo();
        e.ocorridoEm = m.ocorridoEm();
        e.registradoEm = m.registradoEm();
        e.responsavelId = m.responsavelId();
        for (AlteracaoAlocacao a : m.itens()) {
            MovimentacaoJpaEntity.Item item = new MovimentacaoJpaEntity.Item();
            item.animalId = a.animalId();
            item.loteOrigemId = a.loteOrigemId();
            item.loteDestinoId = a.loteDestinoId();
            item.instalacaoOrigemId = a.instalacaoOrigemId();
            item.instalacaoDestinoId = a.instalacaoDestinoId();
            e.itens.add(item);
        }
        movimentacoes.saveAndFlush(e);
    }

    @Override
    public void salvar(Recebimento r) {
        RecebimentoJpaEntity e = new RecebimentoJpaEntity();
        e.id = r.id();
        e.organizacaoId = r.organizacaoId();
        e.propriedadeId = r.propriedadeId();
        e.instalacaoId = r.instalacaoId();
        e.loteId = r.loteId();
        e.movimentacaoId = r.movimentacaoId();
        e.ocorridoEm = r.ocorridoEm();
        e.registradoEm = r.registradoEm();
        e.origemDescricao = r.origemDescricao();
        e.documento = r.documento();
        e.observacao = r.observacao();
        e.quantidade = r.quantidade();
        e.responsavelId = r.responsavelId();
        recebimentos.saveAndFlush(e);
    }

    @Override
    public Optional<Recebimento> buscarRecebimento(UUID organizacaoId, UUID recebimentoId) {
        return recebimentos.findByOrganizacaoIdAndId(organizacaoId, recebimentoId)
                .map(e -> new Recebimento(e.id, e.organizacaoId, e.propriedadeId, e.instalacaoId, e.loteId,
                        e.movimentacaoId, e.ocorridoEm, e.registradoEm, e.origemDescricao, e.documento, e.observacao,
                        e.quantidade, e.responsavelId));
    }
}
