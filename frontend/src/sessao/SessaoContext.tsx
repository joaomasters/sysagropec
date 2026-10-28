import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { aoExpirarSessao, api, ErroApi } from '../api/cliente'
import type { Permissao, Propriedade, Sessao } from '../api/tipos'

interface ContextoSessao {
  sessao: Sessao | null
  carregando: boolean
  propriedades: Propriedade[]
  propriedade: Propriedade | null
  selecionarPropriedade: (id: string) => void
  pode: (permissao: Permissao) => boolean
  entrar: (email: string, senha: string) => Promise<void>
  sair: () => Promise<void>
  recarregar: () => Promise<void>
}

const Contexto = createContext<ContextoSessao | null>(null)
const CHAVE_PROPRIEDADE = 'sysagropec.propriedade'

function lerPreferencia(): string | null {
  try {
    return localStorage.getItem(CHAVE_PROPRIEDADE)
  } catch {
    return null
  }
}

function gravarPreferencia(id: string) {
  try {
    localStorage.setItem(CHAVE_PROPRIEDADE, id)
  } catch {
    // Preferência apenas de conveniência.
  }
}

export function SessaoProvider({ children }: { children: ReactNode }) {
  const cliente = useQueryClient()
  const [propriedadeId, setPropriedadeId] = useState<string | null>(lerPreferencia)

  const consultaSessao = useQuery({
    queryKey: ['sessao'],
    queryFn: async () => {
      try {
        return await api.get<Sessao>('/api/v1/sessao')
      } catch (e) {
        if (e instanceof ErroApi && e.status === 401) return null
        throw e
      }
    },
    staleTime: 60_000,
  })
  const sessao = consultaSessao.data ?? null

  const consultaPropriedades = useQuery({
    queryKey: ['propriedades'],
    queryFn: () => api.get<Propriedade[]>('/api/v1/propriedades'),
    enabled: sessao !== null,
  })
  const propriedades = useMemo(() => consultaPropriedades.data ?? [], [consultaPropriedades.data])

  useEffect(() => aoExpirarSessao(() => cliente.setQueryData(['sessao'], null)), [cliente])

  const propriedade = useMemo(
    () => propriedades.find((p) => p.id === propriedadeId) ?? propriedades[0] ?? null,
    [propriedades, propriedadeId],
  )

  const selecionarPropriedade = useCallback((id: string) => {
    setPropriedadeId(id)
    gravarPreferencia(id)
  }, [])

  const pode = useCallback(
    (permissao: Permissao) =>
      !!sessao && !!propriedade && (sessao.permissoesPorPropriedade[propriedade.id] ?? []).includes(permissao),
    [sessao, propriedade],
  )

  /**
   * Remove do cache os dados do usuário anterior, preservando a consulta da sessão:
   * removê-la desconectaria o observador ativo e a tela não veria a nova sessão.
   */
  const descartarDadosDoUsuario = useCallback(() => {
    cliente.removeQueries({ predicate: (q) => q.queryKey[0] !== 'sessao' })
  }, [cliente])

  const recarregar = useCallback(async () => {
    await cliente.invalidateQueries({ queryKey: ['sessao'] })
    await cliente.invalidateQueries({ queryKey: ['propriedades'] })
  }, [cliente])

  const entrar = useCallback(
    async (email: string, senha: string) => {
      const nova = await api.post<Sessao>('/api/v1/auth/login', { email, senha }, { ignorar401: true })
      descartarDadosDoUsuario()
      cliente.setQueryData(['sessao'], nova)
    },
    [cliente, descartarDadosDoUsuario],
  )

  const sair = useCallback(async () => {
    try {
      await api.post('/api/v1/auth/logout')
    } finally {
      descartarDadosDoUsuario()
      cliente.setQueryData(['sessao'], null)
    }
  }, [cliente, descartarDadosDoUsuario])

  const valor = useMemo<ContextoSessao>(
    () => ({
      sessao,
      carregando: consultaSessao.isPending || (sessao !== null && consultaPropriedades.isPending),
      propriedades,
      propriedade,
      selecionarPropriedade,
      pode,
      entrar,
      sair,
      recarregar,
    }),
    [sessao, consultaSessao.isPending, consultaPropriedades.isPending, propriedades, propriedade, selecionarPropriedade, pode, entrar, sair, recarregar],
  )

  return <Contexto.Provider value={valor}>{children}</Contexto.Provider>
}

// eslint-disable-next-line react-refresh/only-export-components
export function useSessao(): ContextoSessao {
  const c = useContext(Contexto)
  if (!c) throw new Error('useSessao fora de SessaoProvider')
  return c
}

/** Propriedade selecionada; as páginas internas só são exibidas quando existe uma. */
// eslint-disable-next-line react-refresh/only-export-components
export function usePropriedade(): Propriedade {
  const { propriedade } = useSessao()
  if (!propriedade) throw new Error('Nenhuma propriedade selecionada')
  return propriedade
}
