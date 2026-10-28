import { useState, type FormEvent } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { api } from '../api/cliente'
import type { Instalacao, Propriedade } from '../api/tipos'
import { useInstalacoes } from '../api/consultas'
import { useSessao, usePropriedade } from '../sessao/SessaoContext'
import { Campo, Carregando, Erro, Mensagem, Selecao, Vazio } from '../componentes/ui'
import { opcoes, rotulo } from '../util/formatos'

export function Estrutura() {
  const { sessao, propriedades } = useSessao()
  const propriedade = usePropriedade()
  return (
    <>
      <div className="cabecalho-pagina"><h1>Estrutura</h1></div>
      <Instalacoes propriedade={propriedade} />
      <section className="cartao">
        <h2>Propriedades da organização</h2>
        <table>
          <thead><tr><th>Nome</th><th>Município/UF</th><th>Fuso horário</th></tr></thead>
          <tbody>
            {propriedades.map((p) => (
              <tr key={p.id}>
                <td>{p.nome}</td>
                <td>{[p.municipio, p.uf].filter(Boolean).join(' / ') || '—'}</td>
                <td>{p.fusoHorario}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </section>
      {sessao?.administrador && <NovaPropriedade />}
    </>
  )
}

function Instalacoes({ propriedade }: { propriedade: Propriedade }) {
  const { pode } = useSessao()
  const cliente = useQueryClient()
  const { data, isPending, error } = useInstalacoes(propriedade.id)
  const [tipo, setTipo] = useState('CURRAL')
  const [codigo, setCodigo] = useState('')
  const [capacidade, setCapacidade] = useState('')
  const [descricao, setDescricao] = useState('')

  const criar = useMutation({
    mutationFn: () =>
      api.post<Instalacao>(`/api/v1/propriedades/${propriedade.id}/instalacoes`, {
        tipo, codigo, descricao: descricao || null, capacidadeCabecas: capacidade ? Number(capacidade) : null,
      }),
    onSuccess: () => {
      setCodigo('')
      setCapacidade('')
      setDescricao('')
      void cliente.invalidateQueries({ queryKey: ['instalacoes', propriedade.id] })
      void cliente.invalidateQueries({ queryKey: ['dashboard'] })
    },
  })

  function enviar(e: FormEvent) {
    e.preventDefault()
    criar.mutate()
  }

  return (
    <section className="cartao">
      <h2>Currais e instalações — {propriedade.nome}</h2>
      {isPending ? <Carregando /> : error ? <Erro erro={error} /> : data.length === 0 ? (
        <Vazio titulo="Nenhuma instalação cadastrada" />
      ) : (
        <div className="tabela-rolagem">
          <table>
            <thead><tr><th>Código</th><th>Tipo</th><th className="num">Capacidade (cab.)</th><th>Descrição</th></tr></thead>
            <tbody>
              {data.map((i) => (
                <tr key={i.id}>
                  <td>{i.codigo}</td>
                  <td>{rotulo('tipoInstalacao', i.tipo)}</td>
                  <td className="num">{i.capacidadeCabecas ?? 'não definida'}</td>
                  <td>{i.descricao ?? '—'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
      {pode('INSTALACAO_GERIR') && (
        <form onSubmit={enviar} className="painel grade">
          <h3>Nova instalação</h3>
          <Erro erro={criar.error} />
          {criar.isSuccess && <Mensagem tipo="sucesso">Instalação cadastrada.</Mensagem>}
          <div className="grade grade-4">
            <Selecao rotulo="Tipo" value={tipo} onChange={(e) => setTipo(e.target.value)} opcoes={opcoes('tipoInstalacao')} />
            <Campo rotulo="Código" required maxLength={30} value={codigo} onChange={(e) => setCodigo(e.target.value)} />
            <Campo rotulo="Capacidade (cabeças)" type="number" min={1} value={capacidade}
                   onChange={(e) => setCapacidade(e.target.value)} ajuda="Deixe vazio se não houver limite definido." />
            <Campo rotulo="Descrição" maxLength={200} value={descricao} onChange={(e) => setDescricao(e.target.value)} />
          </div>
          <div><button className="primario" type="submit" disabled={criar.isPending}>Cadastrar instalação</button></div>
        </form>
      )}
    </section>
  )
}

function NovaPropriedade() {
  const { recarregar, selecionarPropriedade } = useSessao()
  const [nome, setNome] = useState('')
  const [municipio, setMunicipio] = useState('')
  const [uf, setUf] = useState('')
  const [fuso, setFuso] = useState('America/Sao_Paulo')

  const criar = useMutation({
    mutationFn: () =>
      api.post<Propriedade>('/api/v1/propriedades', { nome, municipio: municipio || null, uf: uf || null, fusoHorario: fuso }),
    onSuccess: async (p) => {
      setNome('')
      setMunicipio('')
      setUf('')
      await recarregar()
      selecionarPropriedade(p.id)
    },
  })

  return (
    <form className="cartao grade" onSubmit={(e) => { e.preventDefault(); criar.mutate() }}>
      <h2>Nova propriedade</h2>
      <Erro erro={criar.error} />
      <div className="grade grade-4">
        <Campo rotulo="Nome" required maxLength={120} value={nome} onChange={(e) => setNome(e.target.value)} />
        <Campo rotulo="Município" maxLength={120} value={municipio} onChange={(e) => setMunicipio(e.target.value)} />
        <Campo rotulo="UF" maxLength={2} value={uf} onChange={(e) => setUf(e.target.value.toUpperCase())} />
        <Selecao rotulo="Fuso horário" value={fuso} onChange={(e) => setFuso(e.target.value)}
                 opcoes={['America/Sao_Paulo', 'America/Cuiaba', 'America/Campo_Grande', 'America/Porto_Velho',
                   'America/Manaus', 'America/Rio_Branco', 'America/Belem', 'America/Fortaleza', 'America/Recife',
                   'America/Bahia', 'America/Araguaina'].map((z) => ({ valor: z, rotulo: z }))} />
      </div>
      <div><button className="primario" type="submit" disabled={criar.isPending}>Cadastrar propriedade</button></div>
    </form>
  )
}

export function PrimeiraPropriedade() {
  const { sessao } = useSessao()
  if (!sessao?.administrador) {
    return (
      <Vazio titulo="Você ainda não tem acesso a nenhuma propriedade">
        Peça ao administrador da organização para liberar seu acesso.
      </Vazio>
    )
  }
  return (
    <>
      <div className="cabecalho-pagina"><h1>Bem-vindo ao SysAgropec</h1></div>
      <Mensagem tipo="info">Para começar, cadastre a primeira propriedade da organização {sessao.organizacaoNome}.</Mensagem>
      <NovaPropriedade />
    </>
  )
}
