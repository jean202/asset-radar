import { useSummary } from '../hooks/useStatistics'

function fmt(v, decimals = 2) {
  if (v == null) return '-'
  return Number(v).toLocaleString(undefined, { maximumFractionDigits: decimals })
}

function pct(v) {
  if (v == null) return '-'
  return (Number(v) * 100).toFixed(2) + '%'
}

export default function SummaryCards({ symbol, source, period }) {
  const { data, loading, error } = useSummary(symbol, source, period)

  if (loading) return <div className="chart-loading">Loading...</div>
  if (error) return <div className="chart-error">{error}</div>
  if (!data) return <div className="chart-empty">No data</div>

  const cards = [
    { label: 'Latest', value: fmt(data.latestPrice) },
    { label: 'Return', value: pct(data.returnRate), className: Number(data.returnRate) >= 0 ? 'positive' : 'negative' },
    { label: 'Min', value: fmt(data.min) },
    { label: 'Max', value: fmt(data.max) },
    { label: 'Mean', value: fmt(data.mean) },
    { label: 'Std Dev', value: fmt(data.stdDev, 4) },
    { label: 'Median', value: fmt(data.median) },
    { label: 'Max DD', value: pct(data.maxDrawdown), className: 'negative' },
    { label: 'P5', value: fmt(data.p5) },
    { label: 'P25', value: fmt(data.p25) },
    { label: 'P75', value: fmt(data.p75) },
    { label: 'P95', value: fmt(data.p95) },
    { label: 'Data Points', value: data.dataPoints?.toLocaleString() },
  ]

  return (
    <div className="chart-container">
      <h3>{data.symbol} — Summary ({data.source})</h3>
      <div className="summary-grid">
        {cards.map(c => (
          <div className="summary-card" key={c.label}>
            <div className="summary-label">{c.label}</div>
            <div className={`summary-value ${c.className || ''}`}>{c.value}</div>
          </div>
        ))}
      </div>
    </div>
  )
}
