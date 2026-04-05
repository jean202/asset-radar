# asset-radar

`asset-radar`는 금, 코인, 한국 주식, 미국 주식 시세를 수집하고 Kafka 기반 파이프라인으로 분석/알림/조회 API까지 제공하는 실시간 자산 모니터링 프로젝트입니다.

## 상태 스냅샷

기준일: 2026-04-04

- 현재 단계: 백엔드 MVP는 동작 가능한 수준까지 올라왔고, 프론트엔드 분석 화면과 문서 정리를 마무리하는 단계
- 현재 워크트리 기준 포함 기능: 실시간 대시보드, 비교 API, 분석/알림 API, 통계 API, React 대시보드/애널리틱스 화면
- 검증 결과: `./gradlew test` 통과, `cd frontend && npm run lint` 통과, `cd frontend && npm run test` 통과, `cd frontend && npm run build` 통과

## 기능별 체크리스트

### 데이터 수집

- [x] Upbit WebSocket 기반 코인 수집
- [x] Gold API 기반 금 시세 폴링
- [x] 한국투자증권(KIS) 기반 한국 주식 수집기
- [x] Alpha Vantage 기반 미국 주식 수집기
- [ ] Binance WebSocket 연동
- [ ] Finnhub 연동

### 파이프라인과 저장소

- [x] Spring Boot 3 + WebFlux 애플리케이션 골격
- [x] Kafka 기반 가격 이벤트 발행/소비
- [x] Redis 최신 시세/분석/알림 캐시
- [x] PostgreSQL 가격/분석/알림 이력 저장
- [x] R2DBC 기반 조회 리더 구현
- [x] Docker Compose 로컬 인프라 구성

### 분석과 알림

- [x] 자산 비교 수익률 계산
- [x] 최신 분석 결과 생성 및 조회
- [x] 알림 규칙 엔진과 severity 분류
- [x] Slack / Webhook 알림 확장 포인트
- [x] 통계 API: 이동평균, 변동성, 상관관계, 요약 통계
- [ ] 포트폴리오 추천/전략화 로직

### API 제공 계층

- [x] `GET /api/dashboard`
- [x] `GET /api/dashboard/stream` SSE
- [x] `GET /api/latest`
- [x] `GET /api/history`
- [x] `GET /api/compare`
- [x] `GET /api/analysis`
- [x] `GET /api/analysis/history`
- [x] `GET /api/alerts`
- [x] `GET /api/alerts/history`
- [x] `GET /api/statistics/moving-average`
- [x] `GET /api/statistics/volatility`
- [x] `GET /api/statistics/correlation`
- [x] `GET /api/statistics/summary`
- [x] 공통 에러 응답 및 Swagger UI

### 프론트엔드

- [x] React + Vite 기반 SPA 분리
- [x] 실시간 Dashboard 화면
- [x] Analytics 화면과 차트 컴포넌트
- [x] Vite dev proxy와 Nginx 배포 이미지
- [x] ESLint 오류 정리
- [x] 프론트엔드 테스트 추가
- [x] 페이지/차트 번들 코드 스플리팅

### 운영과 품질

- [x] Micrometer + Prometheus + Grafana 모니터링 스택
- [x] GitHub Actions CI
- [x] 백엔드 단위/웹/통합 테스트
- [x] 프론트엔드 lint/test/build를 포함한 CI 품질 게이트
- [ ] 운영 배포 스크립트와 환경 분리

## 실행

### 로컬 개발

인프라만 Docker로 띄우고 애플리케이션은 로컬에서 실행하는 방식입니다.

```bash
docker compose up -d kafka redis postgres prometheus grafana
./gradlew bootRun
cd frontend
npm install
npm run dev
```

- 백엔드: `http://localhost:8080`
- 프론트엔드 개발 서버: `http://localhost:5173`
- Grafana: `http://localhost:3000`
- Prometheus: `http://localhost:9090`

### 전체 컨테이너 실행

```bash
docker compose up --build
```

- 백엔드: `http://localhost:8081`
- 프론트엔드: `http://localhost:3001`

## 주요 API

### 실시간 대시보드

```http
GET /api/dashboard
GET /api/dashboard/stream
```

### 시세/이력 조회

```http
GET /api/latest?source=UPBIT&quoteCurrency=KRW
GET /api/history?symbol=BTC&source=UPBIT&quoteCurrency=KRW&limit=100
```

### 자산 비교

```http
GET /api/compare?assets=UPBIT:KRW:BTC,GOLDAPI:USD:XAU&period=30d&baseAmount=1000000
```

비교 규칙:

- `assets`는 `SYMBOL` 또는 `SOURCE:QUOTE:SYMBOL` 형식을 받습니다.
- `period`는 `30d`, `12h`, `15m` 같은 형식을 받습니다.
- 서로 다른 `quoteCurrency`가 섞이면 `projectedValueComparable=false`가 되고 `projectedValue`는 `null`로 내려갑니다.

### 분석/알림

```http
GET /api/analysis?source=UPBIT&quoteCurrency=KRW
GET /api/analysis/history?symbol=BTC&source=UPBIT&quoteCurrency=KRW&limit=30
GET /api/alerts?limit=10
GET /api/alerts/history?symbol=BTC&source=UPBIT&quoteCurrency=KRW&limit=30
```

### 통계

```http
GET /api/statistics/moving-average?symbol=BTC&source=UPBIT&period=30d&type=SMA&window=20
GET /api/statistics/volatility?symbol=BTC&source=UPBIT&period=30d&window=20
GET /api/statistics/correlation?assets=UPBIT:KRW:BTC&assets=UPBIT:KRW:ETH&period=30d
GET /api/statistics/summary?symbol=BTC&source=UPBIT&period=30d
```

## 문서

- `docs/architecture.md`
- `docs/data-sources.md`
- `docs/decision-log.md`
- `PROJECT_PLAN.md`
- `frontend/README.md`

## 현재 남은 작업

- 프론트엔드 lint 오류 정리
- Binance/Finnhub 등 추가 소스 연결
- 추천/전략 계층 고도화
- README와 Swagger 예시 응답 추가 정리
