package br.com.sysagropec.rebanho.application.usecase;

import br.com.sysagropec.rebanho.application.port.in.RebanhoConsultas;
import br.com.sysagropec.rebanho.application.port.in.RebanhoInternoQuery;
import br.com.sysagropec.rebanho.application.port.out.RebanhoPorts.AnimalRepositoryPort;
import br.com.sysagropec.rebanho.application.port.out.RebanhoPorts.LoteRepositoryPort;
import br.com.sysagropec.rebanho.application.port.out.RebanhoPorts.RebanhoLeituraPort;
import br.com.sysagropec.rebanho.domain.model.Enums.Categoria;
import br.com.sysagropec.rebanho.domain.model.Enums.SituacaoAnimal;
import br.com.sysagropec.rebanho.domain.model.Enums.StatusLote;
import br.com.sysagropec.rebanho.domain.model.Enums.TipoIdentificacao;
import br.com.sysagropec.rebanho.domain.model.Vinculo;
import br.com.sysagropec.shared.application.Pagina;
import br.com.sysagropec.shared.application.seguranca.ContextoExecucao;
import br.com.sysagropec.shared.domain.NaoEncontradoException;
import br.com.sysagropec.shared.domain.acesso.Permissao;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class RebanhoConsultasService implements RebanhoConsultas, RebanhoInternoQuery {

    private final RebanhoLeituraPort leitura;
    private final AnimalRepositoryPort animais;
    private final LoteRepositoryPort lotes;

    public RebanhoConsultasService(RebanhoLeituraPort leitura, AnimalRepositoryPort animais, LoteRepositoryPort lotes) {
        this.leitura = leitura;
        this.animais = animais;
        this.lotes = lotes;
    }

    @Override
    public Pagina<AnimalResumo> listarAnimais(ContextoExecucao ctx, FiltroAnimais filtro) {
        ctx.exigir(Permissao.CONSULTAR, filtro.propriedadeId());
        if (filtro.categoria() != null) {
            RebanhoService.enumObrigatorio(Categoria.class, filtro.categoria(), "Categoria");
        }
        if (filtro.situacao() != null) {
            RebanhoService.enumObrigatorio(SituacaoAnimal.class, filtro.situacao(), "Situação");
        }
        return leitura.listarAnimais(ctx.organizacaoId(), filtro, Pagina.paginaValida(filtro.pagina()),
                Pagina.tamanhoValido(filtro.tamanho()));
    }

    @Override
    public AnimalDetalhe detalharAnimal(ContextoExecucao ctx, UUID animalId) {
        AnimalDetalhe detalhe = leitura.detalharAnimal(ctx.organizacaoId(), animalId)
                .orElseThrow(() -> new NaoEncontradoException("Animal"));
        exigirConsulta(ctx, detalhe.propriedadeId(), "Animal");
        return detalhe;
    }

    @Override
    public List<LoteResumo> listarLotes(ContextoExecucao ctx, UUID propriedadeId, boolean incluirEncerrados) {
        ctx.exigir(Permissao.CONSULTAR, propriedadeId);
        return leitura.listarLotes(ctx.organizacaoId(), propriedadeId, incluirEncerrados);
    }

    @Override
    public Optional<AnimalInfo> animal(UUID organizacaoId, UUID animalId) {
        return animais.buscar(organizacaoId, animalId).map(a -> new AnimalInfo(a.id(), a.propriedadeId(), a.numero(),
                a.identificacaoAtiva(TipoIdentificacao.BRINCO).orElse(null), a.ativo(),
                a.localizacoes().stream().map(Vinculo::inicio).min(Comparator.naturalOrder()).orElse(a.criadoEm())));
    }

    @Override
    public Optional<LoteInfo> lote(UUID organizacaoId, UUID loteId) {
        return lotes.buscarLote(organizacaoId, loteId)
                .map(l -> new LoteInfo(l.id(), l.propriedadeId(), l.codigo(), l.status() == StatusLote.ATIVO));
    }

    /** Recurso de propriedade sem acesso é tratado como inexistente. */
    private static void exigirConsulta(ContextoExecucao ctx, UUID propriedadeId, String recurso) {
        if (!ctx.pode(Permissao.CONSULTAR, propriedadeId)) {
            throw new NaoEncontradoException(recurso);
        }
    }
}
