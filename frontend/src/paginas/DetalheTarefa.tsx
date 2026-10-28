import { useParams, Link } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { api } from '../api/cliente'
import type { TarefaDetalhe } from '../api/tipos'
import { useSessao } from '../sessao/SessaoContext'
import { AreaTexto, Carregando, Erro, Selo } from '../componentes/ui'
import { ExecucaoTarefa } from '../componentes/ExecucaoTarefa'
import { dataHora, rotulo } from '../util/formatos'

export function DetalheTarefa() {
  const { id = '' } = useParams()
  const { pode } = useSessao()
  const cliente = useQueryClient()
  const [justificativa, setJustificativa] = useState('')
  const [cancelando, setCancelando] = useState(false)
  const { data: t, isPending, error } = useQuery({
    queryKey: ['tarefa', id],
    queryFn: () => api.get<TarefaDetalhe>(`/api/v1/tarefas/${id}`),
  })
  const cancelar = useMutation({
    mutationFn: () => api.post(`/api/v1/tarefas/${id}/cancelamento`, { justificativa }),
    onSuccess: () => {
      setCancelando(false)
      void cliente.invalidateQueries()
    },
  })

  if (isPending) return <Carregando />
  if (error) return <Erro erro={error} />
  const r = t.resumo
  const aberta = r.status === 'PENDENTE' || r.status === 'EM_ANDAMENTO'

  return (
    <>
      <div className="cabecalho-pagina">
        <h1>{r.titulo}</h1>
        {r.atrasada && <Selo cor="alerta">atrasada</Selo>}
        <Selo>{rotulo('statusTarefa', r.status)}</Selo>
      </div>
      <section className="cartao">
        <dl className="dados">
          <dt>Tipo</dt><dd>{rotulo('tipoTarefa', r.tipo)}</dd>
          <dt>Alvo</dt>
          <dd>{r.alvoTipo === 'ANIMAL' ? <Link to={`/animais/${r.alvoId}`}>{r.alvoDescricao}</Link> : r.alvoDescricao}</dd>
          <dt>Prazo</dt><dd>{dataHora(r.prazo)}</dd>
          <dt>Prioridade</dt><dd>{rotulo('prioridade', r.prioridade)}</dd>
          <dt>Responsável</dt><dd>{r.responsavelNome ?? 'não definido'}</dd>
          <dt>Criada em</dt><dd>{dataHora(t.criadaEm)}</dd>
          {t.iniciadaEm && <><dt>Iniciada em</dt><dd>{dataHora(t.iniciadaEm)}</dd></>}
          {r.finalizadaEm && <><dt>Finalizada em</dt><dd>{dataHora(r.finalizadaEm)} por {t.finalizadaPorNome ?? '—'}</dd></>}
          {t.observacaoConclusao && <><dt>Observação</dt><dd>{t.observacaoConclusao}</dd></>}
          {t.justificativa && <><dt>Justificativa</dt><dd>{t.justificativa}</dd></>}
        </dl>
        {t.descricao && <p style={{ marginTop: 12 }}>{t.descricao}</p>}
      </section>

      {t.checklist.length > 0 && (
        <section className="cartao">
          <h2>Checklist</h2>
          <ul>
            {t.checklist.map((i) => (
              <li key={i.id}>
                {i.concluido ? '☑' : '☐'} {i.descricao}{i.obrigatorio && ' (obrigatório)'}
                {i.concluidoEm && <small> — {dataHora(i.concluidoEm)}</small>}
              </li>
            ))}
          </ul>
        </section>
      )}

      <section className="cartao">
        <h2>Registros produzidos</h2>
        {t.resultados.length === 0 ? <p className="vazio">Nenhum registro técnico vinculado.</p> : (
          <ul>{t.resultados.map((x) => <li key={x.id}>{rotulo('tipoTarefa', x.tipo)} em {dataHora(x.vinculadoEm)}</li>)}</ul>
        )}
      </section>

      {aberta && (
        <section className="cartao">
          <h2>Execução</h2>
          <ExecucaoTarefa tarefa={r} />
          {pode('TAREFA_GERIR') && (
            <div className="painel">
              {!cancelando ? (
                <button type="button" className="perigo" onClick={() => setCancelando(true)}>Cancelar tarefa</button>
              ) : (
                <form className="grade" onSubmit={(e) => { e.preventDefault(); cancelar.mutate() }}>
                  <AreaTexto rotulo="Justificativa do cancelamento" required maxLength={500} value={justificativa}
                             onChange={(e) => setJustificativa(e.target.value)} />
                  <Erro erro={cancelar.error} />
                  <div className="linha">
                    <button className="perigo" type="submit" disabled={cancelar.isPending}>Confirmar cancelamento</button>
                    <button type="button" onClick={() => setCancelando(false)}>Voltar</button>
                  </div>
                </form>
              )}
            </div>
          )}
        </section>
      )}
    </>
  )
}
