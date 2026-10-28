import { useState, type FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { useSessao } from '../sessao/SessaoContext'
import { Campo, Erro } from '../componentes/ui'

export function Login() {
  const { entrar } = useSessao()
  const [email, setEmail] = useState('')
  const [senha, setSenha] = useState('')
  const [erro, setErro] = useState<unknown>(null)
  const [enviando, setEnviando] = useState(false)

  async function enviar(e: FormEvent) {
    e.preventDefault()
    setErro(null)
    setEnviando(true)
    try {
      await entrar(email, senha)
    } catch (err) {
      setErro(err)
    } finally {
      setEnviando(false)
    }
  }

  return (
    <div className="tela-login">
      <form className="cartao grade" onSubmit={enviar}>
        <div>
          <div className="marca">SysAgropec</div>
          <p>Gestão do confinamento</p>
        </div>
        <Erro erro={erro} />
        <Campo rotulo="E-mail" type="email" autoComplete="username" required value={email}
               onChange={(e) => setEmail(e.target.value)} />
        <Campo rotulo="Senha" type="password" autoComplete="current-password" required value={senha}
               onChange={(e) => setSenha(e.target.value)} />
        <button className="primario" type="submit" disabled={enviando}>
          {enviando ? 'Entrando…' : 'Entrar'}
        </button>
        <Link to="/esqueci-senha">Esqueci minha senha</Link>
      </form>
    </div>
  )
}
