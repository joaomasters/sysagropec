package br.com.sysagropec.propriedades.domain.model;

import br.com.sysagropec.shared.domain.RegraNegocioException;
import br.com.sysagropec.shared.domain.Verificar;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public class Propriedade {

    private static final Set<String> UFS = Set.of("AC", "AL", "AP", "AM", "BA", "CE", "DF", "ES", "GO", "MA", "MT",
            "MS", "MG", "PA", "PB", "PR", "PE", "PI", "RJ", "RN", "RS", "RO", "RR", "SC", "SP", "SE", "TO");

    private final UUID id;
    private final UUID organizacaoId;
    private final String nome;
    private final String municipio;
    private final String uf;
    private final ZoneId fusoHorario;
    private final boolean ativa;
    private final Instant criadoEm;

    private Propriedade(UUID id, UUID organizacaoId, String nome, String municipio, String uf, ZoneId fusoHorario,
                        boolean ativa, Instant criadoEm) {
        this.id = Objects.requireNonNull(id);
        this.organizacaoId = Objects.requireNonNull(organizacaoId);
        this.nome = nome;
        this.municipio = municipio;
        this.uf = uf;
        this.fusoHorario = fusoHorario;
        this.ativa = ativa;
        this.criadoEm = criadoEm;
    }

    public static Propriedade nova(UUID id, UUID organizacaoId, String nome, String municipio, String uf,
                                   String fusoHorario, Instant agora) {
        String ufNormalizada = Verificar.textoOpcional(uf, "UF", 2);
        if (ufNormalizada != null) {
            ufNormalizada = ufNormalizada.toUpperCase(Locale.ROOT);
            Verificar.regra(UFS.contains(ufNormalizada), "UF inválida");
        }
        return new Propriedade(id, organizacaoId,
                Verificar.texto(nome, "Nome", 120),
                Verificar.textoOpcional(municipio, "Município", 120),
                ufNormalizada,
                fuso(fusoHorario),
                true,
                agora);
    }

    public static Propriedade reconstituir(UUID id, UUID organizacaoId, String nome, String municipio, String uf,
                                           String fusoHorario, boolean ativa, Instant criadoEm) {
        return new Propriedade(id, organizacaoId, nome, municipio, uf, ZoneId.of(fusoHorario), ativa, criadoEm);
    }

    private static ZoneId fuso(String fusoHorario) {
        String valor = fusoHorario == null || fusoHorario.isBlank() ? "America/Sao_Paulo" : fusoHorario.strip();
        try {
            return ZoneId.of(valor);
        } catch (DateTimeException e) {
            throw new RegraNegocioException("Fuso horário inválido: " + valor);
        }
    }

    public UUID id() { return id; }
    public UUID organizacaoId() { return organizacaoId; }
    public String nome() { return nome; }
    public String municipio() { return municipio; }
    public String uf() { return uf; }
    public ZoneId fusoHorario() { return fusoHorario; }
    public boolean ativa() { return ativa; }
    public Instant criadoEm() { return criadoEm; }
}
