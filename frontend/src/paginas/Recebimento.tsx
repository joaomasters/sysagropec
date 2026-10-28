import { useState, type FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { api } from '../api/cliente'
import { useInstalacoes, useLotes } from '../api/consultas'
import { usePropriedade, useSessao } from '../sessao/SessaoContext'
import { AreaTexto, Campo, Erro, Mensagem, Selecao, Vazio } from '../componentes/ui'
import { agoraLocal, novaChave, opcoes, paraInstante } from '../util/formatos'

interface Linha {
  chave: string
  brinco: string
  identificadorEletronico: string
  sexo: string
  categoria: string
  raca: string
  dataNascimento: string
  nascimentoEstimado: boolean
}

interface Resultado {
  recebimentoId: string
  animais: { id: string; numero: number; brinco: string }[]
}

function novaLinha(base?: Partial<Linha>): Linha {
  return {
    chave: novaChave(), brinco: '', identificadorEletronico: '', sexo: base?.sexo ?? 'MACHO',
    categoria: base?.categoria ?? 'GARROTE', raca: base?.raca ?? '', dataNascimento: '', nascimentoEstimado: false,
  }
}

export function Recebimento() {
  const propriedade = usePropriedade()
  const { pode } = useSessao()
  const cliente = useQueryClient()
  const instalacoes = useInstalacoes(propriedade.id)
  const lotes = useLotes(propriedade.id)

  const [ocorridoEm, setOcorridoEm] = useState(agoraLocal)
  const [instalacaoId, setInstalacaoId] = useState('')
  const [loteId, setLoteId] = useState('')
  const [origemDescricao, setOrigemDescricao] = useState('')
  const [documento, setDocumento] = useState('')
  const [observacao, setObservacao] = useState('')
  const [linhas, setLinhas] = useState<Linha[]>([novaLinha()])
  // Uma chave por formulário: reenvios (duplo clique, rede instável) não duplicam animais.
  const [chave, setChave] = useState(novaChave)

  const receber = useMutation({
    mutationFn: () =>
      api.post<Resultado>(`/api/v1/propriedades/${propriedade.id}/recebimentos`, {
        instalacaoId, loteId: loteId || null, ocorridoEm: paraInstante(ocorridoEm), origemDescricao,
        documento: documento || null, observacao: observacao || null,
        animais: linhas.map((l) => ({
          brinco: l.brinco, identificadorEletronico: l.identificadorEletronico || null, sexo: l.sexo,
          categoria: l.categoria, raca: l.raca || null, dataNascimento: l.dataNascimento || null,
          nascimentoEstimado: l.nascimentoEstimado,
        })),
      }, { chaveIdempotencia: chave }),
    onSuccess: () => {
      void cliente.invalidateQueries({ queryKey: ['animais'] })
      void cliente.invalidateQueries({ queryKey: ['dashboard'] })
      void cliente.invalidateQueries({ queryKey: ['lotes'] })
    },
  })

  if (!pode('ANIMAL_RECEBER')) {
    return <Vazio titulo="Seu perfil não permite receber animais nesta propriedade" />
  }

  if (receber.data) {
    return (
      <>
        <div className="cabecalho-pagina"><h1>Recebimento registrado</h1></div>
        <Mensagem tipo="sucesso">{receber.data.animais.length} animais recebidos.</Mensagem>
        <section className="cartao">
          <table>
            <thead><tr><th>Nº</th><th>Brinco</th></tr></thead>
            <tbody>
              {receber.data.animais.map((a) => (
                <tr key={a.id}><td>{a.numero}</td><td><Link to={`/animais/${a.id}`}>{a.brinco}</Link></td></tr>
              ))}
            </tbody>
          </table>
        </section>
        <div className="linha">
          <Link className="botao primario" to="/animais">Ver animais</Link>
          <button type="button" onClick={() => { receber.reset(); setLinhas([novaLinha()]); setChave(novaChave()) }}>
            Novo recebimento
          </button>
        </div>
      </>
    )
  }

  const curraisAtivos = (instalacoes.data ?? []).filter((i) => i.ativa)
  const atualizar = (chaveLinha: string, campo: keyof Linha, valor: string | boolean) =>
    setLinhas((ls) => ls.map((l) => (l.chave === chaveLinha ? { ...l, [campo]: valor } : l)))

  function enviar(e: FormEvent) {
    e.preventDefault()
    receber.mutate()
  }

  return (
    <form onSubmit={enviar}>
      <div className="cabecalho-pagina"><h1>Receber animais</h1></div>
      {curraisAtivos.length === 0 && !instalacoes.isPending && (
        <Mensagem tipo="aviso">Cadastre ao menos uma instalação em <Link to="/estrutura">Estrutura</Link> antes de receber animais.</Mensagem>
      )}
      <section className="cartao grade">
        <h2>Dados do recebimento</h2>
        <div className="grade grade-2">
          <Campo rotulo="Data e hora da chegada" type="datetime-local" required value={ocorridoEm}
                 onChange={(e) => setOcorridoEm(e.target.value)} />
          <Selecao rotulo="Instalação de destino" required value={instalacaoId} vazio="Selecione"
                   onChange={(e) => setInstalacaoId(e.target.value)}
                   opcoes={curraisAtivos.map((i) => ({
                     valor: i.id, rotulo: `${i.codigo}${i.capacidadeCabecas ? ` (cap. ${i.capacidadeCabecas})` : ''}`,
                   }))} />
          <Selecao rotulo="Lote (opcional)" value={loteId} vazio="Sem lote por enquanto"
                   onChange={(e) => setLoteId(e.target.value)}
                   opcoes={(lotes.data ?? []).map((l) => ({ valor: l.id, rotulo: l.codigo }))} />
          <Campo rotulo="Origem / procedência" required maxLength={200} value={origemDescricao}
                 onChange={(e) => setOrigemDescricao(e.target.value)} ajuda="Fornecedor, fazenda de origem ou leilão." />
          <Campo rotulo="Documento (GTA, nota)" maxLength={60} value={documento} onChange={(e) => setDocumento(e.target.value)} />
        </div>
        <AreaTexto rotulo="Observação" maxLength={500} value={observacao} onChange={(e) => setObservacao(e.target.value)} />
      </section>

      <section className="cartao">
        <h2>Animais ({linhas.length})</h2>
        <div className="tabela-rolagem">
          <table>
            <thead>
              <tr>
                <th>Brinco *</th><th>Ident. eletrônico</th><th>Sexo</th><th>Categoria</th><th>Raça</th>
                <th>Nascimento</th><th>Estimado?</th><th><span className="sr-only">Ações</span></th>
              </tr>
            </thead>
            <tbody>
              {linhas.map((l, i) => (
                <tr key={l.chave}>
                  <td><input aria-label={`Brinco do animal ${i + 1}`} required maxLength={40} value={l.brinco}
                             onChange={(e) => atualizar(l.chave, 'brinco', e.target.value)} /></td>
                  <td><input aria-label={`Identificador eletrônico do animal ${i + 1}`} maxLength={40}
                             value={l.identificadorEletronico}
                             onChange={(e) => atualizar(l.chave, 'identificadorEletronico', e.target.value)} /></td>
                  <td>
                    <select aria-label={`Sexo do animal ${i + 1}`} value={l.sexo} onChange={(e) => atualizar(l.chave, 'sexo', e.target.value)}>
                      {opcoes('sexo').map((o) => <option key={o.valor} value={o.valor}>{o.rotulo}</option>)}
                    </select>
                  </td>
                  <td>
                    <select aria-label={`Categoria do animal ${i + 1}`} value={l.categoria}
                            onChange={(e) => atualizar(l.chave, 'categoria', e.target.value)}>
                      {opcoes('categoria').map((o) => <option key={o.valor} value={o.valor}>{o.rotulo}</option>)}
                    </select>
                  </td>
                  <td><input aria-label={`Raça do animal ${i + 1}`} maxLength={60} value={l.raca}
                             onChange={(e) => atualizar(l.chave, 'raca', e.target.value)} /></td>
                  <td><input aria-label={`Data de nascimento do animal ${i + 1}`} type="date" value={l.dataNascimento}
                             onChange={(e) => atualizar(l.chave, 'dataNascimento', e.target.value)} /></td>
                  <td>
                    <input aria-label={`Nascimento estimado do animal ${i + 1}`} type="checkbox" disabled={!l.dataNascimento}
                           checked={l.nascimentoEstimado}
                           onChange={(e) => atualizar(l.chave, 'nascimentoEstimado', e.target.checked)} />
                  </td>
                  <td>
                    <button type="button" className="discreto" disabled={linhas.length === 1}
                            onClick={() => setLinhas((ls) => ls.filter((x) => x.chave !== l.chave))}>
                      Remover
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        <div className="linha" style={{ marginTop: 12 }}>
          <button type="button" onClick={() => setLinhas((ls) => [...ls, novaLinha(ls[ls.length - 1])])}>Adicionar animal</button>
          <button type="button" onClick={() => setLinhas((ls) => [...ls, ...Array.from({ length: 10 }, () => novaLinha(ls[ls.length - 1]))])}>
            Adicionar 10 linhas
          </button>
        </div>
      </section>

      <Erro erro={receber.error} />
      <button className="primario" type="submit" disabled={receber.isPending || curraisAtivos.length === 0}>
        {receber.isPending ? 'Registrando…' : `Registrar recebimento de ${linhas.length} animal(is)`}
      </button>
    </form>
  )
}
