import { render, screen } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import Analytics from './Analytics'

vi.mock('../components/PriceChart', () => ({
  default: () => <div>Price Chart</div>,
}))

vi.mock('../components/VolatilityChart', () => ({
  default: () => <div>Volatility Chart</div>,
}))

vi.mock('../components/SummaryCards', () => ({
  default: () => <div>Summary Cards</div>,
}))

vi.mock('../components/CorrelationMatrix', () => ({
  default: ({ assets }) => <div>Correlation Matrix: {assets.length}</div>,
}))

describe('Analytics', () => {
  beforeEach(() => {
    globalThis.fetch = vi.fn(() =>
      Promise.resolve({
        ok: true,
        json: () => Promise.resolve({
          assets: [
            { symbol: 'BTC', source: 'UPBIT', quoteCurrency: 'KRW' },
            { symbol: 'ETH', source: 'UPBIT', quoteCurrency: 'KRW' },
          ],
        }),
      }),
    )
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('loads assets and renders analytics widgets for the default selection', async () => {
    render(<Analytics />)

    expect(await screen.findByDisplayValue('BTC (UPBIT)')).toBeInTheDocument()
    expect(screen.getByText('Summary Cards')).toBeInTheDocument()
    expect(screen.getByText('Price Chart')).toBeInTheDocument()
    expect(screen.getByText('Volatility Chart')).toBeInTheDocument()
    expect(screen.getByText('Correlation Matrix: 2')).toBeInTheDocument()
  })
})
