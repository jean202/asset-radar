import { useState, useEffect } from 'react'

const API_BASE = '/api/statistics'

function useFetch(url) {
  const [result, setResult] = useState({
    url: null,
    data: null,
    error: null,
  })

  useEffect(() => {
    if (!url) {
      return undefined
    }

    const controller = new AbortController()

    fetch(url, { signal: controller.signal })
      .then(r => {
        if (!r.ok) throw new Error(`HTTP ${r.status}`)
        return r.json()
      })
      .then(data => setResult({ url, data, error: null }))
      .catch(error => {
        if (error.name === 'AbortError') {
          return
        }
        setResult({ url, data: null, error: error.message })
      })

    return () => controller.abort()
  }, [url])

  if (!url) {
    return { data: null, loading: false, error: null }
  }

  if (result.url !== url) {
    return { data: null, loading: true, error: null }
  }

  return { data: result.data, loading: false, error: result.error }
}

export function useMovingAverage(symbol, source, type = 'SMA', window = 20, period = '30d') {
  const params = new URLSearchParams({ symbol, type, window, period })
  if (source) params.set('source', source)
  return useFetch(symbol ? `${API_BASE}/moving-average?${params}` : null)
}

export function useVolatility(symbol, source, window = 20, period = '30d') {
  const params = new URLSearchParams({ symbol, window, period })
  if (source) params.set('source', source)
  return useFetch(symbol ? `${API_BASE}/volatility?${params}` : null)
}

export function useCorrelation(assets, period = '30d') {
  const params = new URLSearchParams({ period })
  if (assets && assets.length >= 2) {
    assets.forEach(a => params.append('assets', a))
  }
  return useFetch(assets && assets.length >= 2 ? `${API_BASE}/correlation?${params}` : null)
}

export function useSummary(symbol, source, period = '30d') {
  const params = new URLSearchParams({ symbol, period })
  if (source) params.set('source', source)
  return useFetch(symbol ? `${API_BASE}/summary?${params}` : null)
}
