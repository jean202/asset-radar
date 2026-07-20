# Asset-Radar Quick Start

## Start Everything

### Demo mode, no external API keys

```bash
cd /Users/jean325/portfolio/projects/asset-radar
docker compose -f docker-compose.yml -f docker-compose.demo.yml up --build
```

- Frontend: `http://localhost:3001`
- API Docs: `http://localhost:8081/swagger-ui/index.html`
- Grafana: `http://localhost:3000`

### Local development, 3 terminals

**Terminal 1 - Backend & Infrastructure:**
```bash
cd /Users/jean325/portfolio/projects/asset-radar
./start-dev.sh
```

**Terminal 2 - Frontend:**
```bash
cd /Users/jean325/portfolio/projects/asset-radar
./start-frontend.sh
```

**Terminal 3 - Tests/Monitoring:**
```bash
cd /Users/jean325/portfolio/projects/asset-radar
./test-e2e.sh
```

For local demo data, run Terminal 1 with:

```bash
SPRING_PROFILES_ACTIVE=local,demo ./start-dev.sh
```

## 🌐 Open in Browser

| Service | URL | Purpose |
|---------|-----|---------|
| Dashboard | `http://localhost:5173` | See real-time prices & recommendations |
| API Docs | `http://localhost:8080/swagger-ui/index.html` | API documentation |
| Grafana | `http://localhost:3000` | Metrics & monitoring |
| Prometheus | `http://localhost:9090` | Time-series metrics |

## 📊 What You'll See

### Dashboard (localhost:5173)
- **Live/Connecting** status badge (top)
- **Stats**: Asset count, sources, history size, active alerts
- **Asset Tables**: Grouped by source (Upbit, Binance, Gold, KIS, Alpha Vantage, Finnhub, or demo-generated equivalents)
  - Symbol, currency, price, 24h change (%), updated timestamp
  - Real-time updates via SSE every few seconds
- **Portfolio Recommendations**: NEW! ✨
  - Color-coded cards: STRONG_BUY (green), BUY (blue), HOLD (gray), SELL (yellow), STRONG_SELL (red)
  - Shows: symbol, action, confidence %, confidence level
  - First 2 reasons explaining the recommendation
  - Updates every 10 seconds
- **Recent Alerts**: Severity, message, timestamp

## 🔍 API Commands

```bash
# Test all endpoints
./test-e2e.sh

# Get dashboard data
curl http://localhost:8080/api/dashboard | jq '.'

# Get all recommendations
curl http://localhost:8080/api/recommendations | jq '.recommendations[] | {symbol, action, confidence}'

# Get specific symbol recommendation
curl "http://localhost:8080/api/recommendations/symbol/BTC" | jq '.recommendation'

# Get price history
curl "http://localhost:8080/api/history?symbol=BTC&source=UPBIT&limit=10" | jq '.prices[0]'

# Get analysis data
curl "http://localhost:8080/api/analysis" | jq '.analyses[0]'

# Get statistics
curl "http://localhost:8080/api/statistics/volatility?symbol=BTC&source=UPBIT&period=30d" | jq '.'

# Stream real-time prices (SSE)
curl -N http://localhost:8080/api/dashboard/stream
```

## 🎨 Recommendation Colors

```
🟢 STRONG_BUY   - Extreme downtrend, mean reversion buy signal (green)
🔵 BUY          - Weak downtrend or favorable analysis (blue)
⚪ HOLD         - Neutral sentiment, no clear direction (gray)
🟡 SELL         - Weak uptrend, reversal expected (yellow)
🔴 STRONG_SELL  - Extreme uptrend, mean reversion sell signal (red)
```

## 📈 How Recommendations Work

**Two Strategies Combined (50/50 weight each):**

1. **Momentum Strategy** (follows trends)
   - Strong UP trend (+5%+) → BUY recommendation
   - Weak UP trend (+0.5%+) → Slightly bullish
   - Strong DOWN trend (-5%-) → SELL recommendation
   - Weak DOWN trend (-0.5%-) → Slightly bearish

2. **Mean Reversion Strategy** (bets on reversal)
   - Extreme UP trend (+10%+) → SELL (expects reversal down)
   - Weak UP trend (+5%+) → Sell signal
   - Extreme DOWN trend (-10%-) → BUY (expects reversal up)
   - Weak DOWN trend (-5%-) → Buy signal

**Final Recommendation** = Weighted average of both strategies
- Confidence = 0.0 (no confidence) to 1.0 (100% confident)
- Shown as percentage: `Confidence: 72%`

## 🚨 When Recommendations Change

- Every time a new analysis is generated (typically every few seconds)
- When price moves significantly (≥0.5% for weak signals, ≥5% for strong)
- Different assets may have different recommendation timing based on volatility

## ⚙️ Webhook Notifications (Optional)

Test with local webhook server:

```bash
# Terminal 4
python3 /Users/jean325/portfolio/projects/asset-radar/test-webhook-server.py 9090

# Then in backend Terminal 1 environment:
export ASSET_RADAR_ALERT_SLACK_WEBHOOK_URL='http://localhost:9090/webhook'
export ASSET_RADAR_ALERT_DISCORD_WEBHOOK_URL='http://localhost:9090/webhook'
```

Watch alerts appear in Terminal 4 when thresholds are triggered.

## 🧪 Common Tests

```bash
# Verify all API endpoints
./test-e2e.sh

# Check if price data is flowing
curl http://localhost:8080/api/latest | jq '.prices | length'

# Watch real-time SSE updates (Ctrl+C to stop)
curl -N http://localhost:8080/api/dashboard/stream | head -20

# See active alerts
curl http://localhost:8080/api/alerts?limit=5 | jq '.alerts[].message'

# Verify docker services
docker compose ps

# Check backend logs
# (In Terminal 1 where bootRun is running, watch output)
```

## 🛑 Stop Everything

```bash
# In Terminal 1 (backend): Ctrl+C
# In Terminal 2 (frontend): Ctrl+C
# Then stop Docker:
docker compose down
```

## 🚢 Production Deploy

Production deployment is automated by GitHub Actions:

- Workflow: `.github/workflows/deploy.yml`
- Runtime compose: `deploy/docker-compose.prod.yml`
- Runtime secrets: `/opt/asset-radar/.env.prod` on the production host

See `deploy/README.md` for first-time server setup and required GitHub secrets.

## 📝 Key Files

| File | Purpose |
|------|---------|
| `src/main/java/com/jean202/assetradar/analysis/recommendation/` | Java strategy classes |
| `src/main/java/com/jean202/assetradar/api/AssetRecommendationController.java` | API endpoints |
| `src/main/java/com/jean202/assetradar/collector/DemoAssetCollector.java` | API-key-free synthetic data collector |
| `src/main/resources/application-demo.yml` | Demo profile that disables real collectors |
| `frontend/src/pages/Dashboard.jsx` | React dashboard component |
| `frontend/src/App.css` | Styling including recommendation cards |
| `deploy/README.md` | Production deployment and secret injection guide |
| `VERIFICATION_GUIDE.md` | Detailed testing walkthrough |

## 💡 Tips

- **First run**: Real collectors can take 30-60 seconds to warm up. Demo mode emits data immediately.
- **Real-time updates**: Watch recommendation cards change as prices move
- **Colors tell the story**: Green = bullish, Red = bearish, Blue = mixed signals
- **Confidence matters**: High confidence recommendations are more reliable
- **Check reasons**: Always read the 2 reasons shown - they explain the recommendation logic

## 🎓 Learn More

- See full details: `VERIFICATION_GUIDE.md`
- Architecture: `docs/architecture.md`
- API documentation: http://localhost:8080/swagger-ui/index.html
- Strategy code: `src/main/java/com/jean202/assetradar/analysis/recommendation/`
