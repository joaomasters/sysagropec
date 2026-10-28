package br.com.sysagropec.rebanho.domain.model;

import br.com.sysagropec.rebanho.domain.model.Enums.FaseLote;
import br.com.sysagropec.rebanho.domain.model.Enums.Finalidade;
import br.com.sysagropec.rebanho.domain.model.Enums.StatusLote;
import br.com.sysagropec.shared.domain.Verificar;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Agrupamento de manejo. Não equivale a um curral: a localização física é registrada em cada animal. */
public class Lote {

    private final UUID id;
    private final UUID organizacaoId;
    private final UUID propriedadeId;
    private final String codigo;
    private final FaseLote fase;
    private final Finalidade finalidade;
    private StatusLote status;
    private final Instant inicio;
    private Instant fim;
    private final String observacao;
    private final Instant criadoEm;
    private final Long versao;

    private Lote(UUID id, UUID organizacaoId, UUID propriedadeId, String codigo, FaseLote fase, Finalidade finalidade,
                 StatusLote status, Instant inicio, Instant fim, String observacao, Instant criadoEm, Long versao) {
        this.id = Objects.requireNonNull(id);
        this.organizacaoId = Objects.requireNonNull(organizacaoId);
        this.propriedadeId = Objects.requireNonNull(propriedadeId);
        this.codigo = codigo;
        this.fase = fase;
        this.finalidade = finalidade;
        this.status = status;
        this.inicio = inicio;
        this.fim = fim;
        this.observacao = observacao;
        this.criadoEm = criadoEm;
        this.versao = versao;
    }

    public static Lote abrir(UUID id, UUID organizacaoId, UUID propriedadeId, String codigo, FaseLote fase,
                             Finalidade finalidade, String observacao, Instant inicio, Instant agora) {
        return new Lote(id, organizacaoId, propriedadeId,
                Verificar.texto(codigo, "Código do lote", 40),
                Verificar.obrigatorio(fase, "Fase"),
                Verificar.obrigatorio(finalidade, "Finalidade"),
                StatusLote.ATIVO,
                Verificar.obrigatorio(inicio, "Início"),
                null,
                Verificar.textoOpcional(observacao, "Observação", 500),
                agora, null);
    }

    public static Lote reconstituir(UUID id, UUID organizacaoId, UUID propriedadeId, String codigo, FaseLote fase,
                                    Finalidade finalidade, StatusLote status, Instant inicio, Instant fim,
                                    String observacao, Instant criadoEm, Long versao) {
        return new Lote(id, organizacaoId, propriedadeId, codigo, fase, finalidade, status, inicio, fim, observacao,
                criadoEm, versao);
    }

    public void exigirAptoAReceber(Instant quando) {
        Verificar.regra(status == StatusLote.ATIVO, "Lote " + codigo + " está encerrado");
        Verificar.regra(!quando.isBefore(inicio), "A data informada é anterior ao início do lote " + codigo);
    }

    public void encerrar(Instant quando, long animaisVinculados) {
        Verificar.regra(status == StatusLote.ATIVO, "Lote " + codigo + " já está encerrado");
        Verificar.regra(animaisVinculados == 0,
                "Retire os " + animaisVinculados + " animais do lote antes de encerrá-lo");
        Verificar.regra(!quando.isBefore(inicio), "O encerramento não pode ser anterior ao início do lote");
        this.status = StatusLote.ENCERRADO;
        this.fim = quando;
    }

    public UUID id() { return id; }
    public UUID organizacaoId() { return organizacaoId; }
    public UUID propriedadeId() { return propriedadeId; }
    public String codigo() { return codigo; }
    public FaseLote fase() { return fase; }
    public Finalidade finalidade() { return finalidade; }
    public StatusLote status() { return status; }
    public Instant inicio() { return inicio; }
    public Instant fim() { return fim; }
    public String observacao() { return observacao; }
    public Instant criadoEm() { return criadoEm; }
    public Long versao() { return versao; }
}
