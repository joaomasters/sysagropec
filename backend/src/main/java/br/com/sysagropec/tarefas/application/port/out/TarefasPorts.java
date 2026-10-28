package br.com.sysagropec.tarefas.application.port.out;

import br.com.sysagropec.shared.application.Pagina;
import br.com.sysagropec.tarefas.domain.model.Tarefa;

import java.time.Instant;
import java.time.ZoneId;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class TarefasPorts {

    private TarefasPorts() {
    }

    public interface TarefaRepositoryPort {
        void salvar(Tarefa tarefa);

        Optional<Tarefa> buscar(UUID organizacaoId, UUID tarefaId);

        Pagina<Tarefa> listar(ConsultaTarefas consulta);
    }

    /**
     * Critérios combináveis; nulos e conjuntos vazios são ignorados. {@code prazoAntes} e
     * {@code finalizadaDesde} permitem montar a agenda do dia. Ordenação: prazo crescente.
     */
    public record ConsultaTarefas(UUID organizacaoId, UUID propriedadeId, Set<Tarefa.Status> status,
                                  UUID responsavelId, Tarefa.TipoAlvo alvoTipo, UUID alvoId, Instant prazoAntes,
                                  Instant finalizadaDesde, int pagina, int tamanho) {
    }

    public interface AlvosPort {
        /** Verifica se o alvo existe na organização e pertence à propriedade. */
        boolean pertence(UUID organizacaoId, UUID propriedadeId, Tarefa.Alvo alvo);

        Optional<String> descrever(UUID organizacaoId, Tarefa.Alvo alvo);
    }

    public interface UsuariosPort {
        boolean podeExecutar(UUID organizacaoId, UUID usuarioId, UUID propriedadeId);

        Map<UUID, String> nomes(UUID organizacaoId, Collection<UUID> usuarioIds);
    }

    public interface PropriedadesPort {
        ZoneId fusoHorario(UUID organizacaoId, UUID propriedadeId);
    }
}
