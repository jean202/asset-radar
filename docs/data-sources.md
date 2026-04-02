# Data Sources

## 코인

- Upbit WebSocket
- Binance WebSocket

## 한국 주식

- 한국투자증권 Open API

## 미국 주식

- Alpha Vantage 또는 Finnhub

## 금

- 한국은행 API 또는 공개 시세 API
- 현재 기본 구현: `gold-api.com`의 `/price/XAU` 엔드포인트를 5분 주기로 폴링

각 소스는 갱신 주기와 접근 방식이 다르므로, 최종적으로는 `AssetCollector` 인터페이스 뒤에서 흡수한다.
