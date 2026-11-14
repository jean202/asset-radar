<!-- AGENTS.md 와 CLAUDE.md 는 동일하게 유지됩니다. 한쪽을 수정하면 다른 쪽도 같이 수정하세요. -->
# asset-radar — 작업 메모

## 다음 작업 시작 시 가장 먼저 제안할 것

### 관측 스택의 남은 구멍 메우기

2026-08-22 운영 증거 작업 중 실측으로 확인된 미완성 항목이다. 1~3은 runbook의 "알려진 한계"에 적혀 있다.

1. **Kafka consumer lag 메트릭 미노출**
   - `kafka_consumer_fetch_manager_records_lag`가 `/actuator/prometheus`에 없어 Grafana `Kafka — Consumer Lag` 패널이 `No data`다.
   - Spring Kafka 클라이언트 메트릭을 Micrometer에 바인딩해야 한다.
   - 이게 없으면 runbook 시나리오 B(분석 파이프라인 정지)에서 lag 축 진단이 불가능하고,
     `AssetRadarKafkaConsumerLagHigh` 규칙도 발화할 수 없다.
2. **`ObservabilityComponentDown` 규칙이 앱 타깃을 커버하지 않음**
   - `up{job=~"prometheus|grafana|loki|tempo|alertmanager"}`라 `asset-radar-compose`가 빠져 있다.
   - 앱이 죽어도 이 알림은 발화하지 않는다.
   - 단, `asset-radar-local`은 컨테이너 실행 시 항상 DOWN이므로 정규식에 그냥 추가하면 오탐이 된다. 조건 분리가 필요하다.
3. **Alertmanager 라우팅이 미검증**
   - `alertmanager/render-config.sh`로 Slack/Webhook 라우팅을 켤 수 있게 됐지만, webhook URL을 채워
     알림이 실제로 나가는 것을 실측으로 확인한 적은 없다.
4. **운영 스크린샷 재캡처**
   - `docs/screenshots/observability_*.png`는 alert rule 4종과 Loki 로그 패널이 추가되기 전 베이스에서
     찍어 rule 5개 / 패널 10개로 보인다. 현재 정의는 rule 9개 / 패널 11개다.

## 완료된 TODO

### Grafana 운영 증거 보강과 장애 진단 runbook

- **완료일**: 2026-08-22
- **산출물**:
  - `docs/runbook.md` — 1차 트리아지 3단계, 시나리오 A~E, PromQL/LogQL 모음, 복구 명령, 알려진 한계
  - `docs/screenshots/observability_grafana_dashboard.png`
  - `docs/screenshots/observability_prometheus_rules.png`
  - `docs/screenshots/observability_loki_logs.png`
  - README `## Screenshots` 아래 `### 운영 관측` 절
- **캡처 방법**: demo 프로파일 전체 컨테이너 실행 + headless Chrome CDP 스크린샷. Grafana는 익명 Viewer로 Explore가 막히므로 캡처 시에만 `GF_AUTH_ANONYMOUS_ORG_ROLE=Editor` override가 필요하다.
- **작업 중 발견해 함께 고친 결함 2건**:
  - Grafana 대시보드 패널 10개가 존재하지 않는 datasource uid `PBFA97CFB590B2093`를 참조해 전부 `No data`였다. 프로비저닝 uid인 `prometheus`로 교체.
  - Tempo `local-blocks` processor가 `metrics_generator.traces_storage.path` 누락으로 기동 실패 중이었다. span metrics가 remote write되지 않고 있었다.
- **참고**: 당시 함께 넣었던 Docker/CI 복합 빌드 수정(additional build context)은 main이 `webhook-notify`를
  배포 아티팩트로 참조하도록 바뀌어(11c2390) 병합 시 제외했다.

### 관측 스택 alert 보강과 운영 runbook

- **완료일**: 2026-07-27
- **내용**:
  - Alertmanager가 항상 `dev-null`로만 라우팅되던 문제를 고쳐, `ASSET_RADAR_ALERT_SLACK_WEBHOOK_URL` /
    `ASSET_RADAR_ALERT_WEBHOOK_URL`이 설정되면 severity별(`warning`/`critical`)로 Slack/Webhook에
    실제로 전달되게 함. 값이 없으면 기존처럼 no-op으로 동작(로컬 기본값 유지).
  - `OBSERVABILITY_PLAN.md`에 명시되어 있었지만 빠져 있던 alert rule 4종 추가: API P99 지연,
    Kafka consumer lag, alert 트리거 비율 이상, JVM heap 사용률.
  - Grafana 대시보드에 Loki 로그 패널 추가 (`{compose_service="app"} |= "ERROR"`).
  - Alert별 진단 절차를 담은 운영 runbook 신설.
  - Redis/PostgreSQL 연결 장애 감지는 exporter 부재로 보류 — `docs/runbook.md`의 "알려진 한계"에 기록.
- **관련 파일**:
  - `alertmanager/render-config.sh` (신규, `alertmanager/alertmanager.yml` 대체)
  - `prometheus/rules/asset-radar-alerts.yml`
  - `grafana/dashboards/asset-radar.json`
  - `docker-compose.yml`
  - `docs/runbook.md` (신규)
  - `.env.example`

### README에 동작 스크린샷 추가

- **완료일**: 2026-05-17 기준 README에 반영됨
- **파일**:
  - `docs/screenshots/dashboard_1.png`
  - `docs/screenshots/dashboard_2.png`
  - `docs/screenshots/analytics.png`
- **삽입 위치**: `## Architecture` 아래 `## Screenshots`

### 실행 환경 분리와 데모 모드

- **완료일**: 2026-05-17
- **프로파일**:
  - `local`: Docker 인프라 + 호스트 Spring Boot
  - `docker`: 전체 Compose 실행 기본값
  - `demo`: 외부 API 수집기를 끄고 synthetic data 생성
  - `prod`: 운영형 환경변수 기반 설정
- **관련 파일**:
  - `src/main/resources/application-local.yml`
  - `src/main/resources/application-docker.yml`
  - `src/main/resources/application-demo.yml`
  - `src/main/resources/application-prod.yml`
  - `docker-compose.demo.yml`
  - `DemoAssetCollector`

### Swagger 예시 응답 추가 정리

- **완료일**: 2026-06-24
- **내용**:
  - 주요 REST API의 성공/에러 Swagger response example 정리
  - 응답 DTO와 도메인 record의 `@Schema` 설명/예시 보강
- **관련 파일**:
  - `src/main/java/com/jean202/assetradar/api/OpenApiExamples.java`
  - `src/main/java/com/jean202/assetradar/api/*Response.java`
  - `src/main/java/com/jean202/assetradar/domain/AssetPrice.java`
  - `src/main/java/com/jean202/assetradar/domain/AssetAnalysis.java`
  - `src/main/java/com/jean202/assetradar/domain/AssetAlert.java`

### 운영 배포 자동화와 secret 주입 방식 확정

- **완료일**: 2026-06-24
- **결정**:
  - GitHub Actions가 API/프론트엔드 이미지를 GHCR에 빌드/푸시
  - 운영 서버는 SSH로 `deploy/docker-compose.prod.yml`을 갱신하고 Docker Compose로 재기동
  - KIS, Alpha Vantage, Finnhub, 알림 webhook, DB 비밀번호는 운영 서버의 `.env.prod`에서 런타임 주입
- **관련 파일**:
  - `.github/workflows/deploy.yml`
  - `deploy/docker-compose.prod.yml`
  - `deploy/.env.prod.example`
  - `deploy/README.md`
