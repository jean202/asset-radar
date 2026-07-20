package com.jean202.assetradar.api;

final class OpenApiExamples {
    static final String ERROR_INVALID_REQUEST = """
            {
              "timestamp": "2026-05-17T10:15:30Z",
              "status": 400,
              "error": "Bad Request",
              "code": "INVALID_REQUEST",
              "message": "symbol is invalid",
              "path": "/api/history"
            }
            """;

    static final String ERROR_INVALID_PERIOD = """
            {
              "timestamp": "2026-05-17T10:15:30Z",
              "status": 400,
              "error": "Bad Request",
              "code": "INVALID_REQUEST",
              "message": "period must be one of Ns, Nm, Nh, Nd, Nw",
              "path": "/api/compare"
            }
            """;

    static final String ERROR_INVALID_STATISTICS_PERIOD = """
            {
              "timestamp": "2026-05-17T10:15:30Z",
              "status": 400,
              "error": "Bad Request",
              "code": "INVALID_REQUEST",
              "message": "period must be one of Ns, Nm, Nh, Nd, Nw",
              "path": "/api/statistics/moving-average"
            }
            """;

    static final String DASHBOARD = """
            {
              "status": "live",
              "updatedAt": "2026-05-17T10:15:02Z",
              "assetCount": 2,
              "historyRowCount": 4821,
              "historyUpdatedAt": "2026-05-17T10:15:02Z",
              "assets": [
                {
                  "symbol": "BTC",
                  "name": "Bitcoin",
                  "quoteCurrency": "KRW",
                  "source": "UPBIT",
                  "price": 137500000,
                  "signedChangeRate": 0.018,
                  "collectedAt": "2026-05-17T10:15:02Z"
                }
              ],
              "sourceGroups": [
                {
                  "source": "UPBIT",
                  "sourceDisplayName": "업비트",
                  "assetType": "COIN",
                  "assetTypeLabel": "코인",
                  "updatedAt": "2026-05-17T10:15:02Z",
                  "stale": false,
                  "lastUpdatedAgeSeconds": 1,
                  "lastUpdatedAgo": "1s ago",
                  "assetCount": 1,
                  "assets": [
                    {
                      "symbol": "BTC",
                      "name": "Bitcoin",
                      "quoteCurrency": "KRW",
                      "source": "UPBIT",
                      "price": 137500000,
                      "signedChangeRate": 0.018,
                      "collectedAt": "2026-05-17T10:15:02Z"
                    }
                  ]
                }
              ]
            }
            """;

    static final String DASHBOARD_STREAM = """
            id: UPBIT-KRW-BTC-1779012902000
            event: asset-price
            data: {"symbol":"BTC","name":"Bitcoin","quoteCurrency":"KRW","source":"UPBIT","price":137500000,"signedChangeRate":0.018,"collectedAt":"2026-05-17T10:15:02Z"}

            """;

    static final String LATEST_ASSETS = """
            {
              "source": "UPBIT",
              "quoteCurrency": "KRW",
              "count": 2,
              "assets": [
                {
                  "symbol": "BTC",
                  "name": "Bitcoin",
                  "quoteCurrency": "KRW",
                  "source": "UPBIT",
                  "price": 137500000,
                  "signedChangeRate": 0.018,
                  "collectedAt": "2026-05-17T10:15:02Z"
                },
                {
                  "symbol": "ETH",
                  "name": "Ethereum",
                  "quoteCurrency": "KRW",
                  "source": "UPBIT",
                  "price": 5120000,
                  "signedChangeRate": 0.012,
                  "collectedAt": "2026-05-17T10:15:01Z"
                }
              ]
            }
            """;

    static final String ASSET_HISTORY = """
            {
              "symbol": "BTC",
              "source": "UPBIT",
              "quoteCurrency": "KRW",
              "from": "2026-05-17T09:15:00Z",
              "to": "2026-05-17T10:15:00Z",
              "count": 2,
              "prices": [
                {
                  "symbol": "BTC",
                  "name": "Bitcoin",
                  "quoteCurrency": "KRW",
                  "source": "UPBIT",
                  "price": 136900000,
                  "signedChangeRate": 0.004,
                  "collectedAt": "2026-05-17T09:15:00Z"
                },
                {
                  "symbol": "BTC",
                  "name": "Bitcoin",
                  "quoteCurrency": "KRW",
                  "source": "UPBIT",
                  "price": 137500000,
                  "signedChangeRate": 0.018,
                  "collectedAt": "2026-05-17T10:15:00Z"
                }
              ]
            }
            """;

    static final String COMPARE = """
            {
              "from": "2026-04-17T10:15:00Z",
              "to": "2026-05-17T10:15:00Z",
              "baseAmount": 1000000,
              "requestedCount": 2,
              "count": 2,
              "projectedValueComparable": false,
              "quoteCurrencies": ["KRW", "USD"],
              "projectedValueWarning": "projectedValue is only comparable when all assets share the same quoteCurrency",
              "comparisons": [
                {
                  "symbol": "BTC",
                  "source": "UPBIT",
                  "quoteCurrency": "KRW",
                  "startedAt": "2026-04-17T10:15:00Z",
                  "endedAt": "2026-05-17T10:15:00Z",
                  "sampleCount": 720,
                  "startPrice": 125000000,
                  "endPrice": 137500000,
                  "priceChange": 12500000,
                  "returnRate": 0.10000000,
                  "movement": "UP",
                  "projectedValue": null
                }
              ]
            }
            """;

    static final String ANALYSIS = """
            {
              "source": "UPBIT",
              "quoteCurrency": "KRW",
              "count": 1,
              "analyses": [
                {
                  "symbol": "BTC",
                  "quoteCurrency": "KRW",
                  "source": "UPBIT",
                  "currentPrice": 137500000,
                  "previousPrice": 136900000,
                  "priceChange": 600000,
                  "changeRate": 0.00438276,
                  "movement": "UP",
                  "analyzedAt": "2026-05-17T10:15:03Z"
                }
              ]
            }
            """;

    static final String ANALYSIS_HISTORY = """
            {
              "symbol": "BTC",
              "source": "UPBIT",
              "quoteCurrency": "KRW",
              "from": "2026-05-17T09:15:00Z",
              "to": "2026-05-17T10:15:00Z",
              "count": 1,
              "analyses": [
                {
                  "symbol": "BTC",
                  "quoteCurrency": "KRW",
                  "source": "UPBIT",
                  "currentPrice": 137500000,
                  "previousPrice": 136900000,
                  "priceChange": 600000,
                  "changeRate": 0.00438276,
                  "movement": "UP",
                  "analyzedAt": "2026-05-17T10:15:03Z"
                }
              ]
            }
            """;

    static final String ALERTS = """
            {
              "source": "UPBIT",
              "quoteCurrency": "KRW",
              "count": 1,
              "alerts": [
                {
                  "symbol": "BTC",
                  "quoteCurrency": "KRW",
                  "source": "UPBIT",
                  "alertType": "PRICE_CHANGE",
                  "severity": "WARN",
                  "movement": "UP",
                  "currentPrice": 137500000,
                  "previousPrice": 134500000,
                  "priceChange": 3000000,
                  "changeRate": 0.02230483,
                  "thresholdRate": 0.01,
                  "baselineAt": "2026-05-17T10:14:00Z",
                  "windowSeconds": 60,
                  "message": "BTC moved 2.23% in 60 seconds",
                  "alertedAt": "2026-05-17T10:15:03Z"
                }
              ]
            }
            """;

    static final String ALERT_HISTORY = """
            {
              "symbol": "BTC",
              "source": "UPBIT",
              "quoteCurrency": "KRW",
              "from": "2026-05-17T09:15:00Z",
              "to": "2026-05-17T10:15:00Z",
              "count": 1,
              "alerts": [
                {
                  "symbol": "BTC",
                  "quoteCurrency": "KRW",
                  "source": "UPBIT",
                  "alertType": "PRICE_CHANGE",
                  "severity": "WARN",
                  "movement": "UP",
                  "currentPrice": 137500000,
                  "previousPrice": 134500000,
                  "priceChange": 3000000,
                  "changeRate": 0.02230483,
                  "thresholdRate": 0.01,
                  "baselineAt": "2026-05-17T10:14:00Z",
                  "windowSeconds": 60,
                  "message": "BTC moved 2.23% in 60 seconds",
                  "alertedAt": "2026-05-17T10:15:03Z"
                }
              ]
            }
            """;

    static final String RECOMMENDATIONS = """
            {
              "recommendations": [
                {
                  "symbol": "BTC",
                  "action": "BUY",
                  "actionLabel": "매수",
                  "confidence": 0.72,
                  "confidenceLevel": "medium",
                  "reasons": [
                    "현재 가격: 137500000 (변화율: 0.44%, 움직임: UP)",
                    "Momentum Strategy: 매수 (신뢰도: 80%)"
                  ],
                  "analyzedAt": "2026-05-17T10:15:03"
                }
              ],
              "totalCount": 1,
              "analyzedAt": "2026-05-17T10:15:03"
            }
            """;

    static final String RECOMMENDATION = """
            {
              "symbol": "BTC",
              "action": "BUY",
              "actionLabel": "매수",
              "confidence": 0.72,
              "confidenceLevel": "medium",
              "reasons": [
                "현재 가격: 137500000 (변화율: 0.44%, 움직임: UP)",
                "Momentum Strategy: 매수 (신뢰도: 80%)"
              ],
              "analyzedAt": "2026-05-17T10:15:03"
            }
            """;

    static final String MOVING_AVERAGE = """
            {
              "symbol": "BTC",
              "source": "UPBIT",
              "quoteCurrency": "KRW",
              "from": "2026-04-17T10:15:00Z",
              "to": "2026-05-17T10:15:00Z",
              "type": "SMA",
              "window": 20,
              "dataPoints": 1,
              "points": [
                {
                  "timestamp": "2026-05-17T10:15:00Z",
                  "price": 137500000,
                  "movingAverage": 136780000
                }
              ]
            }
            """;

    static final String VOLATILITY = """
            {
              "symbol": "BTC",
              "source": "UPBIT",
              "quoteCurrency": "KRW",
              "from": "2026-04-17T10:15:00Z",
              "to": "2026-05-17T10:15:00Z",
              "window": 20,
              "dataPoints": 1,
              "currentVolatility": 0.02134218,
              "annualizedVolatility": 0.40782154,
              "points": [
                {
                  "timestamp": "2026-05-17T10:15:00Z",
                  "volatility": 0.02134218
                }
              ]
            }
            """;

    static final String CORRELATION = """
            {
              "from": "2026-04-17T10:15:00Z",
              "to": "2026-05-17T10:15:00Z",
              "assetCount": 2,
              "pairs": [
                {
                  "symbolA": "BTC",
                  "sourceA": "UPBIT",
                  "symbolB": "ETH",
                  "sourceB": "UPBIT",
                  "correlation": 0.84321540,
                  "overlappingSamples": 720
                }
              ]
            }
            """;

    static final String SUMMARY = """
            {
              "symbol": "BTC",
              "source": "UPBIT",
              "quoteCurrency": "KRW",
              "from": "2026-04-17T10:15:00Z",
              "to": "2026-05-17T10:15:00Z",
              "dataPoints": 720,
              "min": 125000000,
              "max": 139000000,
              "mean": 132480000,
              "stdDev": 2100000,
              "median": 132700000,
              "p5": 126200000,
              "p25": 130100000,
              "p75": 135200000,
              "p95": 138100000,
              "latestPrice": 137500000,
              "returnRate": 0.10000000,
              "maxDrawdown": 0.04500000
            }
            """;

    private OpenApiExamples() {
    }
}
