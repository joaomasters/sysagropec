import type { Pesagem } from '../api/tipos'
import { data, kg } from '../util/formatos'

/**
 * Evolução do peso do animal. Pesagens canceladas não entram; estimativas aparecem com marcador vazado
 * para não serem confundidas com medições. Não calcula ganho de peso (critério ainda não definido).
 */
export function GraficoPeso({ pesagens }: { pesagens: Pesagem[] }) {
  const pontos = pesagens
    .filter((p) => !p.cancelada)
    .map((p) => ({ t: new Date(p.ocorridoEm).getTime(), peso: p.pesoKg, estimado: p.natureza === 'ESTIMADO', quando: p.ocorridoEm }))
    .sort((a, b) => a.t - b.t)

  if (pontos.length === 0) {
    return null
  }
  if (pontos.length === 1) {
    return <p className="vazio">O gráfico de evolução aparece a partir de duas pesagens válidas.</p>
  }

  const largura = 640
  const altura = 220
  const margem = { esq: 48, dir: 12, topo: 12, base: 28 }
  const tMin = pontos[0].t
  const tMax = pontos[pontos.length - 1].t
  const pMin = Math.min(...pontos.map((p) => p.peso))
  const pMax = Math.max(...pontos.map((p) => p.peso))
  const folga = Math.max(10, (pMax - pMin) * 0.15)
  const yMin = Math.max(0, pMin - folga)
  const yMax = pMax + folga
  const x = (t: number) => margem.esq + ((t - tMin) / Math.max(1, tMax - tMin)) * (largura - margem.esq - margem.dir)
  const y = (p: number) => margem.topo + (1 - (p - yMin) / (yMax - yMin)) * (altura - margem.topo - margem.base)
  const linha = pontos.map((p, i) => `${i === 0 ? 'M' : 'L'}${x(p.t).toFixed(1)},${y(p.peso).toFixed(1)}`).join(' ')
  const marcas = [yMin, (yMin + yMax) / 2, yMax]

  return (
    <figure style={{ margin: 0 }}>
      <svg viewBox={`0 0 ${largura} ${altura}`} width="100%" role="img"
           aria-label={`Evolução do peso: de ${kg(pontos[0].peso)} em ${data(pontos[0].quando)} a ${kg(pontos[pontos.length - 1].peso)} em ${data(pontos[pontos.length - 1].quando)}`}>
        {marcas.map((m) => (
          <g key={m}>
            <line x1={margem.esq} x2={largura - margem.dir} y1={y(m)} y2={y(m)} stroke="#e1e6df" />
            <text x={margem.esq - 6} y={y(m) + 4} textAnchor="end" fontSize="11" fill="#6b766e">{Math.round(m)}</text>
          </g>
        ))}
        <text x={margem.esq} y={altura - 8} fontSize="11" fill="#6b766e">{data(pontos[0].quando)}</text>
        <text x={largura - margem.dir} y={altura - 8} fontSize="11" fill="#6b766e" textAnchor="end">
          {data(pontos[pontos.length - 1].quando)}
        </text>
        <path d={linha} fill="none" stroke="#3a7f47" strokeWidth="2" />
        {pontos.map((p) => (
          <circle key={p.t} cx={x(p.t)} cy={y(p.peso)} r="4.5" fill={p.estimado ? '#fff' : '#3a7f47'} stroke="#3a7f47" strokeWidth="2">
            <title>{`${kg(p.peso)} em ${data(p.quando)}${p.estimado ? ' (estimado)' : ''}`}</title>
          </circle>
        ))}
      </svg>
      <figcaption style={{ fontSize: '0.85rem', color: 'var(--texto-3)' }}>
        ● medido &nbsp; ○ estimado · peso em kg
      </figcaption>
    </figure>
  )
}
