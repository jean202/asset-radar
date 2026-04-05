import { AreaChart, Area, XAxis, YAxis, Tooltip, ResponsiveContainer, Legend } from 'recharts'
import { useVolatility } from '../hooks/useStatistics'

export default function VolatilityChart({ symbol, source, window: volWindow, period }) {
  const { data, loading, error } = useVolatility(symbol, source, volWindow, period)

  if (loading) return <div className="chart-loading">Loading...</div>
  if (error) return <div className="chart-error">{error}</div>
  if (!data || !data.points || data.points.length === 0) return <div className="chart-empty">No data</div>

  const chartData = data.points.map(p => ({
    time: new Date(p.timestamp).toLocaleString('ko-KR', { month: 'short', day: 'numeric', hour: '2-digit' }),
    volatility: Number(p.volatility),
  }))

  return (
    <div className="chart-container">
      <h3>{data.symbol} — Rolling Volatility (window: {data.window})</h3>
      <div className="vol-stats">
        <span>Current: {Number(data.currentVolatility).toFixed(6)}</span>
        <span>Annualized: {(Number(data.annualizedVolatility) * 100).toFixed(2)}%</span>
      </div>
      <ResponsiveContainer width="100%" height={300}>
        <AreaChart data={chartData}>
          <XAxis dataKey="time" tick={{ fontSize: 11 }} interval="preserveStartEnd" />
          <YAxis tick={{ fontSize: 11 }} tickFormatter={v => v.toFixed(4)} />
          <Tooltip formatter={v => v.toFixed(6)} />
          <Legend />
          <Area type="monotone" dataKey="volatility" stroke="#d62728" fill="#ff9896"
            fillOpacity={0.3} name="Volatility" />
        </AreaChart>
      </ResponsiveContainer>
    </div>
  )
}
