import { defineConfig, devices } from '@playwright/test'

/**
 * Verificação do fluxo completo pela interface, contra a aplicação real (backend + PostgreSQL).
 * Pré-requisitos e execução: ver README (seção "Teste de ponta a ponta").
 */
export default defineConfig({
  testDir: './e2e',
  timeout: 60_000,
  fullyParallel: false,
  workers: 1,
  retries: 0,
  reporter: [['list'], ['html', { open: 'never' }]],
  use: {
    baseURL: process.env.E2E_URL ?? 'http://localhost:5173',
    locale: 'pt-BR',
    timezoneId: 'America/Sao_Paulo',
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
  },
  projects: [
    { name: 'desktop', use: { ...devices['Desktop Chrome'] } },
    // Depende do fluxo completo, que cria a propriedade usada na navegação.
    { name: 'celular', use: { ...devices['Pixel 7'] }, dependencies: ['desktop'] },
  ],
})
