import { LineChart, Line, XAxis, YAxis, Tooltip, Legend, ResponsiveContainer, Brush } from 'recharts'
import { useMovingAverage } from '../hooks/useStatistics'

export default function PriceChart({ symbol, source, maType, maWindow, period }) {
  const { data, loading, error } = useMovingAverage(symbol, source, maType, maWindow, period)

  if (loading) return <div className="chart-loading">Loading...</div>
  if (error) return <div className="chart-error">{error}</div>
  if (!data || !data.points || data.points.length === 0) return <div className="chart-empty">No data</div>

  const chartData = data.points.map(p => ({
    time: new Date(p.timestamp).toLocaleString('ko-KR', { month: 'short', day: 'numeric', hour: '2-digit' }),
    price: Number(p.price),
    ma: Number(p.movingAverage),
  }))

  return (
    <div className="chart-container">
      <h3>{data.symbol} — {data.type}-{data.window} ({data.source})</h3>
      <ResponsiveContainer width="100%" height={350}>
        <LineChart data={chartData}>
          <XAxis dataKey="time" tick={{ fontSize: 11 }} interval="preserveStartEnd" />
          <YAxis domain={['auto', 'auto']} tick={{ fontSize: 11 }}
            tickFormatter={v => v.toLocaleString()} />
          <Tooltip formatter={v => v.toLocaleString()} />
          <Legend />
          <Line type="monotone" dataKey="price" stroke="#1f77b4" dot={false} strokeWidth={1.5} name="Price" />
          <Line type="monotone" dataKey="ma" stroke="#ff7f0e" dot={false} strokeWidth={2} name={`${data.type}-${data.window}`} />
          {chartData.length > 50 && <Brush dataKey="time" height={20} stroke="#8884d8" />}
        </LineChart>
      </ResponsiveContainer>
    </div>
  )
}
