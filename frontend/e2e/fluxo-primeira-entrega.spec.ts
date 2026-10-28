import { expect, test, type Page } from '@playwright/test'

/**
 * Critério de aceite da primeira entrega, pela interface:
 * criar propriedade e curral, receber animais, formar lote, registrar pesagem,
 * criar e concluir tarefa e consultar o histórico do animal.
 *
 * Requer um administrador ativo em uma organização SEM propriedades
 * (E2E_EMAIL / E2E_SENHA), por exemplo criado pela inicialização em banco vazio.
 */
const EMAIL = process.env.E2E_EMAIL ?? ''
const SENHA = process.env.E2E_SENHA ?? ''
const sufixo = Date.now().toString(36)

async function entrar(page: Page) {
  await page.goto('/')
  await page.getByLabel('E-mail').fill(EMAIL)
  await page.getByLabel('Senha').fill(SENHA)
  await page.getByRole('button', { name: 'Entrar' }).click()
}

test.describe.configure({ mode: 'serial' })

test('fluxo completo do confinamento', async ({ page }, info) => {
  test.skip(!EMAIL || !SENHA, 'Defina E2E_EMAIL e E2E_SENHA')
  test.skip(info.project.name !== 'desktop', 'O fluxo completo roda uma vez; o celular verifica navegação')

  await entrar(page)

  // Propriedade: primeira tela do administrador sem propriedades, ou formulário em Estrutura.
  const nomeFazenda = `Fazenda E2E ${sufixo}`
  if (!(await page.getByRole('heading', { name: 'Bem-vindo ao SysAgropec' }).isVisible().catch(() => false))) {
    await page.getByRole('link', { name: 'Estrutura' }).click()
  }
  await page.getByLabel('Nome', { exact: true }).fill(nomeFazenda)
  await page.getByLabel('UF').fill('GO')
  await page.getByRole('button', { name: 'Cadastrar propriedade' }).click()
  await expect(page.getByLabel('Propriedade selecionada')).toContainText(nomeFazenda)

  // Curral.
  await page.getByRole('link', { name: 'Estrutura' }).click()
  await page.getByLabel('Código').fill('C1')
  await page.getByLabel('Capacidade (cabeças)').fill('50')
  await page.getByRole('button', { name: 'Cadastrar instalação' }).click()
  await expect(page.getByRole('cell', { name: 'C1', exact: true })).toBeVisible()

  // Recebimento de 2 animais.
  await page.getByRole('link', { name: 'Animais' }).click()
  await page.getByRole('link', { name: 'Receber animais' }).click()
  await page.getByLabel('Instalação de destino').selectOption({ label: 'C1 (cap. 50)' })
  await page.getByLabel('Origem / procedência').fill('Fornecedor E2E')
  await page.getByLabel('Brinco do animal 1').fill(`E2E-${sufixo}-1`)
  await page.getByRole('button', { name: 'Adicionar animal' }).click()
  await page.getByLabel('Brinco do animal 2').fill(`E2E-${sufixo}-2`)
  await page.getByRole('button', { name: /Registrar recebimento de 2/ }).click()
  await expect(page.getByText('2 animais recebidos.')).toBeVisible()

  // Formação de lote com os animais sem lote.
  await page.getByRole('link', { name: 'Lotes' }).click()
  await page.getByLabel('Código').fill(`L-${sufixo}`)
  await page.getByLabel(`E2E-${sufixo}-1`).check()
  await page.getByLabel(`E2E-${sufixo}-2`).check()
  await page.getByRole('button', { name: 'Formar lote' }).click()
  await expect(page.getByRole('link', { name: `L-${sufixo}` })).toBeVisible()

  // Tarefa de pesagem para o animal 1.
  await page.getByRole('link', { name: 'Tarefas' }).click()
  await page.getByRole('button', { name: 'Nova tarefa' }).click()
  await page.getByLabel('Tipo').selectOption('PESAGEM')
  await page.getByLabel('Título').fill('Pesar animal E2E')
  await page.getByLabel('Onde / em quê').selectOption('ANIMAL')
  await page.getByLabel('Buscar animal').fill(`E2E-${sufixo}-1`)
  await page.getByRole('combobox', { name: /^Animal/ }).selectOption({ index: 1 })
  await page.getByRole('button', { name: 'Criar tarefa' }).click()
  await expect(page.getByRole('link', { name: 'Pesar animal E2E' })).toBeVisible()

  // Execução em "Rotinas de hoje": a pesagem conclui a tarefa.
  await page.getByRole('link', { name: 'Rotinas de hoje' }).click()
  const cartao = page.locator('li.tarefa', { hasText: 'Pesar animal E2E' })
  await cartao.getByRole('button', { name: 'Registrar pesagem' }).click()
  await cartao.getByLabel('Peso (kg)').fill('345,5')
  await cartao.getByRole('button', { name: 'Registrar pesagem' }).last().click()
  await expect(page.getByText('Pesagem registrada e tarefa concluída.')).toBeVisible()
  await expect(page.locator('section', { hasText: 'Finalizadas hoje' }).getByText('Pesar animal E2E')).toBeVisible()

  // Histórico do animal.
  await page.getByRole('link', { name: 'Animais' }).click()
  await page.getByRole('link', { name: `E2E-${sufixo}-1` }).click()
  const historico = page.locator('section', { hasText: 'Linha do tempo' })
  await expect(historico.getByText('Entrada na propriedade')).toBeVisible()
  await expect(historico.getByText('Inclusão em lote')).toBeVisible()
  await expect(historico.getByText('Pesagem de acompanhamento')).toBeVisible()
  await expect(historico.getByText('Tarefa: Pesar animal E2E')).toBeVisible()
  await expect(page.getByRole('cell', { name: '345,5 kg' })).toBeVisible()

  // Persistência: recarregar a página mantém os dados.
  await page.reload()
  await expect(page.getByRole('cell', { name: '345,5 kg' })).toBeVisible()
})

test('navegação no celular', async ({ page }, info) => {
  test.skip(!EMAIL || !SENHA, 'Defina E2E_EMAIL e E2E_SENHA')
  test.skip(info.project.name !== 'celular', 'Somente no perfil de celular')
  await entrar(page)
  await page.getByRole('link', { name: 'Rotinas de hoje' }).click()
  await expect(page.getByRole('heading', { name: 'Rotinas de hoje' })).toBeVisible()
  const largura = await page.evaluate(() => document.documentElement.scrollWidth)
  const viewport = page.viewportSize()?.width ?? 0
  expect(largura).toBeLessThanOrEqual(viewport)
})
