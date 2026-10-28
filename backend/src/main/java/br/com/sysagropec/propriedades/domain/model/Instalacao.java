package br.com.sysagropec.propriedades.domain.model;

import br.com.sysagropec.shared.domain.Verificar;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Localização física (curral, piquete, pasto). Não se confunde com lote, que é agrupamento de manejo.
 * Capacidade nula significa "não definida": a ocupação é exibida, mas não é limitada.
 */
public class Instalacao {

    private final UUID id;
    private final UUID organizacaoId;
    private final UUID propriedadeId;
    private final TipoInstalacao tipo;
    private final String codigo;
    private final String descricao;
    private final Integer capacidadeCabecas;
    private final boolean ativa;
    private final Instant criadoEm;

    private Instalacao(UUID id, UUID organizacaoId, UUID propriedadeId, TipoInstalacao tipo, String codigo,
                       String descricao, Integer capacidadeCabecas, boolean ativa, Instant criadoEm) {
        this.id = Objects.requireNonNull(id);
        this.organizacaoId = Objects.requireNonNull(organizacaoId);
        this.propriedadeId = Objects.requireNonNull(propriedadeId);
        this.tipo = tipo;
        this.codigo = codigo;
        this.descricao = descricao;
        this.capacidadeCabecas = capacidadeCabecas;
        this.ativa = ativa;
        this.criadoEm = criadoEm;
    }

    public static Instalacao nova(UUID id, UUID organizacaoId, UUID propriedadeId, TipoInstalacao tipo,
                                  String codigo, String descricao, Integer capacidadeCabecas, Instant agora) {
        Verificar.obrigatorio(tipo, "Tipo");
        Verificar.regra(capacidadeCabecas == null || capacidadeCabecas > 0, "Capacidade deve ser maior que zero");
        Verificar.regra(capacidadeCabecas == null || capacidadeCabecas <= 100_000, "Capacidade acima do limite aceito");
        return new Instalacao(id, organizacaoId, propriedadeId, tipo,
                Verificar.texto(codigo, "Código", 30),
                Verificar.textoOpcional(descricao, "Descrição", 200),
                capacidadeCabecas, true, agora);
    }

    public static Instalacao reconstituir(UUID id, UUID organizacaoId, UUID propriedadeId, TipoInstalacao tipo,
                                          String codigo, String descricao, Integer capacidadeCabecas, boolean ativa,
                                          Instant criadoEm) {
        return new Instalacao(id, organizacaoId, propriedadeId, tipo, codigo, descricao, capacidadeCabecas, ativa,
                criadoEm);
    }

    public UUID id() { return id; }
    public UUID organizacaoId() { return organizacaoId; }
    public UUID propriedadeId() { return propriedadeId; }
    public TipoInstalacao tipo() { return tipo; }
    public String codigo() { return codigo; }
    public String descricao() { return descricao; }
    public Integer capacidadeCabecas() { return capacidadeCabecas; }
    public boolean ativa() { return ativa; }
    public Instant criadoEm() { return criadoEm; }
}
