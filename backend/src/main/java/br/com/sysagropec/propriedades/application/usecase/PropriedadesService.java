package br.com.sysagropec.propriedades.application.usecase;

import br.com.sysagropec.propriedades.application.port.in.PropriedadesInternasQuery;
import br.com.sysagropec.propriedades.application.port.in.PropriedadesUseCase;
import br.com.sysagropec.propriedades.application.port.out.PropriedadeRepositoryPort;
import br.com.sysagropec.propriedades.domain.model.Instalacao;
import br.com.sysagropec.propriedades.domain.model.Propriedade;
import br.com.sysagropec.propriedades.domain.model.TipoInstalacao;
import br.com.sysagropec.shared.application.port.out.AuditoriaPort;
import br.com.sysagropec.shared.application.port.out.AuditoriaPort.EventoAuditoria;
import br.com.sysagropec.shared.application.port.out.RelogioPort;
import br.com.sysagropec.shared.application.seguranca.ContextoExecucao;
import br.com.sysagropec.shared.domain.acesso.Permissao;
import br.com.sysagropec.shared.domain.ConflitoException;
import br.com.sysagropec.shared.domain.NaoEncontradoException;
import br.com.sysagropec.shared.domain.RegraNegocioException;

import java.time.Instant;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class PropriedadesService implements PropriedadesUseCase, PropriedadesUseCase.Consultas, PropriedadesInternasQuery {

    private final PropriedadeRepositoryPort repositorio;
    private final AuditoriaPort auditoria;
    private final RelogioPort relogio;

    public PropriedadesService(PropriedadeRepositoryPort repositorio, AuditoriaPort auditoria, RelogioPort relogio) {
        this.repositorio = repositorio;
        this.auditoria = auditoria;
        this.relogio = relogio;
    }

    @Override
    public PropriedadeResumo cadastrar(ContextoExecucao ctx, CadastrarPropriedadeComando comando) {
        ctx.exigirNaOrganizacao(Permissao.PROPRIEDADE_GERIR);
        Instant agora = relogio.agora();
        Propriedade propriedade = Propriedade.nova(UUID.randomUUID(), ctx.organizacaoId(), comando.nome(),
                comando.municipio(), comando.uf(), comando.fusoHorario(), agora);
        if (repositorio.existeNome(ctx.organizacaoId(), propriedade.nome())) {
            throw new ConflitoException("Já existe uma propriedade com este nome");
        }
        repositorio.salvar(propriedade);
        auditoria.registrar(new EventoAuditoria(ctx.organizacaoId(), ctx.usuarioId(), "PROPRIEDADE_CADASTRADA",
                "PROPRIEDADE", propriedade.id(), Map.of("nome", propriedade.nome()), agora));
        return resumo(propriedade);
    }

    @Override
    public InstalacaoResumo cadastrarInstalacao(ContextoExecucao ctx, CadastrarInstalacaoComando comando) {
        ctx.exigir(Permissao.INSTALACAO_GERIR, comando.propriedadeId());
        Propriedade propriedade = repositorio.buscar(ctx.organizacaoId(), comando.propriedadeId())
                .orElseThrow(() -> new NaoEncontradoException("Propriedade"));
        Instalacao instalacao = Instalacao.nova(UUID.randomUUID(), ctx.organizacaoId(), propriedade.id(),
                tipo(comando.tipo()), comando.codigo(), comando.descricao(), comando.capacidadeCabecas(),
                relogio.agora());
        if (repositorio.existeCodigoInstalacao(propriedade.id(), instalacao.codigo())) {
            throw new ConflitoException("Já existe uma instalação com o código " + instalacao.codigo());
        }
        repositorio.salvar(instalacao);
        return resumo(instalacao);
    }

    @Override
    public List<PropriedadeResumo> listar(ContextoExecucao ctx) {
        Set<UUID> visiveis = ctx.propriedadesCom(Permissao.CONSULTAR);
        return repositorio.listar(ctx.organizacaoId(), visiveis).stream()
                .sorted(Comparator.comparing(Propriedade::nome, String.CASE_INSENSITIVE_ORDER))
                .map(PropriedadesService::resumo)
                .toList();
    }

    @Override
    public List<InstalacaoResumo> listarInstalacoes(ContextoExecucao ctx, UUID propriedadeId) {
        ctx.exigir(Permissao.CONSULTAR, propriedadeId);
        return repositorio.listarInstalacoes(ctx.organizacaoId(), propriedadeId).stream()
                .sorted(Comparator.comparing(Instalacao::codigo, String.CASE_INSENSITIVE_ORDER))
                .map(PropriedadesService::resumo)
                .toList();
    }

    @Override
    public Set<UUID> idsAtivos(UUID organizacaoId) {
        return repositorio.listarAtivas(organizacaoId).stream().map(Propriedade::id).collect(Collectors.toSet());
    }

    @Override
    public Optional<ZoneId> fusoHorario(UUID organizacaoId, UUID propriedadeId) {
        return repositorio.buscar(organizacaoId, propriedadeId).map(Propriedade::fusoHorario);
    }

    @Override
    public Optional<InstalacaoInfo> instalacao(UUID organizacaoId, UUID instalacaoId) {
        return repositorio.buscarInstalacao(organizacaoId, instalacaoId).map(PropriedadesService::info);
    }

    @Override
    public Optional<InstalacaoInfo> instalacaoComBloqueio(UUID organizacaoId, UUID instalacaoId) {
        return repositorio.buscarInstalacaoComBloqueio(organizacaoId, instalacaoId).map(PropriedadesService::info);
    }

    private static TipoInstalacao tipo(String valor) {
        try {
            return TipoInstalacao.valueOf(valor);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new RegraNegocioException("Tipo de instalação inválido");
        }
    }

    private static PropriedadeResumo resumo(Propriedade p) {
        return new PropriedadeResumo(p.id(), p.nome(), p.municipio(), p.uf(), p.fusoHorario().getId(), p.ativa(),
                p.criadoEm());
    }

    private static InstalacaoResumo resumo(Instalacao i) {
        return new InstalacaoResumo(i.id(), i.propriedadeId(), i.tipo().name(), i.codigo(), i.descricao(),
                i.capacidadeCabecas(), i.ativa());
    }

    private static InstalacaoInfo info(Instalacao i) {
        return new InstalacaoInfo(i.id(), i.propriedadeId(), i.codigo(), i.capacidadeCabecas(), i.ativa());
    }
}
