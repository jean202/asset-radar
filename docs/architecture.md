# Architecture

`asset-radar`는 데이터 소스별 수집 방식과 갱신 주기가 다른 자산 가격 데이터를 하나의 이벤트 파이프라인으로 통합하는 구조를 사용한다.

## 현재 구조

```text
[External APIs]
  Upbit / Gold API / KIS / Alpha Vantage
        |
        v
[Collectors]
  CoinCollector / GoldCollector / StockKrCollector / StockUsCollector
        |
        v
[Kafka]
  asset.price.realtime / asset.price.analysis
        |
        v
[Consumers]
  CollectorPipeline / AnalysisProcessor / AssetAnalysisConsumer / AssetAlertConsumer
        |
        +--> Redis latest cache
        +--> PostgreSQL history tables
        +--> Alert notifier
        |
        v
[API Layer]
  dashboard / latest / history / compare / analysis / alerts / statistics
        |
        +--> SSE stream
        +--> Swagger UI
        |
        v
[Frontend]
  React Dashboard / Analytics
```

## 계층별 진행 상태

### 수집 계층

- [x] `CoinCollector`로 Upbit WebSocket 수집
- [x] `GoldCollector`로 `gold-api.com` 폴링
- [x] `StockKrCollector`로 한국 주식 수집
- [x] `StockUsCollector`로 미국 주식 수집
- [ ] Binance/Finnhub 같은 추가 공급자 연결

### 처리 계층

- [x] 가격 이벤트 Kafka 발행
- [x] 분석 이벤트 생성
- [x] 알림 규칙 평가와 notifier 분기
- [x] Redis 최신 상태 캐시
- [x] PostgreSQL 이력 적재

### 제공 계층

- [x] REST API로 최신 시세, 이력, 비교, 분석, 알림 제공
- [x] `GET /api/dashboard/stream` SSE 제공
- [x] Swagger UI 문서화
- [x] React 기반 Dashboard / Analytics 화면

## 저장 구조

현재 PostgreSQL에는 아래 이력 테이블이 존재한다.

- `asset_price_history`
- `asset_analysis_history`
- `asset_alert_history`

Redis에는 최신 가격/분석/알림을 별도 키 prefix로 유지한다.

## 현재 품질 상태

- [x] 백엔드 테스트 통과
- [x] 프론트엔드 lint 통과
- [x] 프론트엔드 테스트 통과
- [x] 프론트엔드 프로덕션 빌드 통과

## 다음 아키텍처 작업

- [ ] 공급자 추가 시 Collector 추상화 확장
- [ ] 운영 환경용 배포/설정 분리
