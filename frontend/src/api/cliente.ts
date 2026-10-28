/**
 * Cliente HTTP da API. Sessão por cookie HttpOnly (mesma origem) e proteção CSRF:
 * o backend emite o cookie XSRF-TOKEN e o cliente o devolve no cabeçalho X-XSRF-TOKEN.
 */

export interface CampoInvalido {
  campo: string
  mensagem: string
}

export class ErroApi extends Error {
  readonly status: number
  readonly titulo: string
  readonly campos: CampoInvalido[]

  constructor(status: number, titulo: string, mensagem: string, campos: CampoInvalido[] = []) {
    super(mensagem)
    this.status = status
    this.titulo = titulo
    this.campos = campos
  }
}

type OuvinteSessao = () => void
const ouvintesSessaoExpirada = new Set<OuvinteSessao>()

export function aoExpirarSessao(ouvinte: OuvinteSessao): () => void {
  ouvintesSessaoExpirada.add(ouvinte)
  return () => ouvintesSessaoExpirada.delete(ouvinte)
}

function lerCookie(nome: string): string | null {
  const item = document.cookie.split('; ').find((c) => c.startsWith(nome + '='))
  return item ? decodeURIComponent(item.substring(nome.length + 1)) : null
}

async function garantirCsrf(): Promise<string | null> {
  let token = lerCookie('XSRF-TOKEN')
  if (!token) {
    await fetch('/api/v1/auth/csrf', { credentials: 'same-origin' })
    token = lerCookie('XSRF-TOKEN')
  }
  return token
}

interface Opcoes {
  chaveIdempotencia?: string
  /** Não notifica expiração de sessão (ex.: tentativa de login). */
  ignorar401?: boolean
}

export async function requisitar<T>(metodo: string, url: string, corpo?: unknown, opcoes: Opcoes = {}): Promise<T> {
  const cabecalhos: Record<string, string> = { Accept: 'application/json' }
  if (corpo !== undefined) {
    cabecalhos['Content-Type'] = 'application/json'
  }
  if (metodo !== 'GET') {
    const token = await garantirCsrf()
    if (token) {
      cabecalhos['X-XSRF-TOKEN'] = token
    }
  }
  if (opcoes.chaveIdempotencia) {
    cabecalhos['Idempotency-Key'] = opcoes.chaveIdempotencia
  }

  let resposta: Response
  try {
    resposta = await fetch(url, {
      method: metodo,
      headers: cabecalhos,
      credentials: 'same-origin',
      body: corpo === undefined ? undefined : JSON.stringify(corpo),
    })
  } catch {
    throw new ErroApi(0, 'Sem conexão', 'Não foi possível contactar o servidor. Verifique a conexão e tente novamente.')
  }

  if (resposta.status === 401 && !opcoes.ignorar401) {
    ouvintesSessaoExpirada.forEach((o) => o())
  }
  if (!resposta.ok) {
    throw await erroDaResposta(resposta)
  }
  if (resposta.status === 204 || resposta.headers.get('Content-Length') === '0') {
    return undefined as T
  }
  const texto = await resposta.text()
  return (texto ? JSON.parse(texto) : undefined) as T
}

async function erroDaResposta(resposta: Response): Promise<ErroApi> {
  try {
    const p = await resposta.json()
    return new ErroApi(resposta.status, p.title ?? 'Erro', p.detail ?? 'Não foi possível concluir a operação.', p.campos ?? [])
  } catch {
    const mensagens: Record<number, string> = {
      401: 'Sua sessão expirou. Entre novamente.',
      403: 'Você não tem permissão para esta operação.',
      404: 'Registro não encontrado.',
    }
    return new ErroApi(resposta.status, 'Erro', mensagens[resposta.status] ?? 'Erro inesperado no servidor. Tente novamente.')
  }
}

export const api = {
  get: <T>(url: string) => requisitar<T>('GET', url),
  post: <T>(url: string, corpo?: unknown, opcoes?: Opcoes) => requisitar<T>('POST', url, corpo ?? {}, opcoes),
  put: <T>(url: string, corpo?: unknown) => requisitar<T>('PUT', url, corpo ?? {}),
  delete: <T>(url: string) => requisitar<T>('DELETE', url),
}

export function mensagemDeErro(erro: unknown): string {
  if (erro instanceof ErroApi) {
    if (erro.campos.length > 0) {
      return erro.message + ': ' + erro.campos.map((c) => `${c.campo} (${c.mensagem})`).join(', ')
    }
    return erro.message
  }
  return 'Erro inesperado. Tente novamente.'
}

export function querystring(parametros: Record<string, string | number | boolean | undefined | null | string[]>): string {
  const q = new URLSearchParams()
  Object.entries(parametros).forEach(([chave, valor]) => {
    if (Array.isArray(valor)) {
      valor.forEach((v) => q.append(chave, v))
    } else if (valor !== undefined && valor !== null && valor !== '') {
      q.set(chave, String(valor))
    }
  })
  const s = q.toString()
  return s ? '?' + s : ''
}
