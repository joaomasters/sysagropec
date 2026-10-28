import type { ReactNode, SelectHTMLAttributes, InputHTMLAttributes, TextareaHTMLAttributes } from 'react'
import { mensagemDeErro } from '../api/cliente'

export function Carregando({ texto = 'Carregando…' }: { texto?: string }) {
  return (
    <div className="carregando" role="status" aria-live="polite">
      {texto}
    </div>
  )
}

export function Erro({ erro }: { erro: unknown }) {
  if (!erro) return null
  return (
    <div className="mensagem erro" role="alert">
      {mensagemDeErro(erro)}
    </div>
  )
}

export function Mensagem({ tipo, children }: { tipo: 'sucesso' | 'info' | 'aviso' | 'erro'; children: ReactNode }) {
  return (
    <div className={`mensagem ${tipo}`} role={tipo === 'erro' ? 'alert' : 'status'}>
      {children}
    </div>
  )
}

export function Vazio({ titulo, children }: { titulo: string; children?: ReactNode }) {
  return (
    <div className="vazio">
      <strong>{titulo}</strong>
      {children}
    </div>
  )
}

interface CampoBase {
  rotulo: string
  ajuda?: string
}

export function Campo({ rotulo, ajuda, ...props }: CampoBase & InputHTMLAttributes<HTMLInputElement>) {
  return (
    <label className="campo">
      {rotulo}
      <input {...props} />
      {ajuda && <span className="ajuda">{ajuda}</span>}
    </label>
  )
}

export function Selecao({
  rotulo,
  ajuda,
  opcoes,
  vazio,
  ...props
}: CampoBase & SelectHTMLAttributes<HTMLSelectElement> & { opcoes: { valor: string; rotulo: string }[]; vazio?: string }) {
  return (
    <label className="campo">
      {rotulo}
      <select {...props}>
        {vazio !== undefined && <option value="">{vazio}</option>}
        {opcoes.map((o) => (
          <option key={o.valor} value={o.valor}>
            {o.rotulo}
          </option>
        ))}
      </select>
      {ajuda && <span className="ajuda">{ajuda}</span>}
    </label>
  )
}

export function AreaTexto({ rotulo, ajuda, ...props }: CampoBase & TextareaHTMLAttributes<HTMLTextAreaElement>) {
  return (
    <label className="campo">
      {rotulo}
      <textarea {...props} />
      {ajuda && <span className="ajuda">{ajuda}</span>}
    </label>
  )
}

export function Selo({ cor, children }: { cor?: 'verde' | 'alerta' | 'erro' | 'info'; children: ReactNode }) {
  return <span className={`selo ${cor ?? ''}`}>{children}</span>
}

export function Paginacao({
  pagina,
  tamanho,
  total,
  aoMudar,
}: {
  pagina: number
  tamanho: number
  total: number
  aoMudar: (pagina: number) => void
}) {
  const paginas = Math.max(1, Math.ceil(total / tamanho))
  if (total <= tamanho) return null
  return (
    <nav className="paginacao" aria-label="Paginação">
      <button type="button" disabled={pagina === 0} onClick={() => aoMudar(pagina - 1)}>
        Anterior
      </button>
      <span>
        Página {pagina + 1} de {paginas} ({total} registros)
      </span>
      <button type="button" disabled={pagina + 1 >= paginas} onClick={() => aoMudar(pagina + 1)}>
        Próxima
      </button>
    </nav>
  )
}
