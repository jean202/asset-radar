import { render, screen, waitFor } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import Dashboard from './Dashboard'

class MockEventSource {
  static instances = []

  constructor(url) {
    this.url = url
    this.listeners = new Map()
    this.onopen = null
    this.onerror = null
    MockEventSource.instances.push(this)
  }

  addEventListener(type, listener) {
    this.listeners.set(type, listener)
  }

  emit(type, payload) {
    const listener = this.listeners.get(type)
    if (listener) {
      listener({ data: JSON.stringify(payload) })
    }
  }

  close() {}
}

describe('Dashboard', () => {
  beforeEach(() => {
    MockEventSource.instances = []
    globalThis.EventSource = MockEventSource
    globalThis.fetch = vi.fn((url) => {
      if (url === '/api/dashboard') {
        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve({
            historyRowCount: 321,
            assets: [
              {
                symbol: 'BTC',
                quoteCurrency: 'KRW',
                source: 'UPBIT',
                price: 137500000,
                signedChangeRate: 0.023,
                collectedAt: '2026-04-04T00:00:00Z',
              },
            ],
          }),
        })
      }

      if (url === '/api/alerts?limit=10') {
        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve({
            alerts: [
              {
                severity: 'WARN',
                message: 'BTC moved sharply',
                alertedAt: '2026-04-04T00:01:00Z',
              },
            ],
          }),
        })
      }

      return Promise.reject(new Error(`Unhandled fetch: ${url}`))
    })
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('renders fetched assets and alerts and updates from SSE', async () => {
    render(<Dashboard />)

    expect(await screen.findByText('BTC')).toBeInTheDocument()
    expect(await screen.findByText('BTC moved sharply')).toBeInTheDocument()
    expect(screen.getByText('321')).toBeInTheDocument()

    const stream = MockEventSource.instances[0]
    stream.emit('asset-price', {
      symbol: 'ETH',
      quoteCurrency: 'KRW',
      source: 'UPBIT',
      price: 5120000,
      signedChangeRate: -0.008,
      collectedAt: '2026-04-04T00:02:00Z',
    })

    await waitFor(() => {
      expect(screen.getByText('ETH')).toBeInTheDocument()
    })
  })
})
