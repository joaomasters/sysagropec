package br.com.sysagropec.propriedades.application.port.in;

import br.com.sysagropec.shared.application.seguranca.ContextoExecucao;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface PropriedadesUseCase {

    PropriedadeResumo cadastrar(ContextoExecucao ctx, CadastrarPropriedadeComando comando);

    InstalacaoResumo cadastrarInstalacao(ContextoExecucao ctx, CadastrarInstalacaoComando comando);

    record CadastrarPropriedadeComando(String nome, String municipio, String uf, String fusoHorario) {
    }

    record CadastrarInstalacaoComando(UUID propriedadeId, String tipo, String codigo, String descricao,
                                      Integer capacidadeCabecas) {
    }

    record PropriedadeResumo(UUID id, String nome, String municipio, String uf, String fusoHorario, boolean ativa,
                             Instant criadoEm) {
    }

    record InstalacaoResumo(UUID id, UUID propriedadeId, String tipo, String codigo, String descricao,
                            Integer capacidadeCabecas, boolean ativa) {
    }

    interface Consultas {
        List<PropriedadeResumo> listar(ContextoExecucao ctx);

        List<InstalacaoResumo> listarInstalacoes(ContextoExecucao ctx, UUID propriedadeId);
    }
}
