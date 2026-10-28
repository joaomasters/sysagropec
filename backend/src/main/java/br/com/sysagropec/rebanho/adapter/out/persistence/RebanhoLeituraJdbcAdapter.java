package br.com.sysagropec.rebanho.adapter.out.persistence;

import br.com.sysagropec.rebanho.application.port.in.RebanhoConsultas.AnimalDetalhe;
import br.com.sysagropec.rebanho.application.port.in.RebanhoConsultas.AnimalResumo;
import br.com.sysagropec.rebanho.application.port.in.RebanhoConsultas.FiltroAnimais;
import br.com.sysagropec.rebanho.application.port.in.RebanhoConsultas.IdentificacaoHistorico;
import br.com.sysagropec.rebanho.application.port.in.RebanhoConsultas.LoteResumo;
import br.com.sysagropec.rebanho.application.port.in.RebanhoConsultas.PeriodoLocalizacao;
import br.com.sysagropec.rebanho.application.port.in.RebanhoConsultas.PeriodoLote;
import br.com.sysagropec.rebanho.application.port.out.RebanhoPorts.RebanhoLeituraPort;
import br.com.sysagropec.shared.application.Pagina;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Modelo de leitura do rebanho em SQL, restrito às tabelas do próprio módulo. */
@Component
class RebanhoLeituraJdbcAdapter implements RebanhoLeituraPort {

    private static final String SELECAO_ANIMAL = """
            select a.id, a.propriedade_id, a.numero, a.sexo, a.categoria, a.finalidade, a.raca, a.situacao,
                   a.data_nascimento, a.nascimento_estimado, a.origem, a.origem_descricao, a.recebimento_id,
                   b.valor as brinco, el.valor as eletronico, vl.lote_id, l.codigo as lote_codigo, lc.instalacao_id,
                   (select min(x.inicio) from animal_localizacao x where x.animal_id = a.id) as entrada_em
            from animal a
            left join animal_identificacao b on b.animal_id = a.id and b.tipo = 'BRINCO' and b.fim is null
            left join animal_identificacao el on el.animal_id = a.id and el.tipo = 'ELETRONICO' and el.fim is null
            left join animal_lote_vinculo vl on vl.animal_id = a.id and vl.fim is null
            left join lote l on l.id = vl.lote_id
            left join animal_localizacao lc on lc.animal_id = a.id and lc.fim is null
            """;

    private final JdbcClient jdbc;

    RebanhoLeituraJdbcAdapter(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Pagina<AnimalResumo> listarAnimais(UUID organizacaoId, FiltroAnimais f, int pagina, int tamanho) {
        StringBuilder where = new StringBuilder(" where a.organizacao_id = :org and a.propriedade_id = :prop");
        Map<String, Object> params = new HashMap<>();
        params.put("org", organizacaoId);
        params.put("prop", f.propriedadeId());
        if (f.busca() != null && !f.busca().isBlank()) {
            String busca = f.busca().strip();
            where.append(" and (lower(b.valor) like :busca or lower(el.valor) like :busca or cast(a.numero as text) = :exata)");
            params.put("busca", "%" + escaparLike(busca.toLowerCase(Locale.ROOT)) + "%");
            params.put("exata", busca);
        }
        if (f.loteId() != null) {
            where.append(" and vl.lote_id = :lote");
            params.put("lote", f.loteId());
        }
        if (Boolean.TRUE.equals(f.semLote())) {
            where.append(" and vl.lote_id is null");
        }
        if (f.instalacaoId() != null) {
            where.append(" and lc.instalacao_id = :instalacao");
            params.put("instalacao", f.instalacaoId());
        }
        if (f.categoria() != null) {
            where.append(" and a.categoria = :categoria");
            params.put("categoria", f.categoria());
        }
        if (f.situacao() != null) {
            where.append(" and a.situacao = :situacao");
            params.put("situacao", f.situacao());
        }
        long total = jdbc.sql("select count(*) from (" + SELECAO_ANIMAL + where + ") t").params(params)
                .query(Long.class).single();
        params.put("limite", tamanho);
        params.put("deslocamento", (long) pagina * tamanho);
        List<AnimalResumo> itens = jdbc.sql(SELECAO_ANIMAL + where
                        + " order by a.numero desc limit :limite offset :deslocamento")
                .params(params)
                .query((rs, n) -> resumo(rs))
                .list();
        return new Pagina<>(itens, pagina, tamanho, total);
    }

    @Override
    public Optional<AnimalDetalhe> detalharAnimal(UUID organizacaoId, UUID animalId) {
        return jdbc.sql(SELECAO_ANIMAL + " where a.organizacao_id = :org and a.id = :id")
                .param("org", organizacaoId)
                .param("id", animalId)
                .query((rs, n) -> new AnimalDetalhe(
                        uuid(rs, "id"), uuid(rs, "propriedade_id"), rs.getLong("numero"), rs.getString("sexo"),
                        rs.getString("categoria"), rs.getString("finalidade"), rs.getString("raca"),
                        rs.getString("situacao"), data(rs, "data_nascimento"), rs.getBoolean("nascimento_estimado"),
                        rs.getString("origem"), rs.getString("origem_descricao"), uuid(rs, "recebimento_id"),
                        instante(rs, "entrada_em"),
                        identificacoes(animalId), periodosLote(animalId), periodosLocalizacao(animalId)))
                .optional();
    }

    @Override
    public List<LoteResumo> listarLotes(UUID organizacaoId, UUID propriedadeId, boolean incluirEncerrados) {
        return jdbc.sql("""
                        select l.id, l.propriedade_id, l.codigo, l.fase, l.finalidade, l.status, l.inicio, l.fim,
                               l.observacao,
                               (select count(*) from animal_lote_vinculo v where v.lote_id = l.id and v.fim is null)
                                   as quantidade
                        from lote l
                        where l.organizacao_id = :org and l.propriedade_id = :prop
                          and (l.status = 'ATIVO' or :incluirEncerrados)
                        order by l.status, l.codigo
                        """)
                .param("org", organizacaoId)
                .param("prop", propriedadeId)
                .param("incluirEncerrados", incluirEncerrados)
                .query((rs, n) -> new LoteResumo(uuid(rs, "id"), uuid(rs, "propriedade_id"), rs.getString("codigo"),
                        rs.getString("fase"), rs.getString("finalidade"), rs.getString("status"),
                        instante(rs, "inicio"), instante(rs, "fim"), rs.getString("observacao"),
                        rs.getLong("quantidade")))
                .list();
    }

    private List<IdentificacaoHistorico> identificacoes(UUID animalId) {
        return jdbc.sql("select tipo, valor, inicio, fim from animal_identificacao where animal_id = :id order by inicio")
                .param("id", animalId)
                .query((rs, n) -> new IdentificacaoHistorico(rs.getString("tipo"), rs.getString("valor"),
                        instante(rs, "inicio"), instante(rs, "fim")))
                .list();
    }

    private List<PeriodoLote> periodosLote(UUID animalId) {
        return jdbc.sql("""
                        select v.lote_id, l.codigo, v.inicio, v.fim from animal_lote_vinculo v
                        join lote l on l.id = v.lote_id
                        where v.animal_id = :id order by v.inicio desc
                        """)
                .param("id", animalId)
                .query((rs, n) -> new PeriodoLote(uuid(rs, "lote_id"), rs.getString("codigo"),
                        instante(rs, "inicio"), instante(rs, "fim")))
                .list();
    }

    private List<PeriodoLocalizacao> periodosLocalizacao(UUID animalId) {
        return jdbc.sql("select instalacao_id, inicio, fim from animal_localizacao where animal_id = :id order by inicio desc")
                .param("id", animalId)
                .query((rs, n) -> new PeriodoLocalizacao(uuid(rs, "instalacao_id"), instante(rs, "inicio"),
                        instante(rs, "fim")))
                .list();
    }

    private static AnimalResumo resumo(ResultSet rs) throws SQLException {
        return new AnimalResumo(uuid(rs, "id"), rs.getLong("numero"), rs.getString("brinco"),
                rs.getString("eletronico"), rs.getString("sexo"), rs.getString("categoria"),
                rs.getString("finalidade"), rs.getString("raca"), rs.getString("situacao"),
                data(rs, "data_nascimento"), rs.getBoolean("nascimento_estimado"), uuid(rs, "lote_id"),
                rs.getString("lote_codigo"), uuid(rs, "instalacao_id"), instante(rs, "entrada_em"));
    }

    static String escaparLike(String valor) {
        return valor.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    static UUID uuid(ResultSet rs, String coluna) throws SQLException {
        return rs.getObject(coluna, UUID.class);
    }

    static Instant instante(ResultSet rs, String coluna) throws SQLException {
        Timestamp t = rs.getTimestamp(coluna);
        return t == null ? null : t.toInstant();
    }

    static LocalDate data(ResultSet rs, String coluna) throws SQLException {
        return rs.getObject(coluna, LocalDate.class);
    }
}
