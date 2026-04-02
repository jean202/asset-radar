# asset-radar

여러 자산의 가격 흐름을 수집하고 비교 분석하기 위한 WebFlux 기반 백엔드 프로젝트입니다.

## 상태

- 현재 상태: 초기 구조 정리 중
- 목표: WebFlux, Kafka, Redis, PostgreSQL을 조합한 이벤트 기반 파이프라인
- 방향: 수집 파이프라인과 문서 구조를 먼저 정리하고 있습니다

## 현재 포함된 내용

- Spring Boot + WebFlux 기본 빌드 설정
- collector / domain / pipeline / api 기본 패키지 구조
- Docker Compose 초안
- 데이터 소스 및 아키텍처 문서
- Upbit WebSocket 기반 코인 수집
- Gold API 기반 금 시세 폴링
- 대시보드 API, 비교 API, 알림/분석 기본 흐름

## 문서

- `docs/architecture.md`
- `docs/data-sources.md`
- `docs/decision-log.md`
- `PROJECT_PLAN.md`

## 실행

```bash
docker compose up -d
./gradlew bootRun
```

## 주요 API

### 대시보드

```http
GET /api/dashboard
GET /api/dashboard/stream
```

예시 응답 요약:

```json
{
  "status": "live",
  "assetCount": 3,
  "assets": [
    { "source": "GOLDAPI", "quoteCurrency": "USD", "symbol": "XAU" },
    { "source": "UPBIT", "quoteCurrency": "KRW", "symbol": "BTC" },
    { "source": "UPBIT", "quoteCurrency": "KRW", "symbol": "ETH" }
  ],
  "sourceGroups": [
    {
      "source": "GOLDAPI",
      "sourceDisplayName": "Gold API",
      "assetType": "GOLD",
      "assetTypeLabel": "금",
      "stale": false,
      "lastUpdatedAgo": "1m ago"
    }
  ]
}
```

### 자산 비교

```http
GET /api/compare?assets=UPBIT:KRW:BTC,GOLDAPI:USD:XAU&period=30d&baseAmount=1000000
```

비교 규칙:

- `assets`는 `SYMBOL` 또는 `SOURCE:QUOTE:SYMBOL` 형식을 받습니다.
- `period`는 `30d`, `12h`, `15m` 같은 형식을 받습니다.
- 서로 다른 `quoteCurrency`가 섞이면 `projectedValueComparable=false`가 되고 `projectedValue`는 `null`로 내려갑니다.

예시 응답 요약:

```json
{
  "requestedCount": 2,
  "count": 2,
  "projectedValueComparable": false,
  "quoteCurrencies": ["KRW", "USD"],
  "projectedValueWarning": "projectedValue is only comparable when all assets share the same quoteCurrency"
}
```

### 에러 응답

잘못된 요청은 공통 `400` JSON으로 응답합니다.

```http
GET /api/compare?assets=UPBIT:BTC&period=bad
```

```json
{
  "status": 400,
  "error": "Bad Request",
  "code": "INVALID_REQUEST",
  "message": "period must be one of Ns, Nm, Nh, Nd, Nw",
  "path": "/api/compare"
}
```

## 메모

이 저장소는 공개용 초기 골격입니다. 실제 수집 로직과 Kafka 연동, 시세 분석은 다음 단계에서 계속 확장할 예정입니다.
