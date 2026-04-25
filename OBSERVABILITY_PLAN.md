# Asset Radar Observability Plan

기준일: 2026-04-18

## 배경

`asset-radar`는 외부 시세 수집, Kafka 기반 파이프라인, 분석/알림 소비자, Redis/PostgreSQL 저장, WebFlux API, SSE 대시보드까지 이어지는 실시간 시스템이다. 현재는 `Prometheus + Grafana`와 Micrometer 커스텀 메트릭이 이미 들어가 있으므로, 다음 단계는 로그, 트레이스, 알림을 연결해 운영 가시성을 완성하는 것이다.

이 프로젝트는 세 도구를 한 번에 붙일 가치가 있다.

- `Loki`: collector, sink, Kafka consumer, 외부 API 연동 실패를 로그 축에서 빠르게 좁히기 위해 필요
- `Tempo`: `수집 -> Kafka -> 분석 -> 알림 -> API` 흐름을 한 트레이스로 추적하기 위해 필요
- `Alertmanager`: 장애 감지를 Slack/Webhook으로 자동 승격하기 위해 필요

## 목표

- 메트릭, 로그, 트레이스를 Grafana 한 화면에서 교차 탐색 가능하게 만든다.
- 외부 시세원 장애, Kafka 지연, 분석/알림 정지, API 성능 저하를 자동 감지한다.
- 운영자가 "데이터가 안 들어오는 이유"를 대시보드와 트레이스만으로 1차 진단할 수 있게 만든다.

## 범위

- 애플리케이션 로그
- Kafka/Redis/PostgreSQL/Prometheus/Grafana 컨테이너 로그
- HTTP 요청/응답, WebClient 외부 호출, Kafka producer/consumer, 내부 분석/알림 파이프라인
- Slack/Webhook 알림 라우팅

## 1단계: Loki 도입

### 작업

- `docker-compose.yml`에 `Loki`와 로그 수집기(`Promtail` 또는 `Grafana Alloy`)를 추가한다.
- 애플리케이션 로그를 JSON 또는 key-value 구조로 정리하고 최소한 아래 필드를 남긴다.
  - `service`
  - `source`
  - `symbol`
  - `quoteCurrency`
  - `topic`
  - `consumerGroup`
  - `traceId`
  - `spanId`
- collector, analysis sink, alert sink 실패 로그를 검색 가능한 패턴으로 통일한다.
- Grafana에서 아래 로그 뷰를 만든다.
  - source별 collector 에러
  - Kafka consumer 예외
  - sink persist 실패
  - Slack/Webhook 알림 실패

### 우선 수집 대상

- `app`
- `kafka`
- `redis`
- `postgres`
- `prometheus`
- `grafana`

## 2단계: Tempo 도입

### 작업

- OpenTelemetry SDK 또는 Spring Boot 3 계열 tracing 연동을 추가한다.
- HTTP inbound 요청에 trace를 심고, 로그와 `traceId`를 연계한다.
- 외부 시세 수집 `WebClient` 호출에 client span을 추가한다.
- Kafka producer/consumer 헤더로 trace context를 전파한다.
- 아래 구간을 하나의 trace로 연결한다.
  - 외부 시세 수집
  - Kafka publish
  - analysis consume
  - analysis publish
  - alert consume
  - notification dispatch
- Grafana에서 log to trace, trace to log 이동이 가능하게 설정한다.

### 추적 우선순위

1. Upbit/Binance/KIS/Alpha Vantage 수집 호출
2. `CollectorPipeline`
3. `AssetAnalysisConsumer`
4. `AssetAlertConsumer`
5. `/api/dashboard`, `/api/dashboard/stream`, `/api/history`, `/api/analysis`, `/api/alerts`

## 3단계: Alertmanager 도입

### 경보 규칙

- source별 수집 건수가 일정 시간 동안 `0`
- source별 collector error 급증
- Kafka consumer lag 증가
- `asset.analysis.processed` 급감 또는 정지
- `asset.alerts.evaluated` 대비 `asset.alerts.triggered` 이상 패턴
- API `5xx` 증가
- API P99 응답시간 임계치 초과
- JVM heap 사용률 과다
- Redis/PostgreSQL 연결 실패

### 알림 라우팅

- `warning`: Slack 운영 채널
- `critical`: Slack 운영 채널 + Webhook
- 동일 원인의 반복 알림은 그룹화와 silence 정책으로 억제

## 구현 순서

1. Loki와 로그 수집기 추가
2. 애플리케이션 로그 구조화
3. Tempo와 trace propagation 추가
4. Grafana에서 logs/traces/metrics 연결
5. Alertmanager와 Prometheus rule 추가
6. Slack/Webhook 운영 채널 연동

## 완료 기준

- Grafana에서 메트릭, 로그, 트레이스를 같은 서비스 기준으로 넘나들 수 있다.
- 특정 자산 source 장애 시 경보가 자동 발송된다.
- Kafka lag 증가 시 원인 consumer와 관련 로그를 바로 찾을 수 있다.
- API 지연 시 관련 trace로 외부 호출 병목 여부를 확인할 수 있다.
- 장애 재현 없이도 최근 운영 이슈를 대시보드에서 1차 분석할 수 있다.

## 보류 항목

- 분산 샘플링 정책 세분화
- 장기 보존 스토리지 분리
- 멀티 환경(dev/staging/prod) 분리 운영
