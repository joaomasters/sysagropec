package br.com.sysagropec.pesagem.application.port.out;

import br.com.sysagropec.pesagem.domain.model.Pesagem;
import br.com.sysagropec.shared.application.seguranca.ContextoExecucao;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class PesagemPorts {

    private PesagemPorts() {
    }

    public interface PesagemRepositoryPort {
        void salvar(Pesagem pesagem);

        Optional<Pesagem> buscar(UUID organizacaoId, UUID pesagemId);

        List<Pesagem> listarPorAnimal(UUID organizacaoId, UUID animalId);

        boolean existeEntradaValida(UUID animalId);
    }

    public interface AnimaisPort {
        Optional<AnimalPesavel> animal(UUID organizacaoId, UUID animalId);

        record AnimalPesavel(UUID id, UUID propriedadeId, boolean ativo, Instant entradaEm) {
        }
    }

    public interface TarefasPort {
        /** @return true se a tarefa foi concluída por este registro */
        boolean vincularPesagem(ContextoExecucao ctx, UUID tarefaId, UUID animalId, UUID pesagemId);
    }
}
