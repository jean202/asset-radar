import { useCorrelation } from '../hooks/useStatistics'

function corrColor(value) {
  const v = Number(value)
  if (v > 0) {
    const intensity = Math.min(Math.round(v * 255), 255)
    return `rgb(${255 - intensity}, ${255 - intensity / 2}, ${255 - intensity})`
  }
  if (v < 0) {
    const intensity = Math.min(Math.round(Math.abs(v) * 255), 255)
    return `rgb(${255 - intensity / 2}, ${255 - intensity / 2}, 255)`
  }
  return '#fff'
}

export default function CorrelationMatrix({ assets, period }) {
  const { data, loading, error } = useCorrelation(assets, period)

  if (loading) return <div className="chart-loading">Loading...</div>
  if (error) return <div className="chart-error">{error}</div>
  if (!data || !data.pairs || data.pairs.length === 0) return <div className="chart-empty">Need 2+ assets</div>

  return (
    <div className="chart-container">
      <h3>Cross-Asset Correlation</h3>
      <table className="corr-table">
        <thead>
          <tr>
            <th>Asset A</th>
            <th>Asset B</th>
            <th>Correlation</th>
            <th>Samples</th>
          </tr>
        </thead>
        <tbody>
          {data.pairs.map((p, i) => (
            <tr key={i}>
              <td>{p.symbolA} ({p.sourceA})</td>
              <td>{p.symbolB} ({p.sourceB})</td>
              <td style={{ backgroundColor: corrColor(p.correlation), fontWeight: 'bold', textAlign: 'center' }}>
                {Number(p.correlation).toFixed(4)}
              </td>
              <td style={{ textAlign: 'center' }}>{p.overlappingSamples}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}
