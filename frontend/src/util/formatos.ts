const fmtData = new Intl.DateTimeFormat('pt-BR', { dateStyle: 'short' })
const fmtDataHora = new Intl.DateTimeFormat('pt-BR', { dateStyle: 'short', timeStyle: 'short' })
const fmtNumero = new Intl.NumberFormat('pt-BR', { maximumFractionDigits: 2 })

export function data(valor: string | null | undefined): string {
  if (!valor) return '—'
  // Datas sem horário (AAAA-MM-DD) são locais: não converter de fuso.
  if (/^\d{4}-\d{2}-\d{2}$/.test(valor)) {
    const [a, m, d] = valor.split('-')
    return `${d}/${m}/${a}`
  }
  return fmtData.format(new Date(valor))
}

export function dataHora(valor: string | null | undefined): string {
  return valor ? fmtDataHora.format(new Date(valor)) : '—'
}

export function numero(valor: number | null | undefined): string {
  return valor === null || valor === undefined ? '—' : fmtNumero.format(valor)
}

export function kg(valor: number): string {
  return `${fmtNumero.format(valor)} kg`
}

/** Valor para <input type="datetime-local"> no horário local do dispositivo. */
export function agoraLocal(): string {
  const d = new Date()
  d.setSeconds(0, 0)
  return new Date(d.getTime() - d.getTimezoneOffset() * 60000).toISOString().slice(0, 16)
}

/** Converte o valor de <input type="datetime-local"> em instante ISO (UTC). */
export function paraInstante(local: string): string {
  return new Date(local).toISOString()
}

export function novaChave(): string {
  return crypto.randomUUID()
}

export const rotulos: Record<string, Record<string, string>> = {
  sexo: { MACHO: 'Macho', FEMEA: 'Fêmea' },
  categoria: {
    BEZERRO: 'Bezerro', BEZERRA: 'Bezerra', GARROTE: 'Garrote', NOVILHA: 'Novilha',
    BOI: 'Boi', VACA: 'Vaca', TOURO: 'Touro',
  },
  finalidade: { ENGORDA: 'Engorda', RECRIA: 'Recria', REPRODUCAO: 'Reprodução' },
  origem: { COMPRA: 'Compra', NASCIMENTO_PROPRIO: 'Nascimento próprio', TRANSFERENCIA: 'Transferência', OUTRA: 'Outra' },
  situacao: { ATIVO: 'Ativo', VENDIDO: 'Vendido', ABATIDO: 'Abatido', MORTO: 'Morto', BAIXADO: 'Baixado' },
  fase: {
    RECEPCAO: 'Recepção', ADAPTACAO: 'Adaptação', CRESCIMENTO: 'Crescimento',
    TERMINACAO: 'Terminação', RECRIA: 'Recria', OUTRA: 'Outra',
  },
  tipoInstalacao: { CURRAL: 'Curral', PIQUETE: 'Piquete', PASTO: 'Pasto', OUTRA: 'Outra' },
  tipoTarefa: {
    TRATO: 'Trato', LEITURA_COCHO: 'Leitura de cocho', LIMPEZA: 'Limpeza', INSPECAO_AGUA: 'Inspeção de água',
    PESAGEM: 'Pesagem', PROCEDIMENTO_SANITARIO: 'Procedimento sanitário', MANUTENCAO: 'Manutenção', OUTRA: 'Outra',
  },
  statusTarefa: {
    PENDENTE: 'Pendente', EM_ANDAMENTO: 'Em andamento', CONCLUIDA: 'Concluída', IMPEDIDA: 'Impedida', CANCELADA: 'Cancelada',
  },
  prioridade: { BAIXA: 'Baixa', MEDIA: 'Média', ALTA: 'Alta' },
  alvoTipo: { PROPRIEDADE: 'Propriedade', INSTALACAO: 'Instalação', LOTE: 'Lote', ANIMAL: 'Animal' },
  natureza: { MEDIDO: 'Medido', ESTIMADO: 'Estimado' },
  metodo: {
    BALANCA_INDIVIDUAL: 'Balança individual', FITA_BARIMETRICA: 'Fita barimétrica', VISUAL: 'Avaliação visual', OUTRO: 'Outro',
  },
  finalidadePesagem: { ENTRADA: 'Entrada', ACOMPANHAMENTO: 'Acompanhamento', SAIDA: 'Saída' },
  perfil: {
    GESTOR: 'Gestor', VETERINARIO: 'Veterinário', NUTRICIONISTA: 'Nutricionista',
    FINANCEIRO: 'Financeiro', OPERADOR: 'Operador', CONSULTA: 'Consulta',
  },
}

export function rotulo(grupo: keyof typeof rotulos, valor: string | null | undefined): string {
  if (!valor) return '—'
  return rotulos[grupo]?.[valor] ?? valor
}

export function opcoes(grupo: keyof typeof rotulos): { valor: string; rotulo: string }[] {
  return Object.entries(rotulos[grupo]).map(([valor, r]) => ({ valor, rotulo: r }))
}
