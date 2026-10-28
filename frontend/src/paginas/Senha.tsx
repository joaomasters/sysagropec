import { useState, type FormEvent } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { api } from '../api/cliente'
import { Campo, Erro, Mensagem } from '../componentes/ui'

export function EsqueciSenha() {
  const [email, setEmail] = useState('')
  const [enviado, setEnviado] = useState(false)
  const [erro, setErro] = useState<unknown>(null)

  async function enviar(e: FormEvent) {
    e.preventDefault()
    setErro(null)
    try {
      await api.post('/api/v1/auth/senha/solicitar-redefinicao', { email })
      setEnviado(true)
    } catch (err) {
      setErro(err)
    }
  }

  return (
    <div className="tela-login">
      <form className="cartao grade" onSubmit={enviar}>
        <h1>Redefinir senha</h1>
        {enviado ? (
          <Mensagem tipo="sucesso">
            Se houver uma conta ativa com este e-mail, enviaremos um link válido por 30 minutos.
          </Mensagem>
        ) : (
          <>
            <Erro erro={erro} />
            <Campo rotulo="E-mail" type="email" required value={email} onChange={(e) => setEmail(e.target.value)} />
            <button className="primario" type="submit">Enviar link</button>
          </>
        )}
        <Link to="/">Voltar ao login</Link>
      </form>
    </div>
  )
}

export function DefinirSenha() {
  const [parametros] = useSearchParams()
  const navegar = useNavigate()
  const token = parametros.get('token') ?? ''
  const [senha, setSenha] = useState('')
  const [confirmacao, setConfirmacao] = useState('')
  const [erro, setErro] = useState<unknown>(null)
  const [concluido, setConcluido] = useState(false)
  const divergentes = confirmacao.length > 0 && senha !== confirmacao

  async function enviar(e: FormEvent) {
    e.preventDefault()
    if (divergentes) return
    setErro(null)
    try {
      await api.post('/api/v1/auth/senha/definir', { token, novaSenha: senha })
      setConcluido(true)
    } catch (err) {
      setErro(err)
    }
  }

  return (
    <div className="tela-login">
      <form className="cartao grade" onSubmit={enviar}>
        <h1>Definir senha</h1>
        {concluido ? (
          <>
            <Mensagem tipo="sucesso">Senha definida. Você já pode entrar.</Mensagem>
            <button className="primario" type="button" onClick={() => navegar('/')}>Ir para o login</button>
          </>
        ) : !token ? (
          <Mensagem tipo="erro">Link incompleto. Use o link recebido por e-mail.</Mensagem>
        ) : (
          <>
            <Erro erro={erro} />
            <Campo rotulo="Nova senha" type="password" autoComplete="new-password" minLength={10} required
                   ajuda="Mínimo de 10 caracteres." value={senha} onChange={(e) => setSenha(e.target.value)} />
            <Campo rotulo="Confirme a senha" type="password" autoComplete="new-password" required
                   aria-invalid={divergentes} value={confirmacao} onChange={(e) => setConfirmacao(e.target.value)}
                   ajuda={divergentes ? 'As senhas não conferem.' : undefined} />
            <button className="primario" type="submit" disabled={divergentes}>Salvar senha</button>
          </>
        )}
      </form>
    </div>
  )
}
