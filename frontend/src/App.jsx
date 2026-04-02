import { useState, useEffect, useRef, useCallback } from 'react'
import './App.css'

const API_BASE = '/api'

function formatPrice(price, quoteCurrency) {
  if (price == null) return '-'
  const num = Number(price)
  if (quoteCurrency === 'KRW') {
    return num.toLocaleString('ko-KR', { maximumFractionDigits: 0 }) + ' KRW'
  }
  return '$' + num.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function formatChange(rate) {
  if (rate == null) return '-'
  const pct = (Number(rate) * 100).toFixed(2)
  const sign = pct > 0 ? '+' : ''
  return sign + pct + '%'
}

function changeClass(rate) {
  if (rate == null) return 'neutral'
  const v = Number(rate)
  if (v > 0) return 'positive'
  if (v < 0) return 'negative'
  return 'neutral'
}

function timeAgo(instant) {
  if (!instant) return '-'
  const diff = Math.floor((Date.now() - new Date(instant).getTime()) / 1000)
  if (diff < 5) return 'just now'
  if (diff < 60) return diff + 's ago'
  if (diff < 3600) return Math.floor(diff / 60) + 'm ago'
  if (diff < 86400) return Math.floor(diff / 3600) + 'h ago'
  return Math.floor(diff / 86400) + 'd ago'
}

function App() {
  const [dashboard, setDashboard] = useState(null)
  const [alerts, setAlerts] = useState([])
  const [sseStatus, setSseStatus] = useState('connecting')
  const pricesRef = useRef(new Map())
  const [, forceUpdate] = useState(0)

  const fetchDashboard = useCallback(() => {
    fetch(API_BASE + '/dashboard')
      .then(r => r.json())
      .then(data => {
        setDashboard(data)
        data.assets?.forEach(a => {
          pricesRef.current.set(a.source + ':' + a.symbol, a)
        })
      })
      .catch(() => {})
  }, [])

  const fetchAlerts = useCallback(() => {
    fetch(API_BASE + '/alerts?limit=10')
      .then(r => r.json())
      .then(data => setAlerts(data.alerts || []))
      .catch(() => {})
  }, [])

  useEffect(() => {
    fetchDashboard()
    fetchAlerts()
    const interval = setInterval(() => {
      fetchAlerts()
      forceUpdate(n => n + 1)
    }, 10000)
    return () => clearInterval(interval)
  }, [fetchDashboard, fetchAlerts])

  useEffect(() => {
    const es = new EventSource(API_BASE + '/dashboard/stream')

    es.addEventListener('asset-price', (e) => {
      try {
        const price = JSON.parse(e.data)
        pricesRef.current.set(price.source + ':' + price.symbol, price)
        forceUpdate(n => n + 1)
      } catch {}
    })

    es.onopen = () => setSseStatus('live')
    es.onerror = () => setSseStatus('connecting')

    return () => es.close()
  }, [])

  const prices = Array.from(pricesRef.current.values())
  const groups = groupBySource(prices)
  const assetCount = prices.length
  const sourceCount = groups.length

  return (
    <>
      <div className="header">
        <h1>Asset Radar</h1>
        <div className="status-badge">
          <span className={'status-dot ' + sseStatus}></span>
          {sseStatus === 'live' ? 'Live' : 'Connecting...'}
        </div>
      </div>

      <div className="stats-row">
        <div className="stat-card">
          <div className="label">Assets</div>
          <div className="value">{assetCount}</div>
        </div>
        <div className="stat-card">
          <div className="label">Sources</div>
          <div className="value">{sourceCount}</div>
        </div>
        <div className="stat-card">
          <div className="label">History Rows</div>
          <div className="value">{dashboard?.historyRowCount?.toLocaleString() ?? '-'}</div>
        </div>
        <div className="stat-card">
          <div className="label">Alerts</div>
          <div className="value">{alerts.length}</div>
        </div>
      </div>

      <div className="source-groups">
        {groups.length === 0 && (
          <div className="empty-state">
            Waiting for price data...
          </div>
        )}
        {groups.map(g => (
          <div className="source-group" key={g.source}>
            <div className="source-group-header">
              <span className="source-name">{g.displayName}</span>
              <div className="source-meta">
                <span>{g.typeLabel}</span>
                <span>{g.assets.length} assets</span>
                {g.stale && <span className="stale-tag">stale</span>}
              </div>
            </div>
            <table className="asset-table">
              <thead>
                <tr>
                  <th>Symbol</th>
                  <th>Currency</th>
                  <th>Price</th>
                  <th>Change</th>
                  <th>Updated</th>
                </tr>
              </thead>
              <tbody>
                {g.assets.map(a => (
                  <tr key={a.symbol}>
                    <td className="symbol-cell">{a.symbol}</td>
                    <td>{a.quoteCurrency}</td>
                    <td className="price-cell">{formatPrice(a.price, a.quoteCurrency)}</td>
                    <td className={'change-cell ' + changeClass(a.signedChangeRate)}>
                      {formatChange(a.signedChangeRate)}
                    </td>
                    <td className="time-cell">{timeAgo(a.collectedAt)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ))}
      </div>

      {alerts.length > 0 && (
        <div className="alerts-section">
          <h2>Recent Alerts</h2>
          <div className="alert-list">
            {alerts.map((a, i) => (
              <div className="alert-item" key={i}>
                <span className={'severity-badge ' + a.severity}>{a.severity}</span>
                <span className="alert-message">{a.message}</span>
                <span className="alert-time">{timeAgo(a.alertedAt)}</span>
              </div>
            ))}
          </div>
        </div>
      )}

      <div className="footer">
        <span>Asset Radar v0.1.0</span>
        <a href="/swagger-ui.html">API Docs</a>
      </div>
    </>
  )
}

const SOURCE_META = {
  UPBIT: { displayName: 'Upbit', typeLabel: 'Coin' },
  BINANCE: { displayName: 'Binance', typeLabel: 'Coin' },
  GOLDAPI: { displayName: 'Gold API', typeLabel: 'Gold' },
  KIS: { displayName: 'KIS', typeLabel: 'Stock KR' },
  KOREAINVESTMENT: { displayName: 'KIS', typeLabel: 'Stock KR' },
  ALPHAVANTAGE: { displayName: 'Alpha Vantage', typeLabel: 'Stock US' },
  FINNHUB: { displayName: 'Finnhub', typeLabel: 'Stock US' },
}

function groupBySource(prices) {
  const map = new Map()
  for (const p of prices) {
    const key = p.source || 'UNKNOWN'
    if (!map.has(key)) {
      const meta = SOURCE_META[key] || { displayName: key, typeLabel: 'Other' }
      map.set(key, { source: key, ...meta, assets: [], stale: false })
    }
    map.get(key).assets.push(p)
  }
  for (const g of map.values()) {
    const latest = g.assets.reduce((max, a) =>
      a.collectedAt > max ? a.collectedAt : max, '')
    const age = latest ? (Date.now() - new Date(latest).getTime()) / 1000 : Infinity
    g.stale = age > 300
    g.assets.sort((a, b) => a.symbol.localeCompare(b.symbol))
  }
  return Array.from(map.values())
}

export default App
