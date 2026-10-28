import { Link } from 'react-router-dom'
import type { TarefaResumo } from '../api/tipos'
import { Selo } from './ui'
import { ExecucaoTarefa } from './ExecucaoTarefa'
import { dataHora, rotulo } from '../util/formatos'

const COR_STATUS: Record<string, 'verde' | 'alerta' | 'erro' | 'info' | undefined> = {
  PENDENTE: undefined, EM_ANDAMENTO: 'info', CONCLUIDA: 'verde', IMPEDIDA: 'erro', CANCELADA: undefined,
}

export function CartaoTarefa({ tarefa, comAcoes = true }: { tarefa: TarefaResumo; comAcoes?: boolean }) {
  return (
    <li className={`tarefa ${tarefa.atrasada ? 'atrasada' : ''}`}>
      <div className="linha" style={{ alignItems: 'center' }}>
        <Link className="titulo" to={`/tarefas/${tarefa.id}`} style={{ flex: 1 }}>{tarefa.titulo}</Link>
        {tarefa.atrasada && <Selo cor="alerta">atrasada</Selo>}
        {tarefa.prioridade === 'ALTA' && <Selo cor="erro">prioridade alta</Selo>}
        <Selo cor={COR_STATUS[tarefa.status]}>{rotulo('statusTarefa', tarefa.status)}</Selo>
      </div>
      <div className="meta">
        <span>{rotulo('tipoTarefa', tarefa.tipo)}</span>
        <span>{tarefa.alvoDescricao ?? rotulo('alvoTipo', tarefa.alvoTipo)}</span>
        <span>Prazo: {dataHora(tarefa.prazo)}</span>
        {tarefa.responsavelNome && <span>Responsável: {tarefa.responsavelNome}</span>}
        {tarefa.checklistTotal > 0 && <span>Checklist: {tarefa.checklistConcluidos}/{tarefa.checklistTotal}</span>}
        {tarefa.finalizadaEm && <span>Finalizada: {dataHora(tarefa.finalizadaEm)}</span>}
      </div>
      {comAcoes && <ExecucaoTarefa tarefa={tarefa} />}
    </li>
  )
}
