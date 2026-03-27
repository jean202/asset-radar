# asset-radar — 실시간 자산 비교 파이프라인

## 프로젝트 개요

금, 코인, 한국 주식, 미국 주식 4종 자산의 시세를 실시간/준실시간으로 수집하고, 비교 분석하여 "지금 뭘 사야 하는가"에 대한 판단을 돕는 데이터 파이프라인 시스템.

### 목적
- **포트폴리오 프로젝트**: WebFlux + Kafka 기반 리액티브/고성능 처리 역량 증명
- **실사용 목적**: 본인의 자산 투자 의사결정 보조

---

## 기술 스택

| 항목 | 기술 |
|------|------|
| Language | Java 17+ |
| Framework | Spring Boot 3, Spring WebFlux |
| Messaging | Apache Kafka |
| Cache/집계 | Redis |
| DB | PostgreSQL (이력 저장) |
| Monitoring | Micrometer + Grafana |
| Container | Docker Compose |
| Build | Gradle |
| CI/CD | GitHub Actions |

---

## 아키텍처

### 전체 흐름

```
[데이터 소스]                    [수집 계층]              [처리 계층]           [제공 계층]

Upbit WebSocket ──────→ CoinCollector ──→ ┐
Binance WebSocket ────→ CoinCollector ──→ │
한국투자증권 API ──────→ StockKrCollector → ├─→ [Kafka] ──→ [Consumer 집계/분석] ──→ Redis Cache
Alpha Vantage API ────→ StockUsCollector → │                      │                    │
한국은행 API ──────────→ GoldCollector ──→ ┘                      ↓                    ↓
                                                          PostgreSQL          REST API
                                                          (이력 저장)         SSE (실시간 푸시)
```

### Kafka 토픽 설계

| 토픽 | 데이터 | 갱신 주기 |
|------|--------|----------|
| `asset.coin.realtime` | BTC, ETH 등 코인 시세 | 실시간 (WebSocket) |
| `asset.stock.kr` | 한국 주식 시세 | 실시간 (한국투자증권 API) |
| `asset.stock.us` | 미국 주식 시세 | 15분 지연 (Alpha Vantage 무료) |
| `asset.gold` | 금 시세 | 시간별 폴링 (한국은행 API) |
| `asset.analysis` | 비교 분석 결과 | Consumer 처리 후 발행 |

---

## 데이터 소스별 수집 전략

이 프로젝트의 핵심 어필 포인트: **데이터 소스마다 갱신 주기와 수집 방식이 다른 환경에서 통일된 파이프라인으로 처리**

| 자산 | API | 수집 방식 | 갱신 주기 | 비고 |
|------|-----|----------|----------|------|
| 코인 | Upbit WebSocket, Binance WebSocket | WebSocket 스트림 → WebFlux | 실시간 | 가장 접근성 좋음 |
| 한국 주식 | 한국투자증권 Open API | REST 폴링 / WebSocket | 실시간 | 개인 계좌 필요, 초당 호출 제한 |
| 미국 주식 | Alpha Vantage 또는 Finnhub | REST 폴링 | 15분 지연 (무료) | 실시간은 유료 |
| 금 | 한국은행 Open API, Gold API | REST 폴링 | 일별/시간별 | 실시간은 어려움 |

### Collector 공통 인터페이스

```java
public interface AssetCollector {
    Flux<AssetPrice> collect();          // 리액티브 스트림으로 통일
    AssetType getAssetType();
    Duration getRefreshInterval();       // 소스별 갱신 주기
}
```

WebSocket 기반(코인)이든 폴링 기반(금)이든, Flux<AssetPrice>로 통일 → Kafka Producer에서 동일하게 처리

---

## 핵심 기능

### 1. 실시간 자산 대시보드

```
[코인]     BTC  ▲ 137,500,000원 (+2.3%)    ETH  ▼ 5,120,000원 (-0.8%)
[한국주식]  삼성전자  ▲ 78,500원 (+1.2%)      SK하이닉스  ▲ 215,000원 (+3.1%)
[미국주식]  AAPL  ▼ $198.50 (-0.5%)         NVDA  ▲ $890.20 (+4.2%)   ※15분 지연
[금]       1g ▲ 135,200원 (+0.3%)                                     ※시간별 갱신

마지막 갱신: 코인 0.3초 전 | 한국주식 1초 전 | 미국주식 12분 전 | 금 45분 전
```

### 2. 자산 간 상대 비교

```
GET /api/compare?assets=BTC,GOLD,SAMSUNG&period=30d

→ 최근 30일 수익률 비교:
   BTC        +12.5%  ████████████▌
   금          +3.2%  ███▏
   삼성전자     +5.8%  █████▊

   → 동일 금액(100만원) 투자 시: BTC 1,125,000 / 금 1,032,000 / 삼성 1,058,000
```

### 3. 알림 — "지금 움직여야 할 때"

```
- BTC가 24시간 내 5% 이상 하락 시 알림
- 금 대비 코인 비율이 특정 임계치 돌파 시 알림
- 한국/미국 주식 시장 동시 급등/급락 감지
```

### 4. 이력 조회 — "과거에는 어땠나"

```
GET /api/history?asset=BTC&from=2026-01-01&to=2026-03-27&interval=1d

→ 일별 종가 + 거래량 + 다른 자산 대비 상대 수익률
```

---

## WebFlux 활용 포인트 (파라메타 NFT 마켓플레이스 경험 연장)

| 포인트 | 설명 |
|--------|------|
| WebSocket 수신 | Upbit/Binance WebSocket → WebFlux WebSocketClient |
| 리액티브 Kafka | reactor-kafka로 Flux 기반 Produce/Consume |
| SSE 푸시 | 클라이언트에 실시간 시세 변동 Server-Sent Events로 전달 |
| 백프레셔 처리 | 코인 WebSocket은 초당 수백 건 → buffer/sample로 제어 |
| Non-blocking DB | R2DBC 또는 이력 저장 비동기 처리 |

---

## 프로젝트 구조

```
asset-radar/
│
├── src/main/java/
│   ├── collector/           # 데이터 수집 (자산별 Collector)
│   │   ├── AssetCollector.java        (공통 인터페이스)
│   │   ├── CoinCollector.java         (Upbit/Binance WebSocket)
│   │   ├── StockKrCollector.java      (한국투자증권 API)
│   │   ├── StockUsCollector.java      (Alpha Vantage)
│   │   └── GoldCollector.java         (한국은행 API)
│   │
│   ├── pipeline/            # Kafka Producer/Consumer
│   │   ├── AssetProducer.java
│   │   ├── AssetConsumer.java
│   │   └── AnalysisProcessor.java     (비교 분석 로직)
│   │
│   ├── analysis/            # 분석/비교 로직
│   │   ├── AssetComparator.java       (자산 간 수익률 비교)
│   │   ├── AlertEvaluator.java        (알림 조건 평가)
│   │   └── TrendAnalyzer.java         (추세 분석)
│   │
│   ├── api/                 # REST API + SSE
│   │   ├── DashboardController.java
│   │   ├── CompareController.java
│   │   └── HistoryController.java
│   │
│   ├── domain/              # 도메인 모델
│   │   ├── AssetPrice.java
│   │   ├── AssetType.java
│   │   └── ComparisonResult.java
│   │
│   └── infra/               # Redis, PostgreSQL, 알림
│       ├── RedisAssetCache.java
│       ├── AssetHistoryRepository.java
│       └── NotificationSender.java
│
├── docker-compose.yml       # Kafka + Redis + PostgreSQL + Grafana
├── docs/
│   ├── architecture.md
│   ├── data-sources.md      (API별 특성/제약 정리)
│   └── decision-log.md
└── grafana/
    └── dashboards/          (Micrometer 메트릭 대시보드 JSON)
```

---

## 모니터링 (Grafana 대시보드)

| 패널 | 메트릭 |
|------|--------|
| Collector 상태 | 소스별 수집 성공/실패율, 지연시간 |
| Kafka 처리량 | 토픽별 초당 메시지 수, Consumer lag |
| API 응답 | 엔드포인트별 응답시간, 에러율 |
| 비즈니스 | 자산별 최신 시세, 알림 발생 횟수 |

→ Grafana 대시보드 스크린샷을 포트폴리오에 넣으면 시각적 임팩트 큼

---

## 다른 포트폴리오 프로젝트와의 연계

| 프로젝트 | 증명하는 역량 |
|----------|-------------|
| **asset-radar (이것)** | 리액티브(WebFlux), 메시징(Kafka), 고성능 처리, 모니터링 |
| card-mizer (카드 실적) | 아키텍처 설계(헥사고날), 도메인 모델링, 테스트 전략 |
| (오픈소스 SDK — 미정) | API/인터페이스 설계, 라이브러리 배포 |

**통합 스토리**: "개인 금융 의사결정을 자동화하는 개발자" — card-mizer로 지출 최적화, asset-radar로 투자 판단

---

## 새 세션에서 시작할 때 프롬프트

```
/Users/admin/IdeaProjects/asset-radar/PROJECT_PLAN.md 파일을 읽고,
이 계획에 따라 프로젝트 초기 세팅을 해줘.

작업 범위:
1. Spring Boot 3 + WebFlux 프로젝트 생성
2. Docker Compose 구성 (Kafka + Redis + PostgreSQL + Grafana)
3. AssetCollector 공통 인터페이스 및 CoinCollector (Upbit WebSocket) 우선 구현
4. Kafka Producer/Consumer 기본 설정
5. docs/ 디렉토리 생성 (architecture.md, data-sources.md, decision-log.md)
6. GitHub 레포 생성 및 초기 커밋

참고:
- 목업/더미 데이터 금지, 실제 동작하는 코드로 작성
- 요청한 작업만 수행, 주변 코드 건드리지 않기
```
