import { NavLink, Outlet } from 'react-router-dom'
import { useSessao } from '../sessao/SessaoContext'

export function Layout() {
  const { sessao, propriedades, propriedade, selecionarPropriedade, sair } = useSessao()

  return (
    <div className="app">
      <header className="topo">
        <NavLink to="/" className="marca" aria-label="SysAgropec, início">
          <img src="/favicon.svg" alt="" width={26} height={26} />
          SysAgropec
        </NavLink>
        <span className="espaco" />
        {propriedades.length > 0 && (
          <label>
            <span className="sr-only">Propriedade</span>
            <select
              value={propriedade?.id ?? ''}
              onChange={(e) => selecionarPropriedade(e.target.value)}
              aria-label="Propriedade selecionada"
            >
              {propriedades.map((p) => (
                <option key={p.id} value={p.id}>
                  {p.nome}
                </option>
              ))}
            </select>
          </label>
        )}
        <span className="usuario">{sessao?.nome}</span>
        <button type="button" className="sair" onClick={() => void sair()}>
          Sair
        </button>
      </header>
      <div className="corpo">
        <nav className="menu" aria-label="Menu principal">
          {propriedade ? (
            <>
              <NavLink to="/" end>Início</NavLink>
              <NavLink to="/rotinas">Rotinas de hoje</NavLink>
              <NavLink to="/animais">Animais</NavLink>
              <NavLink to="/lotes">Lotes</NavLink>
              <NavLink to="/tarefas">Tarefas</NavLink>
              <NavLink to="/estrutura">Estrutura</NavLink>
            </>
          ) : (
            <NavLink to="/" end>Primeiros passos</NavLink>
          )}
          {sessao?.administrador && <NavLink to="/usuarios">Usuários</NavLink>}
        </nav>
        <main className="conteudo" id="conteudo">
          <Outlet />
        </main>
      </div>
    </div>
  )
}
