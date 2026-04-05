import { useState, useEffect } from 'react'
import PriceChart from '../components/PriceChart'
import VolatilityChart from '../components/VolatilityChart'
import CorrelationMatrix from '../components/CorrelationMatrix'
import SummaryCards from '../components/SummaryCards'

const PERIODS = ['1d', '7d', '14d', '30d']
const MA_TYPES = ['SMA', 'EMA']
const MA_WINDOWS = [5, 10, 20, 50]

export default function Analytics() {
  const [assets, setAssets] = useState([])
  const [selectedAsset, setSelectedAsset] = useState(null)
  const [period, setPeriod] = useState('30d')
  const [maType, setMaType] = useState('SMA')
  const [maWindow, setMaWindow] = useState(20)
  const [volWindow, setVolWindow] = useState(20)

  // Fetch available assets from dashboard
  useEffect(() => {
    fetch('/api/dashboard')
      .then(r => r.json())
      .then(data => {
        const list = (data.assets || []).map(a => ({
          symbol: a.symbol,
          source: a.source,
          label: `${a.symbol} (${a.source})`,
          spec: `${a.source}:${a.quoteCurrency}:${a.symbol}`,
        }))
        setAssets(list)
        setSelectedAsset(current => current ?? list[0] ?? null)
      })
      .catch(() => {})
  }, [])

  const correlationAssets = assets.map(a => a.spec)

  return (
    <div className="analytics-page">
      <div className="controls-bar">
        <div className="control-group">
          <label>Asset</label>
          <select value={selectedAsset?.label || ''}
            onChange={e => setSelectedAsset(assets.find(a => a.label === e.target.value))}>
            {assets.map(a => <option key={a.label} value={a.label}>{a.label}</option>)}
          </select>
        </div>

        <div className="control-group">
          <label>Period</label>
          <div className="button-group">
            {PERIODS.map(p => (
              <button key={p} className={period === p ? 'active' : ''} onClick={() => setPeriod(p)}>
                {p}
              </button>
            ))}
          </div>
        </div>

        <div className="control-group">
          <label>MA Type</label>
          <div className="button-group">
            {MA_TYPES.map(t => (
              <button key={t} className={maType === t ? 'active' : ''} onClick={() => setMaType(t)}>
                {t}
              </button>
            ))}
          </div>
        </div>

        <div className="control-group">
          <label>MA Window</label>
          <select value={maWindow} onChange={e => setMaWindow(Number(e.target.value))}>
            {MA_WINDOWS.map(w => <option key={w} value={w}>{w}</option>)}
          </select>
        </div>

        <div className="control-group">
          <label>Vol Window</label>
          <select value={volWindow} onChange={e => setVolWindow(Number(e.target.value))}>
            {MA_WINDOWS.map(w => <option key={w} value={w}>{w}</option>)}
          </select>
        </div>
      </div>

      {selectedAsset && (
        <>
          <SummaryCards symbol={selectedAsset.symbol} source={selectedAsset.source} period={period} />
          <PriceChart symbol={selectedAsset.symbol} source={selectedAsset.source}
            maType={maType} maWindow={maWindow} period={period} />
          <VolatilityChart symbol={selectedAsset.symbol} source={selectedAsset.source}
            window={volWindow} period={period} />
        </>
      )}

      {correlationAssets.length >= 2 && (
        <CorrelationMatrix assets={correlationAssets} period={period} />
      )}
    </div>
  )
}
