import { useState, type FormEvent } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { api } from '../api/cliente'
import { Campo, Erro, Selecao } from './ui'
import { agoraLocal, novaChave, opcoes, paraInstante } from '../util/formatos'

const NATUREZA_DO_METODO: Record<string, string> = {
  BALANCA_INDIVIDUAL: 'MEDIDO',
  FITA_BARIMETRICA: 'ESTIMADO',
  VISUAL: 'ESTIMADO',
}

/** Formulário curto de pesagem, usado na ficha do animal e nas rotinas (com a tarefa de origem). */
export function RegistroPesagem({
  animalId,
  tarefaId,
  aoRegistrar,
}: {
  animalId: string
  tarefaId?: string
  aoRegistrar?: (resultado: { pesagemId: string; tarefaConcluida: boolean }) => void
}) {
  const cliente = useQueryClient()
  const [peso, setPeso] = useState('')
  const [metodo, setMetodo] = useState('BALANCA_INDIVIDUAL')
  const [natureza, setNatureza] = useState('MEDIDO')
  const [finalidade, setFinalidade] = useState('ACOMPANHAMENTO')
  const [ocorridoEm, setOcorridoEm] = useState(agoraLocal)
  const [chave, setChave] = useState(novaChave)

  const registrar = useMutation({
    mutationFn: () =>
      api.post<{ pesagemId: string; tarefaConcluida: boolean }>(`/api/v1/animais/${animalId}/pesagens`, {
        pesoKg: Number(peso.replace(',', '.')), metodo, natureza, finalidade, ocorridoEm: paraInstante(ocorridoEm),
        tarefaId: tarefaId ?? null,
      }, { chaveIdempotencia: chave }),
    onSuccess: (r) => {
      setPeso('')
      setChave(novaChave())
      void cliente.invalidateQueries({ queryKey: ['pesagens', animalId] })
      void cliente.invalidateQueries({ queryKey: ['linha-do-tempo', animalId] })
      void cliente.invalidateQueries({ queryKey: ['rotinas'] })
      void cliente.invalidateQueries({ queryKey: ['tarefa'] })
      void cliente.invalidateQueries({ queryKey: ['dashboard'] })
      aoRegistrar?.(r)
    },
  })

  function alterarMetodo(valor: string) {
    setMetodo(valor)
    if (NATUREZA_DO_METODO[valor]) setNatureza(NATUREZA_DO_METODO[valor])
  }

  function enviar(e: FormEvent) {
    e.preventDefault()
    registrar.mutate()
  }

  return (
    <form className="grade" onSubmit={enviar} aria-label="Registrar pesagem">
      <Erro erro={registrar.error} />
      <div className="grade grade-4">
        <Campo rotulo="Peso (kg)" inputMode="decimal" required pattern="\d{1,4}([.,]\d{1,2})?" value={peso}
               onChange={(e) => setPeso(e.target.value)} ajuda="Até 2 casas decimais." />
        <Selecao rotulo="Método" value={metodo} onChange={(e) => alterarMetodo(e.target.value)} opcoes={opcoes('metodo')} />
        <Selecao rotulo="Natureza" value={natureza} onChange={(e) => setNatureza(e.target.value)}
                 disabled={!!NATUREZA_DO_METODO[metodo]} opcoes={opcoes('natureza')} />
        <Selecao rotulo="Finalidade" value={finalidade} onChange={(e) => setFinalidade(e.target.value)}
                 opcoes={opcoes('finalidadePesagem')} />
        <Campo rotulo="Data e hora" type="datetime-local" required value={ocorridoEm} onChange={(e) => setOcorridoEm(e.target.value)} />
      </div>
      <div>
        <button className="primario" type="submit" disabled={registrar.isPending}>
          {registrar.isPending ? 'Registrando…' : 'Registrar pesagem'}
        </button>
      </div>
    </form>
  )
}
