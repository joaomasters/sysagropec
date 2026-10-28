package br.com.sysagropec.rebanho.application.port.out;

import br.com.sysagropec.rebanho.application.port.in.RebanhoConsultas.AnimalDetalhe;
import br.com.sysagropec.rebanho.application.port.in.RebanhoConsultas.AnimalResumo;
import br.com.sysagropec.rebanho.application.port.in.RebanhoConsultas.FiltroAnimais;
import br.com.sysagropec.rebanho.application.port.in.RebanhoConsultas.LoteResumo;
import br.com.sysagropec.rebanho.domain.model.Animal;
import br.com.sysagropec.rebanho.domain.model.Enums.TipoIdentificacao;
import br.com.sysagropec.rebanho.domain.model.Lote;
import br.com.sysagropec.rebanho.domain.model.Movimentacao;
import br.com.sysagropec.rebanho.domain.model.Recebimento;
import br.com.sysagropec.shared.application.Pagina;

import java.time.ZoneId;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class RebanhoPorts {

    private RebanhoPorts() {
    }

    public interface AnimalRepositoryPort {
        void salvar(Animal animal);

        Optional<Animal> buscar(UUID organizacaoId, UUID animalId);

        List<Animal> buscarVarios(UUID organizacaoId, Collection<UUID> animalIds);

        List<Animal> listarPorRecebimento(UUID organizacaoId, UUID recebimentoId);

        /** Valores (em minúsculas) que já identificam um animal ativo da organização. */
        Set<String> identificacoesEmUso(UUID organizacaoId, TipoIdentificacao tipo, Collection<String> valores);

        long contarNaInstalacao(UUID instalacaoId);

        long contarNoLote(UUID loteId);

        /** Reserva atomicamente {@code quantidade} números sequenciais e retorna o primeiro. */
        long reservarNumeros(UUID organizacaoId, int quantidade);
    }

    public interface LoteRepositoryPort {
        void salvar(Lote lote);

        Optional<Lote> buscarLote(UUID organizacaoId, UUID loteId);

        /** Bloqueia o lote até o fim da transação (serializa entradas e encerramento). */
        Optional<Lote> buscarLoteComBloqueio(UUID organizacaoId, UUID loteId);

        boolean existeCodigoAtivo(UUID propriedadeId, String codigo);
    }

    public interface MovimentacaoRepositoryPort {
        void salvar(Movimentacao movimentacao);

        void salvar(Recebimento recebimento);

        Optional<Recebimento> buscarRecebimento(UUID organizacaoId, UUID recebimentoId);
    }

    public interface RebanhoLeituraPort {
        Pagina<AnimalResumo> listarAnimais(UUID organizacaoId, FiltroAnimais filtro, int pagina, int tamanho);

        Optional<AnimalDetalhe> detalharAnimal(UUID organizacaoId, UUID animalId);

        List<LoteResumo> listarLotes(UUID organizacaoId, UUID propriedadeId, boolean incluirEncerrados);
    }

    public interface PropriedadesPort {
        Optional<InstalacaoAlocavel> instalacaoComBloqueio(UUID organizacaoId, UUID instalacaoId);

        ZoneId fusoHorario(UUID organizacaoId, UUID propriedadeId);

        record InstalacaoAlocavel(UUID id, UUID propriedadeId, String codigo, Integer capacidadeCabecas,
                                  boolean ativa) {
        }
    }
}
