package br.com.sysagropec.consultas.adapter.out.persistence;

import br.com.sysagropec.consultas.application.port.in.ConsultasQuery.Dashboard;
import br.com.sysagropec.consultas.application.port.in.ConsultasQuery.Evento;
import br.com.sysagropec.consultas.application.port.in.ConsultasQuery.LoteAtivo;
import br.com.sysagropec.consultas.application.port.in.ConsultasQuery.Ocupacao;
import br.com.sysagropec.consultas.application.port.in.ConsultasQuery.PesagemRecente;
import br.com.sysagropec.consultas.application.port.in.ConsultasQuery.ResumoPesagens;
import br.com.sysagropec.consultas.application.port.in.ConsultasQuery.ResumoTarefas;
import br.com.sysagropec.consultas.application.port.out.LeituraConsolidadaPort;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Modelo de leitura consolidado. Lê tabelas de vários módulos por SQL, sem depender de suas classes;
 * toda consulta é filtrada pela organização.
 */
@Component
class LeituraConsolidadaJdbcAdapter implements LeituraConsolidadaPort {

    private final JdbcClient jdbc;

    LeituraConsolidadaJdbcAdapter(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<UUID> propriedadeDoAnimal(UUID organizacaoId, UUID animalId) {
        return jdbc.sql("select propriedade_id from animal where organizacao_id = :org and id = :id")
                .param("org", organizacaoId).param("id", animalId)
                .query(UUID.class).optional();
    }

    @Override
    public Optional<ZoneId> fusoHorario(UUID organizacaoId, UUID propriedadeId) {
        return jdbc.sql("select fuso_horario from propriedade where organizacao_id = :org and id = :id")
                .param("org", organizacaoId).param("id", propriedadeId)
                .query(String.class).optional().map(ZoneId::of);
    }

    @Override
    public List<Evento> eventosDoAnimal(UUID organizacaoId, UUID animalId) {
        List<Evento> eventos = new ArrayList<>();
        eventos.addAll(jdbc.sql("""
                        select m.id, m.tipo, m.motivo, m.ocorrido_em, m.registrado_em, u.nome as responsavel,
                               lo.codigo as lote_origem, ld.codigo as lote_destino,
                               io.codigo as inst_origem, idst.codigo as inst_destino
                        from movimentacao_item mi
                        join movimentacao m on m.id = mi.movimentacao_id
                        join usuario u on u.id = m.responsavel_id
                        left join lote lo on lo.id = mi.lote_origem_id
                        left join lote ld on ld.id = mi.lote_destino_id
                        left join instalacao io on io.id = mi.instalacao_origem_id
                        left join instalacao idst on idst.id = mi.instalacao_destino_id
                        where m.organizacao_id = :org and mi.animal_id = :animal
                        """)
                .param("org", organizacaoId).param("animal", animalId)
                .query((rs, n) -> new Evento(rs.getString("tipo"), instante(rs, "ocorrido_em"),
                        instante(rs, "registrado_em"), tituloMovimentacao(rs.getString("tipo")),
                        descricaoMovimentacao(rs), rs.getString("responsavel"), uuid(rs, "id")))
                .list());
        eventos.addAll(jdbc.sql("""
                        select p.id, p.peso_kg, p.natureza, p.metodo, p.finalidade, p.ocorrido_em, p.registrado_em,
                               p.cancelada_em, p.motivo_cancelamento, u.nome as responsavel
                        from pesagem p join usuario u on u.id = p.responsavel_id
                        where p.organizacao_id = :org and p.animal_id = :animal
                        """)
                .param("org", organizacaoId).param("animal", animalId)
                .query((rs, n) -> {
                    boolean cancelada = rs.getTimestamp("cancelada_em") != null;
                    String descricao = rs.getBigDecimal("peso_kg").toPlainString() + " kg ("
                            + rs.getString("natureza").toLowerCase() + ", " + rs.getString("metodo") + ")"
                            + (cancelada ? " — cancelada: " + rs.getString("motivo_cancelamento") : "");
                    return new Evento(cancelada ? "PESAGEM_CANCELADA" : "PESAGEM", instante(rs, "ocorrido_em"),
                            instante(rs, "registrado_em"), "Pesagem de " + rs.getString("finalidade").toLowerCase(),
                            descricao, rs.getString("responsavel"), uuid(rs, "id"));
                })
                .list());
        eventos.addAll(jdbc.sql("""
                        select t.id, t.tipo, t.titulo, t.status, t.prazo, t.finalizada_em, t.criada_em,
                               t.justificativa, t.observacao_conclusao, u.nome as responsavel
                        from tarefa t left join usuario u on u.id = t.finalizada_por
                        where t.organizacao_id = :org and t.alvo_tipo = 'ANIMAL' and t.alvo_id = :animal
                          and t.finalizada_em is not null
                        """)
                .param("org", organizacaoId).param("animal", animalId)
                .query((rs, n) -> new Evento("TAREFA_" + rs.getString("status"), instante(rs, "finalizada_em"),
                        instante(rs, "finalizada_em"), "Tarefa: " + rs.getString("titulo"),
                        descricaoTarefa(rs), rs.getString("responsavel"), uuid(rs, "id")))
                .list());
        return eventos;
    }

    @Override
    public Dashboard dashboard(UUID org, UUID prop, LocalDate data, ZoneId zona, Instant agora) {
        Instant inicioDia = data.atStartOfDay(zona).toInstant();
        Instant fimDia = data.plusDays(1).atStartOfDay(zona).toInstant();

        Map<String, Long> porCategoria = new LinkedHashMap<>();
        jdbc.sql("""
                        select categoria, count(*) as total from animal
                        where organizacao_id = :org and propriedade_id = :prop and situacao = 'ATIVO'
                        group by categoria order by categoria
                        """)
                .param("org", org).param("prop", prop)
                .query((rs, n) -> porCategoria.put(rs.getString("categoria"), rs.getLong("total")))
                .list();
        long ativos = porCategoria.values().stream().mapToLong(Long::longValue).sum();

        long semLote = jdbc.sql("""
                        select count(*) from animal a
                        where a.organizacao_id = :org and a.propriedade_id = :prop and a.situacao = 'ATIVO'
                          and not exists (select 1 from animal_lote_vinculo v where v.animal_id = a.id and v.fim is null)
                        """)
                .param("org", org).param("prop", prop).query(Long.class).single();

        List<Ocupacao> ocupacao = jdbc.sql("""
                        select i.id, i.codigo, i.tipo, i.capacidade_cabecas,
                               (select count(*) from animal_localizacao l where l.instalacao_id = i.id and l.fim is null)
                                   as ocupacao
                        from instalacao i
                        where i.organizacao_id = :org and i.propriedade_id = :prop and i.ativa
                        order by i.codigo
                        """)
                .param("org", org).param("prop", prop)
                .query((rs, n) -> new Ocupacao(uuid(rs, "id"), rs.getString("codigo"), rs.getString("tipo"),
                        (Integer) rs.getObject("capacidade_cabecas"), rs.getLong("ocupacao")))
                .list();

        List<LoteAtivo> lotes = jdbc.sql("""
                        select l.id, l.codigo, l.fase,
                               (select count(*) from animal_lote_vinculo v where v.lote_id = l.id and v.fim is null)
                                   as animais
                        from lote l
                        where l.organizacao_id = :org and l.propriedade_id = :prop and l.status = 'ATIVO'
                        order by l.codigo
                        """)
                .param("org", org).param("prop", prop)
                .query((rs, n) -> new LoteAtivo(uuid(rs, "id"), rs.getString("codigo"), rs.getString("fase"),
                        rs.getLong("animais")))
                .list();

        ResumoTarefas tarefas = jdbc.sql("""
                        select
                          count(*) filter (where status in ('PENDENTE','EM_ANDAMENTO') and prazo < :fimDia) as abertas,
                          count(*) filter (where status in ('PENDENTE','EM_ANDAMENTO') and prazo < :agora) as atrasadas,
                          count(*) filter (where finalizada_em >= :inicioDia) as finalizadas
                        from tarefa where organizacao_id = :org and propriedade_id = :prop
                        """)
                .param("org", org).param("prop", prop)
                .param("fimDia", Timestamp.from(fimDia)).param("agora", Timestamp.from(agora))
                .param("inicioDia", Timestamp.from(inicioDia))
                .query((rs, n) -> new ResumoTarefas(rs.getLong("abertas"), rs.getLong("atrasadas"),
                        rs.getLong("finalizadas")))
                .single();

        Instant desde = agora.minus(Duration.ofDays(30));
        long[] contagem = jdbc.sql("""
                        select count(*) as total,
                               count(*) filter (where natureza = 'MEDIDO') as medidas,
                               count(*) filter (where natureza = 'ESTIMADO') as estimadas
                        from pesagem
                        where organizacao_id = :org and propriedade_id = :prop and cancelada_em is null
                          and ocorrido_em >= :desde
                        """)
                .param("org", org).param("prop", prop).param("desde", Timestamp.from(desde))
                .query((rs, n) -> new long[]{rs.getLong("total"), rs.getLong("medidas"), rs.getLong("estimadas")})
                .single();
        List<PesagemRecente> recentes = jdbc.sql("""
                        select p.id, p.animal_id, a.numero, b.valor as brinco, p.peso_kg, p.natureza, p.ocorrido_em
                        from pesagem p
                        join animal a on a.id = p.animal_id
                        left join animal_identificacao b on b.animal_id = a.id and b.tipo = 'BRINCO' and b.fim is null
                        where p.organizacao_id = :org and p.propriedade_id = :prop and p.cancelada_em is null
                        order by p.ocorrido_em desc limit 5
                        """)
                .param("org", org).param("prop", prop)
                .query((rs, n) -> new PesagemRecente(uuid(rs, "id"), uuid(rs, "animal_id"), rs.getLong("numero"),
                        rs.getString("brinco"), rs.getBigDecimal("peso_kg"), rs.getString("natureza"),
                        instante(rs, "ocorrido_em")))
                .list();

        return new Dashboard(data, zona.getId(), ativos, semLote, porCategoria, ocupacao, lotes, tarefas,
                new ResumoPesagens(contagem[0], contagem[1], contagem[2], recentes));
    }

    private static String tituloMovimentacao(String tipo) {
        return switch (tipo) {
            case "RECEBIMENTO" -> "Entrada na propriedade";
            case "FORMACAO_LOTE" -> "Inclusão em lote";
            default -> "Movimentação";
        };
    }

    private static String descricaoMovimentacao(ResultSet rs) throws SQLException {
        List<String> partes = new ArrayList<>();
        String loteOrigem = rs.getString("lote_origem");
        String loteDestino = rs.getString("lote_destino");
        if (loteOrigem != null || loteDestino != null) {
            if (loteDestino == null) {
                partes.add("Saiu do lote " + loteOrigem);
            } else if (loteOrigem == null) {
                partes.add("Lote " + loteDestino);
            } else if (!loteOrigem.equals(loteDestino)) {
                partes.add("Lote " + loteOrigem + " → " + loteDestino);
            }
        }
        String instOrigem = rs.getString("inst_origem");
        String instDestino = rs.getString("inst_destino");
        if (instDestino != null && !instDestino.equals(instOrigem)) {
            partes.add(instOrigem == null ? "Instalação " + instDestino
                    : "Instalação " + instOrigem + " → " + instDestino);
        }
        partes.add("Motivo: " + rs.getString("motivo"));
        return String.join(". ", partes);
    }

    private static String descricaoTarefa(ResultSet rs) throws SQLException {
        String status = rs.getString("status");
        String texto = switch (status) {
            case "CONCLUIDA" -> rs.getString("observacao_conclusao");
            default -> rs.getString("justificativa");
        };
        return status.charAt(0) + status.substring(1).toLowerCase().replace('_', ' ')
                + (texto == null || texto.isBlank() ? "" : ": " + texto);
    }

    private static UUID uuid(ResultSet rs, String coluna) throws SQLException {
        return rs.getObject(coluna, UUID.class);
    }

    private static Instant instante(ResultSet rs, String coluna) throws SQLException {
        Timestamp t = rs.getTimestamp(coluna);
        return t == null ? null : t.toInstant();
    }
}
