# Asset Radar 장애 진단 Runbook

기준일: 2026-08-22

수집 → Kafka → 분석/알림 → API → 대시보드로 이어지는 파이프라인에서
"데이터가 안 들어온다", "알림이 안 온다", "API가 느리다"를 1차 진단하기 위한 문서입니다.

관측 스택 구성 배경은 [`OBSERVABILITY_PLAN.md`](../OBSERVABILITY_PLAN.md), 시스템 구조는 [`docs/architecture.md`](architecture.md)를 참고하세요.

## 진입점

| 대상 | URL | 용도 |
|------|-----|------|
| Grafana | `http://localhost:3000` | 대시보드 `Asset Radar` (uid `asset-radar-overview`), Explore |
| Prometheus | `http://localhost:9090` | 타깃 상태, rule 로딩, ad-hoc PromQL |
| Alertmanager | `http://localhost:9093` | 발화 중인 알림, silence |
| Loki | `http://localhost:3100` | Grafana Explore에서 조회 |
| Tempo | `http://localhost:3200` | Grafana Explore에서 조회 |
| API | `http://localhost:8081/actuator/health` | 앱 상태 |
| API 메트릭 | `http://localhost:8081/actuator/prometheus` | 원본 메트릭 |
| 프론트엔드 | `http://localhost:3001` | 사용자 화면 |

Grafana는 익명 Viewer가 열려 있어 로그인 없이 조회할 수 있습니다. 편집은 `admin` / `admin`.

## 사전에 알아둘 것

- `prometheus.yml`에는 앱 스크레이프 타깃이 **두 개** 있습니다.
  - `asset-radar-compose` → `app:8080` (컨테이너 실행 시 UP)
  - `asset-radar-local` → `host.docker.internal:8080` (호스트에서 Spring Boot 실행 시 UP)
  - 한쪽만 실행 중이면 **나머지 하나는 DOWN이 정상**입니다. 이걸 장애로 오인하지 마세요.
- Alertmanager 설정은 `alertmanager/render-config.sh`가 기동 시 생성합니다.
  `ASSET_RADAR_ALERT_SLACK_WEBHOOK_URL` / `ASSET_RADAR_ALERT_WEBHOOK_URL`이 비어 있으면(로컬 기본값)
  no-op receiver로 빠지므로 **알림이 발화해도 외부로 전송되지 않습니다.** 값을 채우면 severity별
  (`warning`/`critical`)로 Slack/Webhook에 전달됩니다.
  발화 여부는 Prometheus `/alerts` 또는 Alertmanager UI에서 직접 확인하세요.
  실제 Slack/Discord/Webhook 전송은 앱 자체의 notifier(`ASSET_RADAR_ALERT_NOTIFIER_*_ENABLED`) 경로이며 Alertmanager와 별개입니다.
- `demo` 프로파일에서도 수집기 **7개가 전부 등록**됩니다(`alphavantage`, `binance`, `demo`,
  `finnhub`, `goldapi`, `kis`, `upbit`). 다만 `demo`를 뺀 나머지는 내부적으로 비활성이라
  `asset_collector_prices_total`이 **0인 채로 시계열만 존재**합니다.
  따라서 demo 실행 중 대시보드 범례에 7개 소스가 보이는데 선이 하나만 움직이는 것은 정상입니다.
  비활성 여부는 기동 로그로 확인합니다.
  ```logql
  {compose_project="asset-radar", compose_service="app"} |= "is disabled"
  ```

## 1차 트리아지 (3분)

어떤 증상이든 아래 순서로 범위를 좁힙니다.

### 1단계 — 컨테이너가 살아 있나

```bash
docker compose -p asset-radar ps
```

`Exit`, `Restarting` 상태 컨테이너가 있으면 그 컨테이너의 로그부터 봅니다.

```bash
docker compose -p asset-radar logs --tail=200 app
```

### 2단계 — Prometheus가 앱을 긁고 있나

`http://localhost:9090/targets` 에서 `asset-radar-compose`(또는 `-local`)가 `UP`인지 확인합니다.

DOWN이면 메트릭 기반 판단이 전부 무의미하므로, 여기서 먼저 앱 기동/포트/프로파일을 확인합니다.

### 3단계 — 파이프라인 어느 구간에서 끊겼나

Grafana `Asset Radar` 대시보드에서 위에서 아래로 훑습니다.

| 패널 | 0이면 의심 구간 |
|------|----------------|
| Collector — 수집 속도 | 외부 API / 수집기 |
| Pipeline — 분석 처리량 | Kafka 또는 `AssetAnalysisConsumer` |
| Alerts — 평가 vs 트리거 | `AssetAlertConsumer` |
| API — HTTP 요청 속도 | 프론트엔드 또는 API 계층 |

또는 Prometheus에서 한 번에:

```promql
sum(rate(asset_collector_prices_total[1m]))
sum(rate(asset_analysis_processed_total[1m]))
sum(rate(asset_alerts_evaluated_total[1m]))
```

앞에서부터 0이 되는 지점이 끊긴 구간입니다.

## 시나리오별 진단

### A. 대시보드에 가격이 안 들어온다

**증상**: 프론트엔드가 비어 있거나 `Connecting` 상태 유지, `Collector — 수집 속도`가 0.

1. 수집 자체가 도는지 확인
   ```promql
   sum by (source) (rate(asset_collector_prices_total[5m]))
   ```
   - 전 소스 0 → 앱 기동 또는 프로파일 문제. 2번으로.
   - 특정 소스만 0 → 그 소스의 외부 API 문제. 3번으로.

2. 수집기가 등록됐는지 로그 확인 (Grafana Explore, Loki)
   ```logql
   {compose_project="asset-radar", compose_service="app"} |= "Starting collector"
   ```
   기대한 수집기가 목록에 없으면 프로파일/`*_ENABLED` 환경변수 문제입니다.
   등록은 됐는데 데이터가 0이면 비활성 상태이므로 아래로 한 번 더 좁힙니다.
   ```logql
   {compose_project="asset-radar", compose_service="app"} |= "is disabled"
   ```
   이 로그에 걸리면 장애가 아니라 설정대로 꺼진 것입니다.

3. 수집기가 죽었는지 확인
   ```logql
   {compose_project="asset-radar", compose_service="app"} |= "stopped with an error"
   ```
   `CollectorPipeline`은 스트림이 에러로 종료되면 재구독하지 않습니다.
   이 로그가 보이면 **앱 재시작이 사실상 유일한 복구 수단**입니다.
   ```bash
   docker compose -p asset-radar restart app
   ```

4. 외부 API 응답 실패인지 확인
   ```logql
   {compose_project="asset-radar", compose_service="app"} |~ "collector request failed|KIS token"
   ```
   API 키 만료/쿼터 초과가 가장 흔합니다. KIS는 토큰 발급 실패가 별도 로그로 남습니다.

5. 외부 API가 정상인데도 안 들어오면 저장 단계 확인
   ```logql
   {compose_project="asset-radar", compose_service="app"} |= "Sink" |= "failed for"
   ```
   Redis/PostgreSQL/Kafka sink 실패는 수집 카운터는 올라가지만 화면에는 반영되지 않는 형태로 나타납니다.

**연관 알림**: `AssetRadarCollectorErrors`

### B. 가격은 들어오는데 분석이 멈췄다

**증상**: `Collector — 수집 속도` > 0 인데 `Pipeline — 분석 처리량` = 0.
알림 `AssetRadarAnalysisPipelineIdle`(critical)이 발화합니다.

1. Kafka consumer lag 확인
   ```promql
   kafka_consumer_fetch_manager_records_lag
   ```
   lag이 계속 증가 → consumer가 붙어는 있으나 처리 지연.
   메트릭 자체가 없음 → consumer가 아예 안 붙음.

2. Kafka 컨테이너 상태
   ```bash
   docker compose -p asset-radar logs --tail=100 kafka
   ```

3. 분석 저장 실패 확인
   ```logql
   {compose_project="asset-radar", compose_service="app"} |= "Analysis sink"
   ```
   `Analysis sink ... failed for` 는 개별 저장 실패(처리량은 유지),
   `threw unexpectedly` 는 예상 못 한 예외입니다.

4. Tempo에서 `수집 → Kafka publish → analysis consume` 트레이스가 어디서 끊기는지 확인합니다.
   Grafana Explore → Tempo → Service `asset-radar`.

**복구**: Kafka 이상이면 `docker compose -p asset-radar restart kafka app` (consumer 재조인 필요).

### C. 알림이 안 온다

먼저 **어느 알림인지** 구분합니다. 두 경로가 완전히 다릅니다.

| 경로 | 대상 | 확인 위치 |
|------|------|-----------|
| 앱 알림 (가격/분석 기반) | Slack / Discord / Webhook notifier | 앱 로그, `asset_alerts_*` 메트릭 |
| 인프라 알림 (Prometheus rule) | Alertmanager | `localhost:9090/alerts`, `localhost:9093` |

**앱 알림이 안 오는 경우**

1. 평가 자체가 도는지
   ```promql
   sum(rate(asset_alerts_evaluated_total[5m]))
   sum(rate(asset_alerts_triggered_total[5m]))
   ```
   - `evaluated` = 0 → 알림 소비자 정지 (`AssetRadarAlertPipelineIdle`). B와 동일하게 Kafka/consumer 확인.
   - `evaluated` > 0, `triggered` = 0 → 임계치 미달. 장애가 아니라 정상일 수 있습니다.
   - `triggered` > 0 인데 안 옴 → 발송 단계 문제. 2번으로.

2. 발송 실패 로그
   ```logql
   {compose_project="asset-radar", compose_service="app"} |= "Alert notifier"
   ```
   `Alert notifier ... failed for` → webhook URL/토큰/네트워크.
   `Failed to schedule alert notifier` → executor 포화.

3. notifier 자체가 꺼져 있는지 확인.
   `demo` 프로파일과 기본 compose는 `ASSET_RADAR_ALERT_NOTIFIER_*_ENABLED=false`입니다.
   **이 상태에서 알림이 안 오는 것은 설정대로 동작한 것**입니다.

**인프라 알림이 안 오는 경우**

1. rule이 로딩됐는지: `http://localhost:9090/rules`
2. 발화 중인지: `http://localhost:9090/alerts`
3. Alertmanager까지 전달됐는지: `http://localhost:9093`
4. 전달됐는데 외부로 안 갔다면 no-op receiver로 빠진 것입니다(로컬 기본값).
   `.env`에 `ASSET_RADAR_ALERT_SLACK_WEBHOOK_URL` 또는 `ASSET_RADAR_ALERT_WEBHOOK_URL`을 채운 뒤
   `docker compose up -d alertmanager`로 재기동하면 실제로 전달됩니다.

### D. API가 느리거나 5xx가 난다

**증상**: `API — 응답시간 P99` 급등 또는 `AssetRadarApi5xxBurst` 발화.

1. 어느 엔드포인트인지 특정
   ```promql
   topk(5, sum by (uri) (rate(http_server_requests_seconds_count{service="asset-radar",status=~"5.."}[5m])))
   ```
   ```promql
   histogram_quantile(0.99, sum by (uri, le) (rate(http_server_requests_seconds_bucket{service="asset-radar"}[5m]))) * 1000
   ```

2. 앱 리소스 확인 — 대시보드 `JVM — Heap 사용량`, `System — CPU 사용률`.
   heap이 max에 붙어 있으면 GC 스톨을 먼저 의심합니다(`AssetRadarJvmHeapUsageHigh`).
   점진적 증가는 누수, 스파이크는 일시적 부하로 봅니다. 누수가 의심되면 collector/consumer의
   버퍼링 로직에 무한정 쌓이는 컬렉션이 있는지 확인합니다.
   ```promql
   jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"}
   ```

3. 해당 URI의 트레이스에서 병목 확인.
   Grafana Explore → Tempo → 느린 trace 선택 → 외부 `WebClient` span과 DB span 중 어느 쪽이 긴지 봅니다.
   외부 호출이 길면 A-4의 외부 API 문제와 같은 원인입니다.

4. 에러 로그를 traceId로 역추적
   ```logql
   {compose_project="asset-radar", compose_service="app"} |= "ERROR"
   ```
   Loki datasource에 `traceID` derived field가 걸려 있어 로그에서 트레이스로 바로 이동할 수 있습니다.

5. DB/Redis 쪽이면
   ```bash
   docker compose -p asset-radar logs --tail=100 postgres redis
   ```

### E. Grafana에 데이터가 아예 안 보인다

**증상**: 모든 패널이 `No data`. 알림 `ObservabilityComponentDown` 발화 가능.

1. 관측 컴포넌트 상태
   ```promql
   up{job=~"prometheus|grafana|loki|tempo|alertmanager"}
   ```
2. `0`인 컴포넌트의 로그를 봅니다.
   ```bash
   docker compose -p asset-radar logs --tail=100 loki
   ```
3. 메트릭은 나오는데 로그만 없으면 Promtail을 의심합니다.
   Promtail은 `com.docker.compose.project=asset-radar` 라벨만 수집하므로,
   **compose 프로젝트 이름이 다르면 로그가 한 줄도 안 들어옵니다.**
   ```bash
   docker compose -p asset-radar logs --tail=100 promtail
   ```
4. Grafana datasource는 프로비저닝 파일로 고정(`editable: false`)되어 있습니다.
   UI에서 고치지 말고 `grafana/provisioning/datasources/prometheus.yml`을 고친 뒤 재기동하세요.

> `up{job=~...}` 정규식에 `asset-radar-compose` / `asset-radar-local`은 **포함되어 있지 않습니다.**
> 즉 앱이 죽어도 `ObservabilityComponentDown`은 발화하지 않습니다. 앱 다운은 A/B 경로로 감지합니다.

## 알림 규칙 요약

정의 위치: [`prometheus/rules/asset-radar-alerts.yml`](../prometheus/rules/asset-radar-alerts.yml)

| 알림 | severity | for | 의미 | 대응 |
|------|----------|-----|------|------|
| `ObservabilityComponentDown` | critical | 2m | 관측 컴포넌트 다운 | E |
| `AssetRadarCollectorErrors` | warning | 2m | 수집기 에러 발생 | A |
| `AssetRadarAnalysisPipelineIdle` | critical | 5m | 수집은 되는데 분석 정지 | B |
| `AssetRadarAlertPipelineIdle` | critical | 5m | 분석은 되는데 알림 평가 정지 | C |
| `AssetRadarApi5xxBurst` | warning | 5m | 5xx > 0.05/s | D |
| `AssetRadarApiLatencyP99High` | warning | 5m | URI별 P99 > 1s | D |
| `AssetRadarKafkaConsumerLagHigh` | warning | 5m | consumer lag > 1000 | B (아래 한계 참고) |
| `AssetRadarAlertTriggerRatioAnomaly` | warning | 5m | 트리거/평가 비율 > 50% | 아래 참고 |
| `AssetRadarJvmHeapUsageHigh` | warning | 5m | heap 사용률 > 85% | D-2 |

`AssetRadarAlertTriggerRatioAnomaly`가 발화하면 `application.yml`의
`asset-radar.alert.*-change-rate-threshold` 값이 최근에 바뀌었는지 먼저 확인하고,
설정 변경이 없으면 특정 자산의 실제 급등락(시장 이벤트)인지 봅니다.

## 자주 쓰는 쿼리

**PromQL**

```promql
sum by (source) (rate(asset_collector_prices_total[5m]))
sum by (source) (increase(asset_collector_errors_total[5m]))
sum(rate(asset_analysis_processed_total[5m]))
sum(rate(asset_alerts_triggered_total[5m])) / sum(rate(asset_alerts_evaluated_total[5m]))
histogram_quantile(0.99, sum by (uri, le) (rate(http_server_requests_seconds_bucket{service="asset-radar"}[5m]))) * 1000
jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"}
```

**LogQL** (Grafana Explore → Loki)

```logql
{compose_project="asset-radar", compose_service="app"} |= "ERROR"
{compose_project="asset-radar", compose_service="app"} |= "stopped with an error"
{compose_project="asset-radar", compose_service="app"} |~ "Sink .* failed for"
{compose_project="asset-radar", compose_service="app"} |= "Alert notifier"
{compose_project="asset-radar"} |= "ERROR"
sum by (compose_service) (rate({compose_project="asset-radar"} |= "ERROR" [5m]))
```

사용 가능한 라벨: `compose_project`, `compose_service`, `service`, `container`, `stream`.

## 복구 명령

```bash
# 전체 상태
docker compose -p asset-radar ps

# 앱만 재기동 (수집기 재구독 포함)
docker compose -p asset-radar restart app

# 관측 스택만 재기동
docker compose -p asset-radar restart prometheus grafana loki promtail tempo alertmanager

# Prometheus 설정/rule 리로드 (재기동 없이)
curl -X POST http://localhost:9090/-/reload

# 데모 스택 전체 재기동
docker compose -f docker-compose.yml -f docker-compose.demo.yml up -d --build
```

`--web.enable-lifecycle`이 켜져 있어 Prometheus는 rule 수정 후 리로드만으로 반영됩니다.

## 알려진 한계

- `CollectorPipeline`은 스트림 종료 시 자동 재구독을 하지 않습니다. 수집기 사망 시 앱 재시작이 필요합니다.
- `ObservabilityComponentDown`이 앱 타깃을 커버하지 않습니다.
- Alertmanager의 Slack/Webhook 라우팅은 설정으로 켤 수 있지만, 실제 전달은 아직 실측으로 검증되지 않았습니다.
- `asset-radar-local` 타깃은 컨테이너 실행 시 항상 DOWN으로 남습니다.
- `Kafka — Consumer Lag` 패널은 `No data`입니다.
  `kafka_consumer_fetch_manager_records_lag`가 노출되지 않아, B 시나리오에서는 lag 대신
  `asset_analysis_processed_total`의 정지 여부로 판단해야 합니다.
  같은 이유로 `AssetRadarKafkaConsumerLagHigh` 규칙은 로딩은 되지만 발화할 수 없습니다.
- Redis/PostgreSQL 연결 장애를 직접 감지하는 alert rule이 없습니다. `redis_exporter`/`postgres_exporter`가
  없고 R2DBC pool 메트릭도 노출되지 않습니다. 두 exporter를 `docker-compose.yml`에 붙이거나
  `r2dbc-pool`을 도입해 커넥션 풀 메트릭을 Micrometer에 등록해야 합니다.

## 변경 이력

- 2026-08-22: 초판 작성. 함께 수정한 것들
  - Grafana 대시보드: 패널 10개가 존재하지 않는 datasource uid를 참조하고 있어 `prometheus`로 교체
  - Tempo: `local-blocks` processor가 요구하는 `metrics_generator.traces_storage.path` 누락을 보완
- 2026-11-11: main의 2026-07-27자 alert별 runbook과 병합.
  alert rule 4종과 Alertmanager 라우팅(`render-config.sh`) 내용을 반영했고,
  초판에 있던 Docker/CI 복합 빌드 수정은 main이 `webhook-notify`를 배포 아티팩트로 참조하도록
  바뀌면서 불필요해져 제외했습니다.
