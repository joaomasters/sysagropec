import { useState, type FormEvent } from 'react'
import { useSearchParams } from 'react-router-dom'
import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api, querystring } from '../api/cliente'
import type { AnimalResumo, Pagina, TarefaResumo, UsuarioResumo } from '../api/tipos'
import { useInstalacoes, useLotes } from '../api/consultas'
import { usePropriedade, useSessao } from '../sessao/SessaoContext'
import { AreaTexto, Campo, Carregando, Erro, Mensagem, Paginacao, Selecao, Vazio } from '../componentes/ui'
import { CartaoTarefa } from '../componentes/CartaoTarefa'
import { agoraLocal, opcoes, paraInstante } from '../util/formatos'

export function Tarefas() {
  const propriedade = usePropriedade()
  const { pode } = useSessao()
  const [parametros, setParametros] = useSearchParams()
  const status = parametros.get('status') ?? 'ABERTAS'
  const pagina = Number(parametros.get('pagina') ?? 0)
  const [mostrarNova, setMostrarNova] = useState(parametros.get('nova') === '1')

  const consulta = useQuery({
    queryKey: ['tarefas', propriedade.id, status, pagina],
    queryFn: () =>
      api.get<Pagina<TarefaResumo>>(`/api/v1/propriedades/${propriedade.id}/tarefas` + querystring({
        status: status === 'ABERTAS' || status === 'ATRASADAS' ? ['PENDENTE', 'EM_ANDAMENTO'] : status === 'TODAS' ? undefined : [status],
        apenasAtrasadas: status === 'ATRASADAS' || undefined,
        pagina, tamanho: 30,
      })),
    placeholderData: keepPreviousData,
  })

  function mudar(chave: string, valor: string) {
    const n = new URLSearchParams(parametros)
    n.set(chave, valor)
    if (chave !== 'pagina') n.delete('pagina')
    setParametros(n, { replace: true })
  }

  return (
    <>
      <div className="cabecalho-pagina">
        <h1>Tarefas</h1>
        {pode('TAREFA_GERIR') && (
          <button type="button" className="primario" onClick={() => setMostrarNova((v) => !v)}>
            {mostrarNova ? 'Fechar' : 'Nova tarefa'}
          </button>
        )}
      </div>
      {mostrarNova && pode('TAREFA_GERIR') && <NovaTarefa aoCriar={() => setMostrarNova(false)} />}
      <div className="cartao" style={{ maxWidth: 320 }}>
        <Selecao rotulo="Situação" value={status} onChange={(e) => mudar('status', e.target.value)}
                 opcoes={[
                   { valor: 'ABERTAS', rotulo: 'Abertas' }, { valor: 'ATRASADAS', rotulo: 'Atrasadas' },
                   ...opcoes('statusTarefa'), { valor: 'TODAS', rotulo: 'Todas' },
                 ]} />
      </div>
      {consulta.isPending ? <Carregando /> : consulta.error ? <Erro erro={consulta.error} /> : consulta.data.itens.length === 0 ? (
        <div className="cartao"><Vazio titulo="Nenhuma tarefa nesta situação" /></div>
      ) : (
        <>
          <ul className="lista-tarefas">{consulta.data.itens.map((t) => <CartaoTarefa key={t.id} tarefa={t} />)}</ul>
          <Paginacao pagina={consulta.data.pagina} tamanho={consulta.data.tamanho} total={consulta.data.total}
                     aoMudar={(p) => mudar('pagina', String(p))} />
        </>
      )}
    </>
  )
}

function NovaTarefa({ aoCriar }: { aoCriar: () => void }) {
  const propriedade = usePropriedade()
  const { sessao } = useSessao()
  const cliente = useQueryClient()
  const instalacoes = useInstalacoes(propriedade.id)
  const lotes = useLotes(propriedade.id)
  const usuarios = useQuery({
    queryKey: ['usuarios'],
    queryFn: () => api.get<UsuarioResumo[]>('/api/v1/usuarios'),
    enabled: !!sessao?.administrador,
  })

  const [tipo, setTipo] = useState('TRATO')
  const [titulo, setTitulo] = useState('')
  const [descricao, setDescricao] = useState('')
  const [alvoTipo, setAlvoTipo] = useState('INSTALACAO')
  const [alvoId, setAlvoId] = useState('')
  const [buscaAnimal, setBuscaAnimal] = useState('')
  const [responsavelId, setResponsavelId] = useState('')
  const [prazo, setPrazo] = useState(agoraLocal)
  const [prioridade, setPrioridade] = useState('MEDIA')
  const [itens, setItens] = useState<{ descricao: string; obrigatorio: boolean }[]>([])

  const animais = useQuery({
    queryKey: ['animais', propriedade.id, 'busca', buscaAnimal],
    queryFn: () => api.get<Pagina<AnimalResumo>>(`/api/v1/propriedades/${propriedade.id}/animais` +
      querystring({ busca: buscaAnimal, situacao: 'ATIVO', tamanho: 20 })),
    enabled: alvoTipo === 'ANIMAL' && buscaAnimal.length >= 1,
  })

  const criar = useMutation({
    mutationFn: () =>
      api.post(`/api/v1/propriedades/${propriedade.id}/tarefas`, {
        tipo, titulo, descricao: descricao || null, alvoTipo, alvoId: alvoTipo === 'PROPRIEDADE' ? propriedade.id : alvoId,
        responsavelId: responsavelId || null, prazo: paraInstante(prazo), prioridade,
        checklist: itens.filter((i) => i.descricao.trim()),
      }),
    onSuccess: () => {
      void cliente.invalidateQueries({ queryKey: ['tarefas'] })
      void cliente.invalidateQueries({ queryKey: ['rotinas'] })
      void cliente.invalidateQueries({ queryKey: ['dashboard'] })
      aoCriar()
    },
  })

  const alvos =
    alvoTipo === 'INSTALACAO' ? (instalacoes.data ?? []).map((i) => ({ valor: i.id, rotulo: i.codigo }))
      : alvoTipo === 'LOTE' ? (lotes.data ?? []).map((l) => ({ valor: l.id, rotulo: l.codigo }))
        : alvoTipo === 'ANIMAL' ? (animais.data?.itens ?? []).map((a) => ({ valor: a.id, rotulo: `${a.brinco ?? ''} (Nº ${a.numero})` }))
          : []
  const responsaveis = (usuarios.data ?? [])
    .filter((u) => u.ativo && !u.aguardandoAtivacao && (u.administrador || u.acessos.some((a) => a.propriedadeId === propriedade.id)))

  function enviar(e: FormEvent) {
    e.preventDefault()
    criar.mutate()
  }

  return (
    <form className="cartao grade" onSubmit={enviar}>
      <h2>Nova tarefa</h2>
      <div className="grade grade-4">
        <Selecao rotulo="Tipo" value={tipo} onChange={(e) => setTipo(e.target.value)} opcoes={opcoes('tipoTarefa')} />
        <Campo rotulo="Título" required maxLength={120} value={titulo} onChange={(e) => setTitulo(e.target.value)} />
        <Campo rotulo="Prazo" type="datetime-local" required value={prazo} onChange={(e) => setPrazo(e.target.value)} />
        <Selecao rotulo="Prioridade" value={prioridade} onChange={(e) => setPrioridade(e.target.value)} opcoes={opcoes('prioridade')} />
        <Selecao rotulo="Onde / em quê" value={alvoTipo} onChange={(e) => { setAlvoTipo(e.target.value); setAlvoId('') }}
                 opcoes={opcoes('alvoTipo')} />
        {alvoTipo === 'ANIMAL' && (
          <Campo rotulo="Buscar animal" placeholder="Brinco ou nº" value={buscaAnimal} onChange={(e) => setBuscaAnimal(e.target.value)} />
        )}
        {alvoTipo !== 'PROPRIEDADE' && (
          <Selecao rotulo={alvoTipo === 'ANIMAL' ? 'Animal' : alvoTipo === 'LOTE' ? 'Lote' : 'Instalação'} required
                   value={alvoId} vazio="Selecione" onChange={(e) => setAlvoId(e.target.value)} opcoes={alvos} />
        )}
        {sessao?.administrador && (
          <Selecao rotulo="Responsável" value={responsavelId} vazio="Sem responsável definido"
                   onChange={(e) => setResponsavelId(e.target.value)}
                   opcoes={responsaveis.map((u) => ({ valor: u.id, rotulo: u.nome }))} />
        )}
      </div>
      {tipo === 'PESAGEM' && (
        <Mensagem tipo="info">Tarefas de pesagem são concluídas registrando a pesagem pela própria tarefa.</Mensagem>
      )}
      <AreaTexto rotulo="Descrição" maxLength={1000} value={descricao} onChange={(e) => setDescricao(e.target.value)} />
      <fieldset style={{ border: 0, padding: 0, margin: 0 }} className="grade">
        <legend><strong>Checklist</strong></legend>
        {itens.map((item, i) => (
          <div key={i} className="linha">
            <input aria-label={`Item ${i + 1} do checklist`} style={{ flex: 1 }} maxLength={200} value={item.descricao}
                   onChange={(e) => setItens((xs) => xs.map((x, j) => (j === i ? { ...x, descricao: e.target.value } : x)))} />
            <label className="marcar">
              <input type="checkbox" checked={item.obrigatorio}
                     onChange={(e) => setItens((xs) => xs.map((x, j) => (j === i ? { ...x, obrigatorio: e.target.checked } : x)))} />
              Obrigatório
            </label>
            <button type="button" className="discreto" onClick={() => setItens((xs) => xs.filter((_, j) => j !== i))}>Remover</button>
          </div>
        ))}
        <div><button type="button" onClick={() => setItens((xs) => [...xs, { descricao: '', obrigatorio: true }])}>Adicionar item</button></div>
      </fieldset>
      <Erro erro={criar.error} />
      <div><button className="primario" type="submit" disabled={criar.isPending}>Criar tarefa</button></div>
    </form>
  )
}
