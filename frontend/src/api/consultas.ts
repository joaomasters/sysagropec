import { useQuery } from '@tanstack/react-query'
import { api } from './cliente'
import type { Instalacao, Lote } from './tipos'

export function useInstalacoes(propriedadeId: string) {
  return useQuery({
    queryKey: ['instalacoes', propriedadeId],
    queryFn: () => api.get<Instalacao[]>(`/api/v1/propriedades/${propriedadeId}/instalacoes`),
  })
}

export function useLotes(propriedadeId: string, incluirEncerrados = false) {
  return useQuery({
    queryKey: ['lotes', propriedadeId, incluirEncerrados],
    queryFn: () =>
      api.get<Lote[]>(`/api/v1/propriedades/${propriedadeId}/lotes?incluirEncerrados=${incluirEncerrados}`),
  })
}

/** Mapa id → código, para exibir a localização dos animais. */
export function useCodigosInstalacao(propriedadeId: string): Record<string, string> {
  const { data } = useInstalacoes(propriedadeId)
  return Object.fromEntries((data ?? []).map((i) => [i.id, i.codigo]))
}
