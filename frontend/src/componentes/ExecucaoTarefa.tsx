import { useState, type FormEvent } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from '../api/cliente'
import type { AnimalResumo, Pagina, TarefaDetalhe, TarefaResumo } from '../api/tipos'
import { useSessao } from '../sessao/SessaoContext'
import { AreaTexto, Carregando, Erro, Mensagem, Selecao } from './ui'
import { RegistroPesagem } from './RegistroPesagem'

type Modo = 'concluir' | 'impedir' | 'pesar' | null

/**
 * Ações de campo sobre uma tarefa aberta. Tarefas de pesagem são concluídas registrando a pesagem:
 * o próprio registro técnico comprova a execução (nenhuma pesagem é criada na conclusão).
 */
export function ExecucaoTarefa({ tarefa }: { tarefa: TarefaResumo }) {
  const { pode } = useSessao()
  const cliente = useQueryClient()
  const [modo, setModo] = useState<Modo>(null)
  const [texto, setTexto] = useState('')
  const [marcados, setMarcados] = useState<Set<string>>(new Set())
  const [aviso, setAviso] = useState<string | null>(null)

  const detalhe = useQuery({
    queryKey: ['tarefa', tarefa.id],
    queryFn: () => api.get<TarefaDetalhe>(`/api/v1/tarefas/${tarefa.id}`),
    enabled: modo === 'concluir',
  })

  const atualizar = () => {
    void cliente.invalidateQueries({ queryKey: ['rotinas'] })
    void cliente.invalidateQueries({ queryKey: ['tarefas'] })
    void cliente.invalidateQueries({ queryKey: ['tarefa', tarefa.id] })
    void cliente.invalidateQueries({ queryKey: ['dashboard'] })
  }
  const acao = useMutation({
    mutationFn: ({ rota, corpo }: { rota: string; corpo?: unknown }) => api.post(`/api/v1/tarefas/${tarefa.id}/${rota}`, corpo),
    onSuccess: () => {
      setModo(null)
      setTexto('')
      atualizar()
    },
  })

  if (!pode('TAREFA_EXECUTAR') || !['PENDENTE', 'EM_ANDAMENTO'].includes(tarefa.status)) return null
  const ehPesagem = tarefa.tipo === 'PESAGEM'

  function concluir(e: FormEvent) {
    e.preventDefault()
    acao.mutate({ rota: 'conclusao', corpo: { observacao: texto || null, itensConcluidos: [...marcados] } })
  }

  function impedir(e: FormEvent) {
    e.preventDefault()
    acao.mutate({ rota: 'impedimento', corpo: { justificativa: texto } })
  }

  return (
    <div>
      {aviso && <Mensagem tipo="sucesso">{aviso}</Mensagem>}
      <div className="linha">
        {tarefa.status === 'PENDENTE' && (
          <button type="button" onClick={() => acao.mutate({ rota: 'inicio' })} disabled={acao.isPending}>Iniciar</button>
        )}
        {ehPesagem && (
          <button type="button" className="primario" onClick={() => setModo(modo === 'pesar' ? null : 'pesar')}>
            Registrar pesagem
          </button>
        )}
        {(!ehPesagem || tarefa.alvoTipo !== 'ANIMAL') && (
          <button type="button" className={ehPesagem ? '' : 'primario'} onClick={() => setModo(modo === 'concluir' ? null : 'concluir')}>
            Concluir
          </button>
        )}
        <button type="button" onClick={() => setModo(modo === 'impedir' ? null : 'impedir')}>Não foi possível</button>
      </div>
      <Erro erro={acao.error} />

      {modo === 'pesar' && (
        <div className="painel">
          {tarefa.alvoTipo === 'ANIMAL' ? (
            <RegistroPesagem animalId={tarefa.alvoId} tarefaId={tarefa.id}
                             aoRegistrar={(r) => { setModo(null); setAviso(r.tarefaConcluida ? 'Pesagem registrada e tarefa concluída.' : 'Pesagem registrada.'); atualizar() }} />
          ) : tarefa.alvoTipo === 'LOTE' ? (
            <PesagemDoLote tarefa={tarefa} aoRegistrar={() => { setAviso('Pesagem registrada.'); atualizar() }} />
          ) : (
            <p>Registre as pesagens pela ficha de cada animal.</p>
          )}
        </div>
      )}

      {modo === 'concluir' && (
        <form className="painel grade" onSubmit={concluir}>
          {detalhe.isPending ? <Carregando /> : detalhe.data && detalhe.data.checklist.length > 0 && (
            <fieldset style={{ border: 0, padding: 0, margin: 0 }}>
              <legend><strong>Checklist</strong></legend>
              {detalhe.data.checklist.map((i) => (
                <label key={i.id} className="marcar" style={{ display: 'flex' }}>
                  <input type="checkbox" disabled={i.concluido} checked={i.concluido || marcados.has(i.id)}
                         onChange={() => setMarcados((s) => {
                           const n = new Set(s)
                           if (n.has(i.id)) n.delete(i.id)
                           else n.add(i.id)
                           return n
                         })} />
                  {i.descricao}{i.obrigatorio && ' *'}
                </label>
              ))}
            </fieldset>
          )}
          <AreaTexto rotulo="Observação (opcional)" maxLength={1000} value={texto} onChange={(e) => setTexto(e.target.value)} />
          <div><button className="primario" type="submit" disabled={acao.isPending}>Confirmar conclusão</button></div>
        </form>
      )}

      {modo === 'impedir' && (
        <form className="painel grade" onSubmit={impedir}>
          <AreaTexto rotulo="Justificativa" required maxLength={500} value={texto} onChange={(e) => setTexto(e.target.value)}
                     ajuda="Explique o que impediu a execução." />
          <div><button className="perigo" type="submit" disabled={acao.isPending}>Registrar impedimento</button></div>
        </form>
      )}
    </div>
  )
}

function PesagemDoLote({ tarefa, aoRegistrar }: { tarefa: TarefaResumo; aoRegistrar: () => void }) {
  const [animalId, setAnimalId] = useState('')
  const animais = useQuery({
    queryKey: ['animais', tarefa.propriedadeId, 'lote', tarefa.alvoId],
    queryFn: () => api.get<Pagina<AnimalResumo>>(
      `/api/v1/propriedades/${tarefa.propriedadeId}/animais?loteId=${tarefa.alvoId}&situacao=ATIVO&tamanho=200`),
  })
  if (animais.isPending) return <Carregando />
  return (
    <div className="grade">
      <Selecao rotulo="Animal" value={animalId} vazio="Selecione o animal" onChange={(e) => setAnimalId(e.target.value)}
               opcoes={(animais.data?.itens ?? []).map((a) => ({ valor: a.id, rotulo: a.brinco ?? `Nº ${a.numero}` }))} />
      {animalId && <RegistroPesagem key={animalId} animalId={animalId} tarefaId={tarefa.id} aoRegistrar={() => { setAnimalId(''); aoRegistrar() }} />}
      <small>Após pesar os animais, use “Concluir” para finalizar a tarefa.</small>
    </div>
  )
}
