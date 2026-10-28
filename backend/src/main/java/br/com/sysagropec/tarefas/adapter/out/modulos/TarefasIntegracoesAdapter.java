package br.com.sysagropec.tarefas.adapter.out.modulos;

import br.com.sysagropec.identidade.application.port.in.UsuariosInternosQuery;
import br.com.sysagropec.propriedades.application.port.in.PropriedadesInternasQuery;
import br.com.sysagropec.rebanho.application.port.in.RebanhoInternoQuery;
import br.com.sysagropec.shared.domain.NaoEncontradoException;
import br.com.sysagropec.shared.domain.acesso.Permissao;
import br.com.sysagropec.tarefas.application.port.out.TarefasPorts.AlvosPort;
import br.com.sysagropec.tarefas.application.port.out.TarefasPorts.PropriedadesPort;
import br.com.sysagropec.tarefas.application.port.out.TarefasPorts.UsuariosPort;
import br.com.sysagropec.tarefas.domain.model.Tarefa;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Resolve alvos, responsáveis e fuso por meio dos contratos públicos de outros módulos. */
@Component
class TarefasIntegracoesAdapter implements AlvosPort, UsuariosPort, PropriedadesPort {

    private final PropriedadesInternasQuery propriedades;
    private final RebanhoInternoQuery rebanho;
    private final UsuariosInternosQuery usuarios;

    TarefasIntegracoesAdapter(PropriedadesInternasQuery propriedades, RebanhoInternoQuery rebanho,
                              UsuariosInternosQuery usuarios) {
        this.propriedades = propriedades;
        this.rebanho = rebanho;
        this.usuarios = usuarios;
    }

    @Override
    public boolean pertence(UUID organizacaoId, UUID propriedadeId, Tarefa.Alvo alvo) {
        return switch (alvo.tipo()) {
            case PROPRIEDADE -> alvo.id().equals(propriedadeId)
                    && propriedades.fusoHorario(organizacaoId, propriedadeId).isPresent();
            case INSTALACAO -> propriedades.instalacao(organizacaoId, alvo.id())
                    .filter(i -> i.propriedadeId().equals(propriedadeId)).isPresent();
            case LOTE -> rebanho.lote(organizacaoId, alvo.id())
                    .filter(l -> l.propriedadeId().equals(propriedadeId)).isPresent();
            case ANIMAL -> rebanho.animal(organizacaoId, alvo.id())
                    .filter(a -> a.propriedadeId().equals(propriedadeId)).isPresent();
        };
    }

    @Override
    public Optional<String> descrever(UUID organizacaoId, Tarefa.Alvo alvo) {
        return switch (alvo.tipo()) {
            case PROPRIEDADE -> Optional.of("Propriedade");
            case INSTALACAO -> propriedades.instalacao(organizacaoId, alvo.id()).map(i -> "Instalação " + i.codigo());
            case LOTE -> rebanho.lote(organizacaoId, alvo.id()).map(l -> "Lote " + l.codigo());
            case ANIMAL -> rebanho.animal(organizacaoId, alvo.id())
                    .map(a -> "Animal Nº " + a.numero() + (a.brinco() == null ? "" : " (brinco " + a.brinco() + ")"));
        };
    }

    @Override
    public boolean podeExecutar(UUID organizacaoId, UUID usuarioId, UUID propriedadeId) {
        return usuarios.possuiPermissao(organizacaoId, usuarioId, propriedadeId, Permissao.TAREFA_EXECUTAR);
    }

    @Override
    public Map<UUID, String> nomes(UUID organizacaoId, Collection<UUID> usuarioIds) {
        return usuarios.nomes(organizacaoId, usuarioIds);
    }

    @Override
    public ZoneId fusoHorario(UUID organizacaoId, UUID propriedadeId) {
        return propriedades.fusoHorario(organizacaoId, propriedadeId)
                .orElseThrow(() -> new NaoEncontradoException("Propriedade"));
    }
}
