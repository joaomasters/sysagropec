import { Navigate, Route, Routes } from 'react-router-dom'
import { useSessao } from './sessao/SessaoContext'
import { Layout } from './componentes/Layout'
import { Carregando } from './componentes/ui'
import { Login } from './paginas/Login'
import { EsqueciSenha, DefinirSenha } from './paginas/Senha'
import { Inicio } from './paginas/Inicio'
import { Rotinas } from './paginas/Rotinas'
import { Animais } from './paginas/Animais'
import { FichaAnimal } from './paginas/FichaAnimal'
import { Recebimento } from './paginas/Recebimento'
import { Movimentar } from './paginas/Movimentar'
import { Lotes } from './paginas/Lotes'
import { Tarefas } from './paginas/Tarefas'
import { DetalheTarefa } from './paginas/DetalheTarefa'
import { Estrutura, PrimeiraPropriedade } from './paginas/Estrutura'
import { Usuarios } from './paginas/Usuarios'

export function App() {
  const { sessao, carregando, propriedade } = useSessao()

  if (carregando) {
    return <Carregando />
  }

  if (!sessao) {
    return (
      <Routes>
        <Route path="/esqueci-senha" element={<EsqueciSenha />} />
        <Route path="/definir-senha" element={<DefinirSenha />} />
        <Route path="*" element={<Login />} />
      </Routes>
    )
  }

  return (
    <Routes>
      <Route path="/definir-senha" element={<DefinirSenha />} />
      <Route element={<Layout />}>
        {propriedade ? (
          <>
            <Route index element={<Inicio />} />
            <Route path="rotinas" element={<Rotinas />} />
            <Route path="animais" element={<Animais />} />
            <Route path="animais/receber" element={<Recebimento />} />
            <Route path="animais/movimentar" element={<Movimentar />} />
            <Route path="animais/:id" element={<FichaAnimal />} />
            <Route path="lotes" element={<Lotes />} />
            <Route path="tarefas" element={<Tarefas />} />
            <Route path="tarefas/:id" element={<DetalheTarefa />} />
            <Route path="estrutura" element={<Estrutura />} />
            <Route path="usuarios" element={<Usuarios />} />
            <Route path="*" element={<Navigate to="/" replace />} />
          </>
        ) : (
          <>
            <Route path="usuarios" element={<Usuarios />} />
            <Route path="*" element={<PrimeiraPropriedade />} />
          </>
        )}
      </Route>
    </Routes>
  )
}
