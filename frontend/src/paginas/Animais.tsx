import { useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { api, querystring } from '../api/cliente'
import type { AnimalResumo, Pagina } from '../api/tipos'
import { useCodigosInstalacao, useInstalacoes, useLotes } from '../api/consultas'
import { usePropriedade, useSessao } from '../sessao/SessaoContext'
import { Campo, Carregando, Erro, Paginacao, Selecao, Selo, Vazio } from '../componentes/ui'
import { data, opcoes, rotulo } from '../util/formatos'

export function Animais() {
  const propriedade = usePropriedade()
  const { pode } = useSessao()
  const navegar = useNavigate()
  const [parametros, setParametros] = useSearchParams()
  const [selecionados, setSelecionados] = useState<Set<string>>(new Set())

  const filtro = {
    busca: parametros.get('busca') ?? '',
    loteId: parametros.get('lote') ?? '',
    instalacaoId: parametros.get('instalacao') ?? '',
    categoria: parametros.get('categoria') ?? '',
    situacao: parametros.get('situacao') ?? 'ATIVO',
    pagina: Number(parametros.get('pagina') ?? 0),
  }
  const semLote = filtro.loteId === 'sem'

  const lotes = useLotes(propriedade.id)
  const instalacoes = useInstalacoes(propriedade.id)
  const codigos = useCodigosInstalacao(propriedade.id)
  const consulta = useQuery({
    queryKey: ['animais', propriedade.id, filtro],
    queryFn: () =>
      api.get<Pagina<AnimalResumo>>(`/api/v1/propriedades/${propriedade.id}/animais` + querystring({
        busca: filtro.busca, loteId: semLote ? undefined : filtro.loteId, semLote: semLote || undefined,
        instalacaoId: filtro.instalacaoId, categoria: filtro.categoria, situacao: filtro.situacao,
        pagina: filtro.pagina, tamanho: 50,
      })),
    placeholderData: keepPreviousData,
  })

  function mudar(chave: string, valor: string) {
    const novos = new URLSearchParams(parametros)
    if (valor) novos.set(chave, valor)
    else novos.delete(chave)
    if (chave !== 'pagina') novos.delete('pagina')
    setParametros(novos, { replace: true })
  }

  function alternar(id: string) {
    setSelecionados((s) => {
      const n = new Set(s)
      if (n.has(id)) n.delete(id)
      else n.add(id)
      return n
    })
  }

  const itens = consulta.data?.itens ?? []
  const podeMover = pode('ANIMAL_MOVIMENTAR')
  const todosMarcados = itens.length > 0 && itens.every((a) => selecionados.has(a.id))

  return (
    <>
      <div className="cabecalho-pagina">
        <h1>Animais</h1>
        {pode('ANIMAL_RECEBER') && <Link className="botao primario" to="/animais/receber">Receber animais</Link>}
      </div>

      <section className="cartao grade grade-4" aria-label="Filtros">
        <Campo rotulo="Buscar" placeholder="Brinco, eletrônico ou nº" defaultValue={filtro.busca}
               onKeyDown={(e) => { if (e.key === 'Enter') mudar('busca', e.currentTarget.value) }}
               onBlur={(e) => mudar('busca', e.target.value)} />
        <Selecao rotulo="Lote" value={filtro.loteId} vazio="Todos" onChange={(e) => mudar('lote', e.target.value)}
                 opcoes={[{ valor: 'sem', rotulo: 'Sem lote' }, ...(lotes.data ?? []).map((l) => ({ valor: l.id, rotulo: l.codigo }))]} />
        <Selecao rotulo="Instalação" value={filtro.instalacaoId} vazio="Todas" onChange={(e) => mudar('instalacao', e.target.value)}
                 opcoes={(instalacoes.data ?? []).map((i) => ({ valor: i.id, rotulo: i.codigo }))} />
        <Selecao rotulo="Categoria" value={filtro.categoria} vazio="Todas" onChange={(e) => mudar('categoria', e.target.value)}
                 opcoes={opcoes('categoria')} />
      </section>

      {podeMover && selecionados.size > 0 && (
        <div className="mensagem info linha" role="status">
          <span style={{ flex: 1 }}>{selecionados.size} animal(is) selecionado(s)</span>
          <button type="button" className="primario"
                  onClick={() => navegar('/animais/movimentar', { state: { animalIds: [...selecionados] } })}>
            Movimentar
          </button>
          {pode('LOTE_GERIR') && (
            <button type="button" onClick={() => navegar('/lotes', { state: { animalIds: [...selecionados] } })}>
              Formar lote
            </button>
          )}
          <button type="button" className="discreto" onClick={() => setSelecionados(new Set())}>Limpar</button>
        </div>
      )}

      <section className="cartao">
        {consulta.isPending ? <Carregando /> : consulta.error ? <Erro erro={consulta.error} /> : itens.length === 0 ? (
          <Vazio titulo="Nenhum animal encontrado">Ajuste os filtros ou registre um recebimento.</Vazio>
        ) : (
          <>
            <div className="tabela-rolagem">
              <table>
                <thead>
                  <tr>
                    {podeMover && (
                      <th>
                        <input type="checkbox" aria-label="Selecionar todos da página" checked={todosMarcados}
                               onChange={() => setSelecionados((s) => {
                                 const n = new Set(s)
                                 itens.forEach((a) => (todosMarcados ? n.delete(a.id) : n.add(a.id)))
                                 return n
                               })} />
                      </th>
                    )}
                    <th className="num">Nº</th><th>Brinco</th><th>Categoria</th><th>Raça</th><th>Lote</th>
                    <th>Instalação</th><th>Entrada</th><th>Situação</th>
                  </tr>
                </thead>
                <tbody>
                  {itens.map((a) => (
                    <tr key={a.id}>
                      {podeMover && (
                        <td><input type="checkbox" aria-label={`Selecionar animal ${a.brinco ?? a.numero}`}
                                   checked={selecionados.has(a.id)} onChange={() => alternar(a.id)} /></td>
                      )}
                      <td className="num">{a.numero}</td>
                      <td><Link to={`/animais/${a.id}`}>{a.brinco ?? '—'}</Link></td>
                      <td>{rotulo('categoria', a.categoria)}</td>
                      <td>{a.raca ?? '—'}</td>
                      <td>{a.loteCodigo ?? <Selo cor="alerta">sem lote</Selo>}</td>
                      <td>{a.instalacaoId ? codigos[a.instalacaoId] ?? '…' : '—'}</td>
                      <td>{data(a.entradaEm)}</td>
                      <td><Selo cor={a.situacao === 'ATIVO' ? 'verde' : undefined}>{rotulo('situacao', a.situacao)}</Selo></td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <Paginacao pagina={consulta.data.pagina} tamanho={consulta.data.tamanho} total={consulta.data.total}
                       aoMudar={(p) => mudar('pagina', String(p))} />
          </>
        )}
      </section>
    </>
  )
}
