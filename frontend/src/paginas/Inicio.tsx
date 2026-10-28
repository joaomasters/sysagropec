import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { api } from '../api/cliente'
import type { Dashboard } from '../api/tipos'
import { usePropriedade } from '../sessao/SessaoContext'
import { Carregando, Erro, Vazio } from '../componentes/ui'
import { data, dataHora, kg, rotulo } from '../util/formatos'

export function Inicio() {
  const propriedade = usePropriedade()
  const { data: d, isPending, error } = useQuery({
    queryKey: ['dashboard', propriedade.id],
    queryFn: () => api.get<Dashboard>(`/api/v1/propriedades/${propriedade.id}/dashboard`),
  })

  if (isPending) return <Carregando />
  if (error) return <Erro erro={error} />

  return (
    <>
      <div className="cabecalho-pagina">
        <h1>{propriedade.nome}</h1>
        <span className="selo">Hoje: {data(d.data)}</span>
      </div>

      <section className="grade grade-4" aria-label="Indicadores">
        <Indicador nome="Animais ativos" valor={d.animaisAtivos} />
        <Indicador nome="Animais sem lote" valor={d.animaisSemLote} alerta={d.animaisSemLote > 0} />
        <Indicador nome="Tarefas abertas até hoje" valor={d.tarefas.abertasAteHoje} />
        <Indicador nome="Tarefas atrasadas" valor={d.tarefas.atrasadas} alerta={d.tarefas.atrasadas > 0} />
        <Indicador nome="Finalizadas hoje" valor={d.tarefas.finalizadasHoje} />
        <Indicador nome="Pesagens (30 dias)" valor={d.pesagens.ultimos30Dias}
                   detalhe={`${d.pesagens.medidas30Dias} medidas · ${d.pesagens.estimadas30Dias} estimadas`} />
      </section>

      <div className="grade grade-2" style={{ marginTop: 16 }}>
        <section className="cartao">
          <h2>Ocupação das instalações</h2>
          {d.ocupacao.length === 0 ? (
            <Vazio titulo="Nenhuma instalação cadastrada">
              <Link to="/estrutura">Cadastrar currais</Link>
            </Vazio>
          ) : (
            <div className="tabela-rolagem">
            <table>
              <thead>
                <tr><th>Instalação</th><th className="num">Animais</th><th>Lotação</th></tr>
              </thead>
              <tbody>
                {d.ocupacao.map((o) => {
                  const taxa = o.capacidade ? o.ocupacao / o.capacidade : null
                  return (
                    <tr key={o.instalacaoId}>
                      <td>{o.codigo} <small>({rotulo('tipoInstalacao', o.tipo)})</small></td>
                      <td className="num">{o.ocupacao}{o.capacidade ? ` / ${o.capacidade}` : ''}</td>
                      <td>
                        {taxa === null ? (
                          <small>capacidade não definida</small>
                        ) : (
                          <div className={`barra ${taxa >= 0.9 ? 'cheia' : ''}`} role="img"
                               aria-label={`${Math.round(taxa * 100)}% da capacidade`}>
                            <span style={{ width: `${Math.min(100, taxa * 100)}%` }} />
                          </div>
                        )}
                      </td>
                    </tr>
                  )
                })}
              </tbody>
            </table>
            </div>
          )}
        </section>

        <section className="cartao">
          <h2>Lotes ativos</h2>
          {d.lotes.length === 0 ? (
            <Vazio titulo="Nenhum lote ativo"><Link to="/lotes">Formar lote</Link></Vazio>
          ) : (
            <table>
              <thead><tr><th>Lote</th><th>Fase</th><th className="num">Animais</th></tr></thead>
              <tbody>
                {d.lotes.map((l) => (
                  <tr key={l.id}>
                    <td><Link to={`/animais?lote=${l.id}`}>{l.codigo}</Link></td>
                    <td>{rotulo('fase', l.fase)}</td>
                    <td className="num">{l.animais}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </section>

        <section className="cartao">
          <h2>Animais por categoria</h2>
          {d.animaisAtivos === 0 ? (
            <Vazio titulo="Nenhum animal ativo"><Link to="/animais/receber">Receber animais</Link></Vazio>
          ) : (
            <table>
              <tbody>
                {Object.entries(d.animaisPorCategoria).map(([cat, qtd]) => (
                  <tr key={cat}><td>{rotulo('categoria', cat)}</td><td className="num">{qtd}</td></tr>
                ))}
              </tbody>
            </table>
          )}
        </section>

        <section className="cartao">
          <h2>Últimas pesagens</h2>
          {d.pesagens.recentes.length === 0 ? (
            <Vazio titulo="Nenhuma pesagem registrada" />
          ) : (
            <table>
              <tbody>
                {d.pesagens.recentes.map((p) => (
                  <tr key={p.pesagemId}>
                    <td><Link to={`/animais/${p.animalId}`}>{p.brinco ?? `Nº ${p.animalNumero}`}</Link></td>
                    <td className="num">{kg(p.pesoKg)}</td>
                    <td>{p.natureza === 'ESTIMADO' ? <span className="selo alerta">estimado</span> : null}</td>
                    <td>{dataHora(p.ocorridoEm)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </section>
      </div>

      <section className="cartao">
        <h2>Desempenho e custos</h2>
        <p className="vazio" style={{ textAlign: 'left', padding: 0 }}>
          Indisponível nesta etapa. Ganho de peso, consumo e custos serão exibidos quando os módulos de nutrição e
          custos estiverem implementados e houver dados suficientes, com os critérios de cálculo documentados.
        </p>
      </section>
    </>
  )
}

function Indicador({ nome, valor, alerta, detalhe }: { nome: string; valor: number; alerta?: boolean; detalhe?: string }) {
  return (
    <div className={`indicador ${alerta ? 'alerta' : ''}`}>
      <div className="valor">{valor}</div>
      <div className="nome">{nome}</div>
      {detalhe && <small>{detalhe}</small>}
    </div>
  )
}
