package br.com.sysagropec.rebanho.domain.model;

import br.com.sysagropec.rebanho.domain.model.Enums.Categoria;
import br.com.sysagropec.rebanho.domain.model.Enums.Finalidade;
import br.com.sysagropec.rebanho.domain.model.Enums.OrigemAnimal;
import br.com.sysagropec.rebanho.domain.model.Enums.Sexo;
import br.com.sysagropec.rebanho.domain.model.Enums.SituacaoAnimal;
import br.com.sysagropec.rebanho.domain.model.Enums.TipoIdentificacao;
import br.com.sysagropec.shared.domain.ConflitoException;
import br.com.sysagropec.shared.domain.RegraNegocioException;
import br.com.sysagropec.shared.domain.Verificar;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * Agregado do animal. Mantém identificações e o histórico de vínculos com lote (manejo)
 * e instalação (localização física). Invariantes:
 * <ul>
 *     <li>no máximo um vínculo de lote e um de instalação abertos;</li>
 *     <li>animal ativo sempre tem localização;</li>
 *     <li>movimentações não podem ser anteriores à última alocação registrada (sem reescrever o histórico);</li>
 *     <li>categoria compatível com o sexo; nascimento não posterior à entrada.</li>
 * </ul>
 */
public class Animal {

    private final UUID id;
    private final UUID organizacaoId;
    private final UUID propriedadeId;
    private final long numero;
    private final Sexo sexo;
    private final Categoria categoria;
    private final Finalidade finalidade;
    private final String raca;
    private final LocalDate dataNascimento;
    private final boolean nascimentoEstimado;
    private final OrigemAnimal origem;
    private final String origemDescricao;
    private final UUID recebimentoId;
    private final SituacaoAnimal situacao;
    private final Instant criadoEm;
    private final List<Identificacao> identificacoes;
    private final List<Vinculo> vinculosLote;
    private final List<Vinculo> localizacoes;
    private final Long versao;

    private Animal(UUID id, UUID organizacaoId, UUID propriedadeId, long numero, Sexo sexo, Categoria categoria,
                   Finalidade finalidade, String raca, LocalDate dataNascimento, boolean nascimentoEstimado,
                   OrigemAnimal origem, String origemDescricao, UUID recebimentoId, SituacaoAnimal situacao,
                   Instant criadoEm, List<Identificacao> identificacoes, List<Vinculo> vinculosLote,
                   List<Vinculo> localizacoes, Long versao) {
        this.id = Objects.requireNonNull(id);
        this.organizacaoId = Objects.requireNonNull(organizacaoId);
        this.propriedadeId = Objects.requireNonNull(propriedadeId);
        this.numero = numero;
        this.sexo = sexo;
        this.categoria = categoria;
        this.finalidade = finalidade;
        this.raca = raca;
        this.dataNascimento = dataNascimento;
        this.nascimentoEstimado = nascimentoEstimado;
        this.origem = origem;
        this.origemDescricao = origemDescricao;
        this.recebimentoId = recebimentoId;
        this.situacao = situacao;
        this.criadoEm = criadoEm;
        this.identificacoes = new ArrayList<>(identificacoes);
        this.vinculosLote = new ArrayList<>(vinculosLote);
        this.localizacoes = new ArrayList<>(localizacoes);
        this.versao = versao;
    }

    public record DadosEntrada(String brinco, String identificadorEletronico, Sexo sexo, Categoria categoria,
                               Finalidade finalidade, String raca, LocalDate dataNascimento,
                               boolean nascimentoEstimado, OrigemAnimal origem, String origemDescricao) {
    }

    /** Destino de uma movimentação: novo lote, saída do lote e/ou nova instalação. Nulo mantém o atual. */
    public record Destino(UUID loteId, boolean retirarDoLote, UUID instalacaoId) {
        public Destino {
            Verificar.regra(!(loteId != null && retirarDoLote), "Informe um lote de destino ou a retirada do lote, não ambos");
            Verificar.regra(loteId != null || retirarDoLote || instalacaoId != null,
                    "Informe ao menos um destino (lote, retirada do lote ou instalação)");
        }
    }

    /**
     * Entrada de um animal na propriedade, já alocado em uma instalação e opcionalmente em um lote.
     *
     * @param dataEntrada data local (fuso da propriedade) correspondente a {@code ocorridoEm}
     */
    public static Animal registrarEntrada(UUID id, UUID organizacaoId, UUID propriedadeId, long numero,
                                          DadosEntrada dados, UUID recebimentoId, UUID movimentacaoId,
                                          UUID instalacaoId, UUID loteId, Instant ocorridoEm, LocalDate dataEntrada,
                                          Instant agora) {
        Verificar.obrigatorio(dados.sexo(), "Sexo");
        Verificar.obrigatorio(dados.categoria(), "Categoria");
        Verificar.obrigatorio(dados.finalidade(), "Finalidade");
        Verificar.obrigatorio(dados.origem(), "Origem");
        Verificar.obrigatorio(instalacaoId, "Instalação");
        String brinco = Verificar.texto(dados.brinco(), "Brinco", 40);
        if (!dados.categoria().compativelCom(dados.sexo())) {
            throw new RegraNegocioException("Brinco " + brinco + ": categoria " + dados.categoria()
                    + " incompatível com o sexo " + dados.sexo());
        }
        if (dados.dataNascimento() != null && dados.dataNascimento().isAfter(dataEntrada)) {
            throw new RegraNegocioException("Brinco " + brinco + ": data de nascimento posterior à entrada");
        }
        Verificar.regra(dados.dataNascimento() != null || !dados.nascimentoEstimado(),
                "Brinco " + brinco + ": informe a data de nascimento estimada ou desmarque a estimativa");

        List<Identificacao> identificacoes = new ArrayList<>();
        identificacoes.add(Identificacao.nova(TipoIdentificacao.BRINCO, brinco, ocorridoEm));
        String eletronico = Verificar.textoOpcional(dados.identificadorEletronico(), "Identificador eletrônico", 40);
        if (eletronico != null) {
            identificacoes.add(Identificacao.nova(TipoIdentificacao.ELETRONICO, eletronico, ocorridoEm));
        }
        List<Vinculo> lotes = loteId == null ? List.of() : List.of(Vinculo.abrir(loteId, movimentacaoId, ocorridoEm));
        List<Vinculo> locais = List.of(Vinculo.abrir(instalacaoId, movimentacaoId, ocorridoEm));

        return new Animal(id, organizacaoId, propriedadeId, numero, dados.sexo(), dados.categoria(),
                dados.finalidade(), Verificar.textoOpcional(dados.raca(), "Raça", 60), dados.dataNascimento(),
                dados.nascimentoEstimado(), dados.origem(),
                Verificar.textoOpcional(dados.origemDescricao(), "Descrição da origem", 200), recebimentoId,
                SituacaoAnimal.ATIVO, agora, identificacoes, lotes, locais, null);
    }

    public static Animal reconstituir(UUID id, UUID organizacaoId, UUID propriedadeId, long numero, Sexo sexo,
                                      Categoria categoria, Finalidade finalidade, String raca,
                                      LocalDate dataNascimento, boolean nascimentoEstimado, OrigemAnimal origem,
                                      String origemDescricao, UUID recebimentoId, SituacaoAnimal situacao,
                                      Instant criadoEm, List<Identificacao> identificacoes,
                                      List<Vinculo> vinculosLote, List<Vinculo> localizacoes, Long versao) {
        return new Animal(id, organizacaoId, propriedadeId, numero, sexo, categoria, finalidade, raca, dataNascimento,
                nascimentoEstimado, origem, origemDescricao, recebimentoId, situacao, criadoEm, identificacoes,
                vinculosLote, localizacoes, versao);
    }

    /**
     * Aplica a movimentação, encerrando os vínculos alterados e abrindo os novos no mesmo instante.
     *
     * @throws ConflitoException se {@code quando} for anterior à última alocação registrada
     */
    public AlteracaoAlocacao realocar(Destino destino, UUID movimentacaoId, Instant quando) {
        Verificar.regra(situacao == SituacaoAnimal.ATIVO, "Animal Nº " + numero + " não está ativo");
        Instant ultima = inicioUltimaAlocacao();
        if (quando.isBefore(ultima)) {
            throw new ConflitoException("Animal Nº " + numero + " tem alocação registrada em " + ultima
                    + ", posterior à data informada. Lançamentos retroativos não são aceitos");
        }
        UUID loteAntes = loteAtual().orElse(null);
        UUID instalacaoAntes = instalacaoAtual().orElseThrow(
                () -> new IllegalStateException("Animal ativo sem localização: " + id));
        UUID loteDepois = destino.retirarDoLote() ? null : (destino.loteId() != null ? destino.loteId() : loteAntes);
        UUID instalacaoDepois = destino.instalacaoId() != null ? destino.instalacaoId() : instalacaoAntes;

        AlteracaoAlocacao alteracao = new AlteracaoAlocacao(id, loteAntes, loteDepois, instalacaoAntes,
                instalacaoDepois);
        if (!alteracao.mudouLote() && !alteracao.mudouInstalacao()) {
            throw new RegraNegocioException("Animal Nº " + numero + " já está no destino informado");
        }
        if (alteracao.mudouLote()) {
            abertoEm(vinculosLote).ifPresent(v -> v.encerrar(quando));
            if (loteDepois != null) {
                vinculosLote.add(Vinculo.abrir(loteDepois, movimentacaoId, quando));
            }
        }
        if (alteracao.mudouInstalacao()) {
            abertoEm(localizacoes).ifPresent(v -> v.encerrar(quando));
            localizacoes.add(Vinculo.abrir(instalacaoDepois, movimentacaoId, quando));
        }
        return alteracao;
    }

    public Optional<UUID> loteAtual() {
        return abertoEm(vinculosLote).map(Vinculo::alvoId);
    }

    public Optional<UUID> instalacaoAtual() {
        return abertoEm(localizacoes).map(Vinculo::alvoId);
    }

    public Optional<String> identificacaoAtiva(TipoIdentificacao tipo) {
        return identificacoes.stream().filter(i -> i.tipo() == tipo && i.ativa()).map(Identificacao::valor).findFirst();
    }

    public boolean ativo() {
        return situacao == SituacaoAnimal.ATIVO;
    }

    /** Início mais recente entre os vínculos (abertos ou encerrados) e encerramentos registrados. */
    public Instant inicioUltimaAlocacao() {
        return Stream.concat(vinculosLote.stream(), localizacoes.stream())
                .flatMap(v -> Stream.of(v.inicio(), v.fim()))
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(criadoEm);
    }

    private static Optional<Vinculo> abertoEm(List<Vinculo> vinculos) {
        return vinculos.stream().filter(Vinculo::aberto).findFirst();
    }

    public UUID id() { return id; }
    public UUID organizacaoId() { return organizacaoId; }
    public UUID propriedadeId() { return propriedadeId; }
    public long numero() { return numero; }
    public Sexo sexo() { return sexo; }
    public Categoria categoria() { return categoria; }
    public Finalidade finalidade() { return finalidade; }
    public String raca() { return raca; }
    public LocalDate dataNascimento() { return dataNascimento; }
    public boolean nascimentoEstimado() { return nascimentoEstimado; }
    public OrigemAnimal origem() { return origem; }
    public String origemDescricao() { return origemDescricao; }
    public UUID recebimentoId() { return recebimentoId; }
    public SituacaoAnimal situacao() { return situacao; }
    public Instant criadoEm() { return criadoEm; }
    public List<Identificacao> identificacoes() { return Collections.unmodifiableList(identificacoes); }
    public List<Vinculo> vinculosLote() { return Collections.unmodifiableList(vinculosLote); }
    public List<Vinculo> localizacoes() { return Collections.unmodifiableList(localizacoes); }
    public Long versao() { return versao; }
}
