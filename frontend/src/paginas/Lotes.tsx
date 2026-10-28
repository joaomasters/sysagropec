import { useState, type FormEvent } from 'react'
import { Link, useLocation } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from '../api/cliente'
import type { AnimalResumo, Pagina } from '../api/tipos'
import { useInstalacoes, useLotes } from '../api/consultas'
import { usePropriedade, useSessao } from '../sessao/SessaoContext'
import { AreaTexto, Campo, Carregando, Erro, Mensagem, Selecao, Selo, Vazio } from '../componentes/ui'
import { agoraLocal, dataHora, opcoes, paraInstante, rotulo } from '../util/formatos'

export function Lotes() {
  const propriedade = usePropriedade()
  const { pode } = useSessao()
  const cliente = useQueryClient()
  const [incluirEncerrados, setIncluirEncerrados] = useState(false)
  const lotes = useLotes(propriedade.id, incluirEncerrados)

  const encerrar = useMutation({
    mutationFn: (id: string) => api.post(`/api/v1/lotes/${id}/encerramento`, {}),
    onSuccess: () => void cliente.invalidateQueries({ queryKey: ['lotes'] }),
  })

  return (
    <>
      <div className="cabecalho-pagina">
        <h1>Lotes</h1>
        <label className="marcar">
          <input type="checkbox" checked={incluirEncerrados} onChange={(e) => setIncluirEncerrados(e.target.checked)} />
          Mostrar encerrados
        </label>
      </div>
      <section className="cartao">
        <Erro erro={encerrar.error} />
        {lotes.isPending ? <Carregando /> : lotes.error ? <Erro erro={lotes.error} /> : lotes.data.length === 0 ? (
          <Vazio titulo="Nenhum lote" />
        ) : (
          <div className="tabela-rolagem">
            <table>
              <thead><tr><th>Código</th><th>Fase</th><th>Finalidade</th><th className="num">Animais</th><th>Início</th><th>Status</th><th /></tr></thead>
              <tbody>
                {lotes.data.map((l) => (
                  <tr key={l.id}>
                    <td><Link to={`/animais?lote=${l.id}`}>{l.codigo}</Link></td>
                    <td>{rotulo('fase', l.fase)}</td>
                    <td>{rotulo('finalidade', l.finalidade)}</td>
                    <td className="num">{l.quantidadeAnimais}</td>
                    <td>{dataHora(l.inicio)}</td>
                    <td><Selo cor={l.status === 'ATIVO' ? 'verde' : undefined}>{l.status === 'ATIVO' ? 'Ativo' : `Encerrado ${dataHora(l.fim)}`}</Selo></td>
                    <td>
                      {pode('LOTE_GERIR') && l.status === 'ATIVO' && l.quantidadeAnimais === 0 && (
                        <button type="button" className="discreto" disabled={encerrar.isPending}
                                onClick={() => { if (confirm(`Encerrar o lote ${l.codigo}?`)) encerrar.mutate(l.id) }}>
                          Encerrar
                        </button>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>
      {pode('LOTE_GERIR') && <NovoLote />}
    </>
  )
}

function NovoLote() {
  const propriedade = usePropriedade()
  const cliente = useQueryClient()
  const preSelecionados: string[] = (useLocation().state as { animalIds?: string[] } | null)?.animalIds ?? []
  const instalacoes = useInstalacoes(propriedade.id)
  const semLote = useQuery({
    queryKey: ['animais', propriedade.id, 'sem-lote'],
    queryFn: () => api.get<Pagina<AnimalResumo>>(`/api/v1/propriedades/${propriedade.id}/animais?semLote=true&situacao=ATIVO&tamanho=200`),
  })

  const [codigo, setCodigo] = useState('')
  const [fase, setFase] = useState('ADAPTACAO')
  const [finalidade, setFinalidade] = useState('ENGORDA')
  const [instalacaoId, setInstalacaoId] = useState('')
  const [ocorridoEm, setOcorridoEm] = useState(agoraLocal)
  const [observacao, setObservacao] = useState('')
  const [animais, setAnimais] = useState<Set<string>>(new Set(preSelecionados))

  const formar = useMutation({
    mutationFn: () =>
      api.post(`/api/v1/propriedades/${propriedade.id}/lotes`, {
        codigo, fase, finalidade, observacao: observacao || null, ocorridoEm: paraInstante(ocorridoEm),
        animalIds: [...animais], instalacaoId: instalacaoId || null,
      }),
    onSuccess: () => {
      setCodigo('')
      setObservacao('')
      setAnimais(new Set())
      void cliente.invalidateQueries()
    },
  })

  function enviar(e: FormEvent) {
    e.preventDefault()
    formar.mutate()
  }

  const candidatos = semLote.data?.itens ?? []
  const externos = preSelecionados.filter((id) => !candidatos.some((c) => c.id === id))

  return (
    <form className="cartao grade" onSubmit={enviar}>
      <h2>Formar lote</h2>
      {formar.isSuccess && <Mensagem tipo="sucesso">Lote formado.</Mensagem>}
      <div className="grade grade-4">
        <Campo rotulo="Código" required maxLength={40} value={codigo} onChange={(e) => setCodigo(e.target.value)} />
        <Selecao rotulo="Fase" value={fase} onChange={(e) => setFase(e.target.value)} opcoes={opcoes('fase')} />
        <Selecao rotulo="Finalidade" value={finalidade} onChange={(e) => setFinalidade(e.target.value)} opcoes={opcoes('finalidade')} />
        <Campo rotulo="Data e hora de formação" type="datetime-local" required value={ocorridoEm} onChange={(e) => setOcorridoEm(e.target.value)} />
        <Selecao rotulo="Mover para a instalação" value={instalacaoId} vazio="Manter onde estão"
                 onChange={(e) => setInstalacaoId(e.target.value)}
                 opcoes={(instalacoes.data ?? []).filter((i) => i.ativa).map((i) => ({ valor: i.id, rotulo: i.codigo }))} />
      </div>
      <AreaTexto rotulo="Observação" maxLength={500} value={observacao} onChange={(e) => setObservacao(e.target.value)} />

      <fieldset style={{ border: 0, padding: 0, margin: 0 }}>
        <legend><strong>Animais ({animais.size} selecionados)</strong></legend>
        {externos.length > 0 && (
          <Mensagem tipo="info">{externos.length} animal(is) selecionado(s) na lista já pertence(m) a outro lote e será(ão) transferido(s).</Mensagem>
        )}
        {semLote.isPending ? <Carregando /> : candidatos.length === 0 ? (
          <p className="vazio">Nenhum animal ativo sem lote. Também é possível formar o lote vazio.</p>
        ) : (
          <div className="grade grade-4">
            {candidatos.map((a) => (
              <label key={a.id} className="marcar">
                <input type="checkbox" checked={animais.has(a.id)}
                       onChange={() => setAnimais((s) => {
                         const n = new Set(s)
                         if (n.has(a.id)) n.delete(a.id)
                         else n.add(a.id)
                         return n
                       })} />
                {a.brinco ?? `Nº ${a.numero}`}
              </label>
            ))}
          </div>
        )}
      </fieldset>
      <Erro erro={formar.error} />
      <div><button className="primario" type="submit" disabled={formar.isPending}>Formar lote</button></div>
    </form>
  )
}
