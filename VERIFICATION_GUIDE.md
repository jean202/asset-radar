# Asset-Radar Portfolio Recommendations - Verification Guide

This guide walks through end-to-end verification of the portfolio recommendation system.

## 📋 Prerequisites

- Docker Desktop running
- Java 17+ installed
- Node.js 18+ installed
- Backend already built with: `./gradlew build`

## 🚀 Step 1: Start Infrastructure & Backend

Open **Terminal 1** and run:

```bash
cd /Users/jean325/portfolio/projects/asset-radar
./start-dev.sh
```

To verify the full flow without external API keys, use the demo profile:

```bash
SPRING_PROFILES_ACTIVE=local,demo ./start-dev.sh
```

This will:
1. Start all Docker services (Kafka, Redis, PostgreSQL, Prometheus, Grafana, Loki, Tempo, Alertmanager)
2. Wait 30 seconds for services to be ready
3. Start the Spring Boot backend

**Expected output:**
```
Started AssetRadarApplication in X.XXX seconds
```

**Services available at:**
- Backend API: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- Grafana: `http://localhost:3000`
- Prometheus: `http://localhost:9090`
- Loki: `http://localhost:3100`

## 🎨 Step 2: Start Frontend

Open **Terminal 2** and run:

```bash
cd /Users/jean325/portfolio/projects/asset-radar
./start-frontend.sh
```

**Expected output:**
```
  VITE v5.X.X  ready in XXX ms

  ➜  Local:   http://localhost:5173/
  ➜  press h to show help
```

## ✅ Step 3: Verify API Endpoints

Open **Terminal 3** and run the test suite:

```bash
cd /Users/jean325/portfolio/projects/asset-radar
chmod +x test-e2e.sh
./test-e2e.sh
```

**Expected tests:**
1. ✅ Dashboard endpoint responding
2. ✅ Recommendations endpoint responding
3. ✅ SSE stream responding
4. ✅ Price data available (or warming up)
5. ✅ Alerts endpoint responding
6. ✅ Analysis endpoint responding

**Sample curl commands if manual testing:**

```bash
# Check dashboard
curl http://localhost:8080/api/dashboard | jq '.assets | length'

# Check recommendations
curl http://localhost:8080/api/recommendations | jq '.recommendations[0]'

# Check specific asset recommendations
curl "http://localhost:8080/api/recommendations?source=UPBIT&quoteCurrency=KRW" | jq '.'

# Get recommendations for a specific symbol
curl "http://localhost:8080/api/recommendations/symbol/BTC" | jq '.'

# Check alerts
curl http://localhost:8080/api/alerts?limit=3 | jq '.alerts[0]'
```

## 🌐 Step 4: View Frontend Dashboard

1. Open browser to `http://localhost:5173`
2. You should see:
   - **Status badge**: "Live" or "Connecting..." (SSE connection status)
   - **Stats row**: Asset count, sources, history rows, alerts count
   - **Source groups**: Assets grouped by source (Upbit, Binance, Gold API, KIS, Alpha Vantage, Finnhub, or demo-generated equivalents)
     - Each group shows: asset symbol, price, 24h change (color-coded ✅ green / ❌ red)
     - Updated timestamp (e.g., "just now", "5m ago")
   - **Portfolio Recommendations section** (if any price data exists):
     - Cards showing symbol, action (color-coded), confidence %, confidence level
     - First 2 reasons explaining the recommendation
     - "Analyzed X minutes ago" timestamp
   - **Recent Alerts**: Severity level, message, timestamp

## 📊 Step 5: Monitor Price Data & Recommendations

### Scenario: Real-time price updates

1. Keep dashboard open in browser
2. Watch the **Source Groups** section
   - Prices update in real-time via SSE stream
   - "Updated X seconds ago" changes
   - Change percentages recalculate
   - Row highlighting on hover

3. Watch the **Portfolio Recommendations** section
   - Automatically fetches every 10 seconds
   - Updates when underlying analysis changes
   - Color changes as sentiment changes (STRONG_BUY → BUY → HOLD, etc.)

### Check individual recommendation

```bash
# Get single symbol recommendation
curl "http://localhost:8080/api/recommendations/symbol/BTC" | jq '.recommendation | {symbol, action, confidence, confidenceLevel}'

# Expected output:
# {
#   "symbol": "BTC",
#   "action": "BUY",
#   "confidence": 0.72,
#   "confidenceLevel": "medium"
# }
```

## 🔔 Step 6: Verify Webhook Notifications (Optional)

If you have Slack/Discord webhooks configured:

1. Set environment variables (before starting backend):

```bash
export ASSET_RADAR_ALERT_SLACK_WEBHOOK_URL='https://hooks.slack.com/services/YOUR/WEBHOOK/URL'
export ASSET_RADAR_ALERT_DISCORD_WEBHOOK_URL='https://discordapp.com/api/webhooks/YOUR/WEBHOOK'
export ASSET_RADAR_ALERT_NOTIFIER_SLACK_ENABLED=true
export ASSET_RADAR_ALERT_NOTIFIER_DISCORD_ENABLED=true
```

2. Or test with local webhook server on port 9090:

```bash
# Terminal 4
cd /Users/jean325/portfolio/projects/asset-radar
python3 test-webhook-server.py 9090
```

Then set:
```bash
export ASSET_RADAR_ALERT_SLACK_WEBHOOK_URL='http://localhost:9090/webhook'
export ASSET_RADAR_ALERT_DISCORD_WEBHOOK_URL='http://localhost:9090/webhook'
```

3. Monitor webhook server output - you should see alerts as they trigger:

```
================================================================================
⏰ HH:MM:SS | 🔔 웹훅 수신!
📍 경로: /webhook

📦 페이로드:
{
  "severity": "WARN",
  "message": "BTC price moved 5.2%",
  "alertedAt": "2026-05-03T10:30:45Z"
}
================================================================================
```

## 📈 Step 7: Check Analysis & Statistics

```bash
# Get latest analysis
curl "http://localhost:8080/api/analysis" | jq '.analyses[0]'

# Get analysis history for specific asset
curl "http://localhost:8080/api/analysis/history?symbol=BTC&source=UPBIT&quoteCurrency=KRW&limit=5" | jq '.analyses[0]'

# Get moving average
curl "http://localhost:8080/api/statistics/moving-average?symbol=BTC&source=UPBIT&period=30d&type=SMA&window=20" | jq '.'

# Get volatility
curl "http://localhost:8080/api/statistics/volatility?symbol=BTC&source=UPBIT&period=30d&window=20" | jq '.'

# Get correlation between assets
curl "http://localhost:8080/api/statistics/correlation?assets=UPBIT:KRW:BTC&assets=UPBIT:KRW:ETH&period=30d" | jq '.'
```

## 🎯 What to Expect

### Recommendation Actions & Colors

- 🟢 **STRONG_BUY** (Green): High confidence to buy - extreme downtrend
- 🔵 **BUY** (Blue): Moderate confidence to buy - weak downtrend or mean reversion
- ⚪ **HOLD** (Gray): Neutral sentiment - normal range
- 🟡 **SELL** (Yellow): Weak to moderate sell signal - uptrend reversal expected
- 🔴 **STRONG_SELL** (Red): High confidence to sell - extreme uptrend

### When Recommendations Change

Recommendations update when:
- Price changes ≥ 0.5% (weak signals)
- Price changes ≥ 5% (strong signals)
- Price changes ≥ 10% (extreme signals)
- New analysis data is generated (typically every price update)

The engine combines two strategies with equal weight (0.5 each):

**Momentum Strategy** (follows the trend):
- Large uptrend → BUY/STRONG_BUY
- Large downtrend → SELL/STRONG_SELL

**Mean Reversion Strategy** (bets on reversal):
- Extreme uptrend → SELL/STRONG_SELL (expects reversal down)
- Extreme downtrend → BUY/STRONG_BUY (expects reversal up)

Final recommendation is the weighted combination with confidence score.

## 🧪 Troubleshooting

### "No price data yet"
- Collectors are warming up on first run (takes 1-2 minutes)
- Use `SPRING_PROFILES_ACTIVE=local,demo ./start-dev.sh` if you need immediate synthetic data without API keys
- Check collector logs in backend terminal
- Verify Kafka is running: `docker compose ps | grep kafka`

### Recommendations not appearing
- Check that `/api/analysis` returns data
- Verify `/api/recommendations` endpoint is responding
- Check browser console for fetch errors
- Verify frontend is accessing correct backend URL (port 8080)

### Webhooks not firing
- Verify backend started with webhook env vars set
- Check Slack/Discord webhook URL is correct
- Ensure alert severity meets notification threshold
  - Slack: CRITICAL only
  - Discord: WARN and above

### SSE stream not updating
- Check frontend console for connection status
- Verify Kafka topic `asset.price.realtime` has messages
- Restart backend if stream hangs

## 📊 Monitoring Dashboards

While testing, check operational dashboards:

- **Grafana** (http://localhost:3000)
  - Username: admin, Password: admin
  - Dashboards show: JVM metrics, request rates, Kafka lag

- **Prometheus** (http://localhost:9090)
  - Search for metrics: `asset_price`, `asset_recommendation`, `asset_alert`

- **Alertmanager** (http://localhost:9093)
  - View any triggered alerts

## ✨ Success Criteria

- ✅ Backend API endpoints all responding
- ✅ Frontend dashboard displays real-time prices via SSE
- ✅ Recommendations section renders with color-coded actions
- ✅ Recommendations update every 10 seconds
- ✅ Confidence scores and reasons display correctly
- ✅ (Optional) Webhooks fire when alerts are triggered
- ✅ Analytics page loads and shows charts

## 🔗 Next Steps

After verification:
1. Capture Grafana/Prometheus/Loki operational evidence for the portfolio README
2. Configure monitoring alerts in Alertmanager
3. Add more data sources if needed
4. Fine-tune strategy weights based on actual results

Production deployment is defined in `deploy/README.md`. Runtime secrets are injected from the production host's `.env.prod`, not from the image build.
