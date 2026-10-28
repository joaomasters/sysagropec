import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { api } from '../api/cliente'
import type { RotinasDoDia } from '../api/tipos'
import { usePropriedade, useSessao } from '../sessao/SessaoContext'
import { Carregando, Erro, Vazio } from '../componentes/ui'
import { CartaoTarefa } from '../componentes/CartaoTarefa'
import { data } from '../util/formatos'

/** Tela do operador: o que fazer hoje (incluindo pendências atrasadas) e o que já foi finalizado hoje. */
export function Rotinas() {
  const propriedade = usePropriedade()
  const { pode } = useSessao()
  const { data: r, isPending, error } = useQuery({
    queryKey: ['rotinas', propriedade.id],
    queryFn: () => api.get<RotinasDoDia>(`/api/v1/propriedades/${propriedade.id}/rotinas-de-hoje`),
    refetchInterval: 60_000,
  })

  if (isPending) return <Carregando />
  if (error) return <Erro erro={error} />

  return (
    <>
      <div className="cabecalho-pagina">
        <h1>Rotinas de hoje</h1>
        <span className="selo">{data(r.data)}</span>
        {pode('TAREFA_GERIR') && <Link className="botao" to="/tarefas?nova=1">Nova tarefa</Link>}
      </div>
      <section aria-labelledby="abertas">
        <h2 id="abertas">A fazer ({r.abertas.length})</h2>
        {r.abertas.length === 0 ? (
          <div className="cartao"><Vazio titulo="Nada pendente para hoje" /></div>
        ) : (
          <ul className="lista-tarefas">{r.abertas.map((t) => <CartaoTarefa key={t.id} tarefa={t} />)}</ul>
        )}
      </section>
      <section aria-labelledby="finalizadas" style={{ marginTop: 24 }}>
        <h2 id="finalizadas">Finalizadas hoje ({r.finalizadasHoje.length})</h2>
        {r.finalizadasHoje.length === 0 ? (
          <p className="vazio">Nenhuma tarefa finalizada hoje.</p>
        ) : (
          <ul className="lista-tarefas">{r.finalizadasHoje.map((t) => <CartaoTarefa key={t.id} tarefa={t} comAcoes={false} />)}</ul>
        )}
      </section>
    </>
  )
}
