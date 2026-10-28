package br.com.sysagropec.pesagem.adapter.out.modulos;

import br.com.sysagropec.pesagem.application.port.out.PesagemPorts.AnimaisPort;
import br.com.sysagropec.pesagem.application.port.out.PesagemPorts.TarefasPort;
import br.com.sysagropec.rebanho.application.port.in.RebanhoInternoQuery;
import br.com.sysagropec.shared.application.seguranca.ContextoExecucao;
import br.com.sysagropec.tarefas.application.port.in.TarefasResultadoUseCase;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/** Integração com os contratos públicos de rebanho e tarefas. */
@Component
class PesagemIntegracoesAdapter implements AnimaisPort, TarefasPort {

    private final RebanhoInternoQuery rebanho;
    private final TarefasResultadoUseCase tarefas;

    PesagemIntegracoesAdapter(RebanhoInternoQuery rebanho, TarefasResultadoUseCase tarefas) {
        this.rebanho = rebanho;
        this.tarefas = tarefas;
    }

    @Override
    public Optional<AnimalPesavel> animal(UUID organizacaoId, UUID animalId) {
        return rebanho.animal(organizacaoId, animalId)
                .map(a -> new AnimalPesavel(a.id(), a.propriedadeId(), a.ativo(), a.entradaEm()));
    }

    @Override
    public boolean vincularPesagem(ContextoExecucao ctx, UUID tarefaId, UUID animalId, UUID pesagemId) {
        return tarefas.registrarResultado(ctx, tarefaId,
                new TarefasResultadoUseCase.Resultado("PESAGEM", pesagemId, "ANIMAL", animalId));
    }
}
