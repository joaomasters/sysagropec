// Contratos da API /api/v1 (espelham as respostas do backend).

export type Permissao =
  | 'CONSULTAR'
  | 'INSTALACAO_GERIR'
  | 'ANIMAL_RECEBER'
  | 'ANIMAL_MOVIMENTAR'
  | 'LOTE_GERIR'
  | 'PESAGEM_REGISTRAR'
  | 'PESAGEM_CANCELAR'
  | 'TAREFA_GERIR'
  | 'TAREFA_EXECUTAR'

export interface Sessao {
  usuarioId: string
  nome: string
  email: string
  organizacaoId: string
  organizacaoNome: string
  administrador: boolean
  permissoesPorPropriedade: Record<string, Permissao[]>
}

export interface Pagina<T> {
  itens: T[]
  pagina: number
  tamanho: number
  total: number
}

export interface Propriedade {
  id: string
  nome: string
  municipio: string | null
  uf: string | null
  fusoHorario: string
  ativa: boolean
}

export interface Instalacao {
  id: string
  propriedadeId: string
  tipo: string
  codigo: string
  descricao: string | null
  capacidadeCabecas: number | null
  ativa: boolean
}

export interface AnimalResumo {
  id: string
  numero: number
  brinco: string | null
  identificadorEletronico: string | null
  sexo: string
  categoria: string
  finalidade: string
  raca: string | null
  situacao: string
  dataNascimento: string | null
  nascimentoEstimado: boolean
  loteId: string | null
  loteCodigo: string | null
  instalacaoId: string | null
  entradaEm: string | null
}

export interface AnimalDetalhe {
  id: string
  propriedadeId: string
  numero: number
  sexo: string
  categoria: string
  finalidade: string
  raca: string | null
  situacao: string
  dataNascimento: string | null
  nascimentoEstimado: boolean
  origem: string
  origemDescricao: string | null
  recebimentoId: string | null
  entradaEm: string | null
  identificacoes: { tipo: string; valor: string; inicio: string; fim: string | null }[]
  lotes: { loteId: string; loteCodigo: string; inicio: string; fim: string | null }[]
  localizacoes: { instalacaoId: string; inicio: string; fim: string | null }[]
}

export interface Lote {
  id: string
  propriedadeId: string
  codigo: string
  fase: string
  finalidade: string
  status: string
  inicio: string
  fim: string | null
  observacao: string | null
  quantidadeAnimais: number
}

export interface Pesagem {
  id: string
  animalId: string
  pesoKg: number
  natureza: string
  metodo: string
  finalidade: string
  equipamento: string | null
  observacao: string | null
  ocorridoEm: string
  registradoEm: string
  tarefaId: string | null
  cancelada: boolean
  canceladaEm: string | null
  motivoCancelamento: string | null
}

export interface TarefaResumo {
  id: string
  propriedadeId: string
  tipo: string
  titulo: string
  alvoTipo: string
  alvoId: string
  alvoDescricao: string | null
  responsavelId: string | null
  responsavelNome: string | null
  prazo: string
  prioridade: string
  status: string
  atrasada: boolean
  checklistTotal: number
  checklistConcluidos: number
  finalizadaEm: string | null
}

export interface TarefaDetalhe {
  resumo: TarefaResumo
  descricao: string | null
  criadaEm: string
  iniciadaEm: string | null
  finalizadaPorNome: string | null
  observacaoConclusao: string | null
  justificativa: string | null
  checklist: { id: string; ordem: number; descricao: string; obrigatorio: boolean; concluido: boolean; concluidoEm: string | null }[]
  resultados: { tipo: string; id: string; vinculadoEm: string }[]
}

export interface RotinasDoDia {
  data: string
  fusoHorario: string
  abertas: TarefaResumo[]
  finalizadasHoje: TarefaResumo[]
}

export interface EventoLinhaDoTempo {
  tipo: string
  ocorridoEm: string
  registradoEm: string
  titulo: string
  descricao: string | null
  responsavel: string | null
  referenciaId: string
}

export interface Dashboard {
  data: string
  fusoHorario: string
  animaisAtivos: number
  animaisSemLote: number
  animaisPorCategoria: Record<string, number>
  ocupacao: { instalacaoId: string; codigo: string; tipo: string; capacidade: number | null; ocupacao: number }[]
  lotes: { id: string; codigo: string; fase: string; animais: number }[]
  tarefas: { abertasAteHoje: number; atrasadas: number; finalizadasHoje: number }
  pesagens: {
    ultimos30Dias: number
    medidas30Dias: number
    estimadas30Dias: number
    recentes: { pesagemId: string; animalId: string; animalNumero: number; brinco: string | null; pesoKg: number; natureza: string; ocorridoEm: string }[]
  }
}

export interface UsuarioResumo {
  id: string
  nome: string
  email: string
  administrador: boolean
  ativo: boolean
  aguardandoAtivacao: boolean
  acessos: { propriedadeId: string; perfil: string }[]
}
