package br.com.sysagropec.consultas.application.usecase;

import br.com.sysagropec.consultas.application.port.in.ConsultasQuery;
import br.com.sysagropec.consultas.application.port.out.LeituraConsolidadaPort;
import br.com.sysagropec.shared.application.port.out.RelogioPort;
import br.com.sysagropec.shared.application.seguranca.ContextoExecucao;
import br.com.sysagropec.shared.domain.NaoEncontradoException;
import br.com.sysagropec.shared.domain.acesso.Permissao;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.UUID;

public class ConsultasService implements ConsultasQuery {

    private final LeituraConsolidadaPort leitura;
    private final RelogioPort relogio;

    public ConsultasService(LeituraConsolidadaPort leitura, RelogioPort relogio) {
        this.leitura = leitura;
        this.relogio = relogio;
    }

    @Override
    public LinhaDoTempo linhaDoTempo(ContextoExecucao ctx, UUID animalId) {
        UUID propriedadeId = leitura.propriedadeDoAnimal(ctx.organizacaoId(), animalId)
                .filter(p -> ctx.pode(Permissao.CONSULTAR, p))
                .orElseThrow(() -> new NaoEncontradoException("Animal"));
        return new LinhaDoTempo(animalId, leitura.eventosDoAnimal(ctx.organizacaoId(), animalId).stream()
                .sorted(Comparator.comparing(Evento::ocorridoEm).thenComparing(Evento::registradoEm).reversed())
                .toList());
    }

    @Override
    public Dashboard dashboard(ContextoExecucao ctx, UUID propriedadeId) {
        ctx.exigir(Permissao.CONSULTAR, propriedadeId);
        ZoneId zona = leitura.fusoHorario(ctx.organizacaoId(), propriedadeId)
                .orElseThrow(() -> new NaoEncontradoException("Propriedade"));
        Instant agora = relogio.agora();
        return leitura.dashboard(ctx.organizacaoId(), propriedadeId, LocalDate.ofInstant(agora, zona), zona, agora);
    }
}
