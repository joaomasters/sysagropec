import { useState, type FormEvent } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { api } from '../api/cliente'
import { useInstalacoes, useLotes } from '../api/consultas'
import { usePropriedade, useSessao } from '../sessao/SessaoContext'
import { Campo, Erro, Selecao, Vazio } from '../componentes/ui'
import { agoraLocal, paraInstante } from '../util/formatos'

export function Movimentar() {
  const propriedade = usePropriedade()
  const { pode } = useSessao()
  const navegar = useNavigate()
  const cliente = useQueryClient()
  const animalIds: string[] = (useLocation().state as { animalIds?: string[] } | null)?.animalIds ?? []
  const lotes = useLotes(propriedade.id)
  const instalacoes = useInstalacoes(propriedade.id)

  const [lote, setLote] = useState('')
  const [instalacao, setInstalacao] = useState('')
  const [motivo, setMotivo] = useState('')
  const [ocorridoEm, setOcorridoEm] = useState(agoraLocal)

  const mover = useMutation({
    mutationFn: () =>
      api.post(`/api/v1/propriedades/${propriedade.id}/movimentacoes`, {
        animalIds,
        loteDestinoId: lote && lote !== 'retirar' ? lote : null,
        retirarDoLote: lote === 'retirar',
        instalacaoDestinoId: instalacao || null,
        motivo,
        ocorridoEm: paraInstante(ocorridoEm),
      }),
    onSuccess: () => {
      void cliente.invalidateQueries()
      navegar('/animais')
    },
  })

  if (!pode('ANIMAL_MOVIMENTAR')) return <Vazio titulo="Seu perfil não permite movimentar animais" />
  if (animalIds.length === 0) {
    return <Vazio titulo="Nenhum animal selecionado"><Link to="/animais">Selecione os animais na lista</Link></Vazio>
  }

  function enviar(e: FormEvent) {
    e.preventDefault()
    mover.mutate()
  }

  return (
    <form className="cartao grade" onSubmit={enviar}>
      <h1>Movimentar {animalIds.length} animal(is)</h1>
      <p>Informe o novo lote, a nova instalação ou ambos. O histórico anterior é preservado.</p>
      <div className="grade grade-2">
        <Selecao rotulo="Lote de destino" value={lote} vazio="Manter lote atual" onChange={(e) => setLote(e.target.value)}
                 opcoes={[{ valor: 'retirar', rotulo: 'Retirar do lote' }, ...(lotes.data ?? []).map((l) => ({ valor: l.id, rotulo: l.codigo }))]} />
        <Selecao rotulo="Instalação de destino" value={instalacao} vazio="Manter instalação atual"
                 onChange={(e) => setInstalacao(e.target.value)}
                 opcoes={(instalacoes.data ?? []).filter((i) => i.ativa).map((i) => ({ valor: i.id, rotulo: i.codigo }))} />
        <Campo rotulo="Data e hora" type="datetime-local" required value={ocorridoEm} onChange={(e) => setOcorridoEm(e.target.value)} />
        <Campo rotulo="Motivo" required maxLength={300} value={motivo} onChange={(e) => setMotivo(e.target.value)} />
      </div>
      <Erro erro={mover.error} />
      <div className="linha">
        <button className="primario" type="submit" disabled={mover.isPending || (!lote && !instalacao)}>Confirmar movimentação</button>
        <Link className="botao" to="/animais">Cancelar</Link>
      </div>
    </form>
  )
}
