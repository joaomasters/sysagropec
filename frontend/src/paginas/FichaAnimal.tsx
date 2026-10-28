import { useState } from 'react'
import { useParams } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from '../api/cliente'
import type { AnimalDetalhe, EventoLinhaDoTempo, Pesagem } from '../api/tipos'
import { useCodigosInstalacao } from '../api/consultas'
import { usePropriedade, useSessao } from '../sessao/SessaoContext'
import { Carregando, Erro, Selo, Vazio } from '../componentes/ui'
import { RegistroPesagem } from '../componentes/RegistroPesagem'
import { GraficoPeso } from '../componentes/GraficoPeso'
import { data, dataHora, kg, rotulo } from '../util/formatos'

export function FichaAnimal() {
  const { id = '' } = useParams()
  const propriedade = usePropriedade()
  const { pode } = useSessao()
  const codigos = useCodigosInstalacao(propriedade.id)

  const animal = useQuery({ queryKey: ['animal', id], queryFn: () => api.get<AnimalDetalhe>(`/api/v1/animais/${id}`) })
  const pesagens = useQuery({ queryKey: ['pesagens', id], queryFn: () => api.get<Pesagem[]>(`/api/v1/animais/${id}/pesagens`) })
  const linha = useQuery({
    queryKey: ['linha-do-tempo', id],
    queryFn: () => api.get<{ eventos: EventoLinhaDoTempo[] }>(`/api/v1/animais/${id}/linha-do-tempo`),
  })

  if (animal.isPending) return <Carregando />
  if (animal.error) return <Erro erro={animal.error} />
  const a = animal.data
  const brinco = a.identificacoes.find((i) => i.tipo === 'BRINCO' && !i.fim)?.valor
  const eletronico = a.identificacoes.find((i) => i.tipo === 'ELETRONICO' && !i.fim)?.valor
  const loteAtual = a.lotes.find((l) => !l.fim)
  const localAtual = a.localizacoes.find((l) => !l.fim)

  return (
    <>
      <div className="cabecalho-pagina">
        <h1>Animal Nº {a.numero} {brinco && <small>· brinco {brinco}</small>}</h1>
        <Selo cor={a.situacao === 'ATIVO' ? 'verde' : undefined}>{rotulo('situacao', a.situacao)}</Selo>
      </div>

      <div className="grade grade-2">
        <section className="cartao">
          <h2>Contexto atual</h2>
          <dl className="dados">
            <dt>Lote</dt><dd>{loteAtual?.loteCodigo ?? <Selo cor="alerta">sem lote</Selo>}</dd>
            <dt>Instalação</dt><dd>{localAtual ? codigos[localAtual.instalacaoId] ?? '…' : '—'}</dd>
            <dt>Entrada</dt><dd>{dataHora(a.entradaEm)}</dd>
            <dt>Categoria</dt><dd>{rotulo('categoria', a.categoria)} · {rotulo('sexo', a.sexo)}</dd>
            <dt>Finalidade</dt><dd>{rotulo('finalidade', a.finalidade)}</dd>
            <dt>Raça</dt><dd>{a.raca ?? '—'}</dd>
            <dt>Nascimento</dt>
            <dd>{a.dataNascimento ? `${data(a.dataNascimento)}${a.nascimentoEstimado ? ' (estimado)' : ''}` : 'não informado'}</dd>
            <dt>Origem</dt><dd>{a.origemDescricao ?? rotulo('origem', a.origem)}</dd>
            {eletronico && <><dt>Ident. eletrônico</dt><dd>{eletronico}</dd></>}
          </dl>
        </section>

        <section className="cartao">
          <h2>Linha do tempo</h2>
          {linha.isPending ? <Carregando /> : linha.error ? <Erro erro={linha.error} /> : linha.data.eventos.length === 0 ? (
            <Vazio titulo="Sem eventos" />
          ) : (
            <ol className="linha-tempo">
              {linha.data.eventos.map((e) => (
                <li key={e.tipo + e.referenciaId}>
                  <div className="quando">
                    {dataHora(e.ocorridoEm)}{e.responsavel && ` · ${e.responsavel}`}
                    {e.registradoEm && Math.abs(new Date(e.registradoEm).getTime() - new Date(e.ocorridoEm).getTime()) > 3_600_000 &&
                      ` · lançado em ${dataHora(e.registradoEm)}`}
                  </div>
                  <strong>{e.titulo}</strong>
                  {e.descricao && <div>{e.descricao}</div>}
                </li>
              ))}
            </ol>
          )}
        </section>
      </div>

      <section className="cartao">
        <h2>Pesagens</h2>
        {pesagens.isPending ? <Carregando /> : pesagens.error ? <Erro erro={pesagens.error} /> : (
          <>
            <GraficoPeso pesagens={pesagens.data} />
            <TabelaPesagens pesagens={pesagens.data} animalId={id} podeCancelar={pode('PESAGEM_CANCELAR')} />
          </>
        )}
        {pode('PESAGEM_REGISTRAR') && a.situacao === 'ATIVO' && (
          <div className="painel">
            <h3>Nova pesagem</h3>
            <RegistroPesagem animalId={id} />
          </div>
        )}
      </section>

      <div className="grade grade-2">
        <section className="cartao">
          <h2>Histórico de lotes</h2>
          {a.lotes.length === 0 ? <Vazio titulo="Nunca esteve em lote" /> : (
            <table>
              <thead><tr><th>Lote</th><th>Início</th><th>Fim</th></tr></thead>
              <tbody>
                {a.lotes.map((l) => (
                  <tr key={l.loteId + l.inicio}><td>{l.loteCodigo}</td><td>{dataHora(l.inicio)}</td><td>{l.fim ? dataHora(l.fim) : 'atual'}</td></tr>
                ))}
              </tbody>
            </table>
          )}
        </section>
        <section className="cartao">
          <h2>Histórico de localização</h2>
          <table>
            <thead><tr><th>Instalação</th><th>Início</th><th>Fim</th></tr></thead>
            <tbody>
              {a.localizacoes.map((l) => (
                <tr key={l.instalacaoId + l.inicio}><td>{codigos[l.instalacaoId] ?? '…'}</td><td>{dataHora(l.inicio)}</td><td>{l.fim ? dataHora(l.fim) : 'atual'}</td></tr>
              ))}
            </tbody>
          </table>
        </section>
      </div>
    </>
  )
}

function TabelaPesagens({ pesagens, animalId, podeCancelar }: { pesagens: Pesagem[]; animalId: string; podeCancelar: boolean }) {
  const cliente = useQueryClient()
  const [cancelando, setCancelando] = useState<string | null>(null)
  const [motivo, setMotivo] = useState('')
  const cancelar = useMutation({
    mutationFn: (id: string) => api.post(`/api/v1/pesagens/${id}/cancelamento`, { motivo }),
    onSuccess: () => {
      setCancelando(null)
      setMotivo('')
      void cliente.invalidateQueries({ queryKey: ['pesagens', animalId] })
      void cliente.invalidateQueries({ queryKey: ['linha-do-tempo', animalId] })
    },
  })

  if (pesagens.length === 0) return <Vazio titulo="Nenhuma pesagem registrada" />
  return (
    <div className="tabela-rolagem">
      <table>
        <thead><tr><th>Data</th><th className="num">Peso</th><th>Natureza</th><th>Método</th><th>Finalidade</th><th /></tr></thead>
        <tbody>
          {pesagens.map((p) => (
            <tr key={p.id} style={p.cancelada ? { opacity: 0.6 } : undefined}>
              <td>{dataHora(p.ocorridoEm)}</td>
              <td className="num">{p.cancelada ? <s>{kg(p.pesoKg)}</s> : kg(p.pesoKg)}</td>
              <td>{p.natureza === 'ESTIMADO' ? <Selo cor="alerta">estimado</Selo> : <Selo cor="verde">medido</Selo>}</td>
              <td>{rotulo('metodo', p.metodo)}</td>
              <td>{rotulo('finalidadePesagem', p.finalidade)}</td>
              <td>
                {p.cancelada ? (
                  <small title={p.motivoCancelamento ?? ''}>cancelada: {p.motivoCancelamento}</small>
                ) : podeCancelar && cancelando !== p.id ? (
                  <button type="button" className="discreto" onClick={() => setCancelando(p.id)}>Cancelar</button>
                ) : cancelando === p.id ? (
                  <form className="linha" onSubmit={(e) => { e.preventDefault(); cancelar.mutate(p.id) }}>
                    <input aria-label="Motivo do cancelamento" placeholder="Motivo" required maxLength={300}
                           value={motivo} onChange={(e) => setMotivo(e.target.value)} />
                    <button type="submit" className="perigo" disabled={cancelar.isPending}>Confirmar</button>
                    <button type="button" className="discreto" onClick={() => setCancelando(null)}>Voltar</button>
                    <Erro erro={cancelar.error} />
                  </form>
                ) : null}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}
