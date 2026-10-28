import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { ErroApi } from './api/cliente'
import { SessaoProvider } from './sessao/SessaoContext'
import { App } from './App'
import './estilos.css'

const cliente = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 15_000,
      refetchOnWindowFocus: true,
      // Erros de autorização ou validação não melhoram com nova tentativa.
      retry: (tentativas, erro) => !(erro instanceof ErroApi && erro.status >= 400 && erro.status < 500) && tentativas < 2,
    },
  },
})

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <QueryClientProvider client={cliente}>
      <BrowserRouter>
        <SessaoProvider>
          <App />
        </SessaoProvider>
      </BrowserRouter>
    </QueryClientProvider>
  </StrictMode>,
)
