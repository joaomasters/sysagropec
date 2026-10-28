import { useState, type FormEvent } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from '../api/cliente'
import type { UsuarioResumo } from '../api/tipos'
import { useSessao } from '../sessao/SessaoContext'
import { Campo, Carregando, Erro, Mensagem, Selecao, Selo, Vazio } from '../componentes/ui'
import { opcoes, rotulo } from '../util/formatos'

export function Usuarios() {
  const { sessao, propriedades } = useSessao()
  const cliente = useQueryClient()
  const usuarios = useQuery({
    queryKey: ['usuarios'],
    queryFn: () => api.get<UsuarioResumo[]>('/api/v1/usuarios'),
    enabled: !!sessao?.administrador,
  })
  const atualizar = () => void cliente.invalidateQueries({ queryKey: ['usuarios'] })

  const conceder = useMutation({
    mutationFn: (v: { usuarioId: string; propriedadeId: string; perfil: string }) =>
      api.put(`/api/v1/usuarios/${v.usuarioId}/acessos/${v.propriedadeId}`, { perfil: v.perfil }),
    onSuccess: atualizar,
  })
  const revogar = useMutation({
    mutationFn: (v: { usuarioId: string; propriedadeId: string }) =>
      api.delete(`/api/v1/usuarios/${v.usuarioId}/acessos/${v.propriedadeId}`),
    onSuccess: atualizar,
  })
  const desativar = useMutation({
    mutationFn: (id: string) => api.post(`/api/v1/usuarios/${id}/desativacao`),
    onSuccess: atualizar,
  })

  if (!sessao?.administrador) return <Vazio titulo="Área restrita ao administrador da organização" />
  const nomePropriedade = (id: string) => propriedades.find((p) => p.id === id)?.nome ?? 'propriedade inativa'

  return (
    <>
      <div className="cabecalho-pagina"><h1>Usuários e acessos</h1></div>
      <NovoUsuario aoCriar={atualizar} />
      <Erro erro={conceder.error ?? revogar.error ?? desativar.error} />
      {usuarios.isPending ? <Carregando /> : usuarios.error ? <Erro erro={usuarios.error} /> : (
        <div className="grade">
          {usuarios.data.map((u) => (
            <section key={u.id} className="cartao">
              <div className="linha" style={{ alignItems: 'center' }}>
                <h2 style={{ flex: 1, margin: 0 }}>{u.nome} <small>· {u.email}</small></h2>
                {u.administrador && <Selo cor="verde">administrador</Selo>}
                {u.aguardandoAtivacao && <Selo cor="info">convite pendente</Selo>}
                {!u.ativo && <Selo cor="erro">inativo</Selo>}
                {u.ativo && u.id !== sessao.usuarioId && (
                  <button type="button" className="perigo"
                          onClick={() => { if (confirm(`Desativar ${u.nome}? O acesso é encerrado imediatamente.`)) desativar.mutate(u.id) }}>
                    Desativar
                  </button>
                )}
              </div>
              {u.administrador ? (
                <p>Acesso a todas as propriedades da organização.</p>
              ) : (
                <>
                  {u.acessos.length === 0 ? <p className="vazio">Sem acesso a propriedades.</p> : (
                    <table>
                      <tbody>
                        {u.acessos.map((a) => (
                          <tr key={a.propriedadeId}>
                            <td>{nomePropriedade(a.propriedadeId)}</td>
                            <td>{rotulo('perfil', a.perfil)}</td>
                            <td><button type="button" className="discreto"
                                        onClick={() => revogar.mutate({ usuarioId: u.id, propriedadeId: a.propriedadeId })}>Revogar</button></td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  )}
                  {u.ativo && <ConcederAcesso aoConceder={(propriedadeId, perfil) => conceder.mutate({ usuarioId: u.id, propriedadeId, perfil })} />}
                </>
              )}
            </section>
          ))}
        </div>
      )}
    </>
  )
}

function ConcederAcesso({ aoConceder }: { aoConceder: (propriedadeId: string, perfil: string) => void }) {
  const { propriedades } = useSessao()
  const [propriedadeId, setPropriedadeId] = useState('')
  const [perfil, setPerfil] = useState('OPERADOR')
  return (
    <form className="linha painel" onSubmit={(e) => { e.preventDefault(); if (propriedadeId) aoConceder(propriedadeId, perfil) }}>
      <Selecao rotulo="Propriedade" value={propriedadeId} vazio="Selecione" onChange={(e) => setPropriedadeId(e.target.value)}
               opcoes={propriedades.map((p) => ({ valor: p.id, rotulo: p.nome }))} />
      <Selecao rotulo="Perfil" value={perfil} onChange={(e) => setPerfil(e.target.value)} opcoes={opcoes('perfil')} />
      <button type="submit">Conceder / alterar acesso</button>
    </form>
  )
}

function NovoUsuario({ aoCriar }: { aoCriar: () => void }) {
  const [nome, setNome] = useState('')
  const [email, setEmail] = useState('')
  const [administrador, setAdministrador] = useState(false)
  const criar = useMutation({
    mutationFn: () => api.post('/api/v1/usuarios', { nome, email, administrador }),
    onSuccess: () => {
      setNome('')
      setEmail('')
      setAdministrador(false)
      aoCriar()
    },
  })
  function enviar(e: FormEvent) {
    e.preventDefault()
    criar.mutate()
  }
  return (
    <form className="cartao grade" onSubmit={enviar}>
      <h2>Convidar usuário</h2>
      {criar.isSuccess && <Mensagem tipo="sucesso">Convite enviado por e-mail. O link vale por 72 horas.</Mensagem>}
      <Erro erro={criar.error} />
      <div className="grade grade-4">
        <Campo rotulo="Nome" required maxLength={120} value={nome} onChange={(e) => setNome(e.target.value)} />
        <Campo rotulo="E-mail" type="email" required value={email} onChange={(e) => setEmail(e.target.value)} />
        <label className="marcar">
          <input type="checkbox" checked={administrador} onChange={(e) => setAdministrador(e.target.checked)} />
          Administrador da organização
        </label>
      </div>
      <div><button className="primario" type="submit" disabled={criar.isPending}>Enviar convite</button></div>
    </form>
  )
}
