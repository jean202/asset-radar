# Observability Runbook

기준일: 2026-07-27

`Prometheus + Grafana + Loki + Tempo + Alertmanager` 스택으로 들어오는 alert별로,
어떤 화면에서 원인을 좁힐 수 있는지 정리한다. 대상은 `docker-compose.yml` 기준
로컬/docker 프로파일 스택이며, alert 정의는
[`prometheus/rules/asset-radar-alerts.yml`](../prometheus/rules/asset-radar-alerts.yml)에 있다.

## 공통 진단 절차

1. Grafana `Asset Radar` 대시보드(`http://localhost:3000`, uid `asset-radar-overview`)에서
   문제 시각대의 관련 패널을 먼저 확인한다.
2. 로그가 필요하면 Grafana Explore에서 Loki 데이터소스로 전환해
   `{compose_service="app"}` 또는 `{compose_service="app"} |= "ERROR"`로 좁힌다.
3. 특정 요청/트레이스를 추적해야 하면 로그 라인의 `traceId`를 Tempo에서 조회한다.
   Loki 데이터소스는 로그 라인의 `traceID`/`traceId`/`trace_id` 필드를 자동으로
   Tempo trace 링크로 변환한다(derived field, `grafana/provisioning/datasources/prometheus.yml` 참고).
4. Alertmanager UI(`http://localhost:9093`)에서 현재 발화 중인 alert와 silence 상태를 확인한다.

## Alert별 대응

### ObservabilityComponentDown

- 의미: Prometheus/Grafana/Loki/Tempo/Alertmanager 중 하나가 스크레이프 대상에서 사라짐.
- 확인: `docker compose ps`로 해당 컨테이너 상태 확인, `docker compose logs <service>`.
- 잦은 원인: 컨테이너 재시작 루프, 설정 파일 문법 오류(특히 `alertmanager/render-config.sh` 수정 후).

### AssetRadarCollectorErrors

- 의미: 특정 source의 collector가 에러를 내고 있음(`asset_collector_errors_total`).
- 확인: Grafana 대시보드의 "Collector — 에러" 패널에서 어느 source인지 확인 후,
  Loki에서 `{compose_service="app"} |= "<source>"` 로 에러 로그 확인.
- 잦은 원인: 외부 API 키 만료/rate limit, 외부 API 응답 스키마 변경, 네트워크 장애.

### AssetRadarAnalysisPipelineIdle / AssetRadarAlertPipelineIdle

- 의미: 앞 단계는 정상인데 다음 단계가 멈춤 (수집은 되는데 분석이 안 되거나,
  분석은 되는데 알림 평가가 안 됨).
- 확인: Kafka consumer 상태를 Grafana "Kafka — Consumer Lag" 패널에서 먼저 본다.
  lag가 계속 쌓이면 consumer가 멈춘 것이고, lag는 안 쌓이는데 처리량이 0이면
  consumer 자체는 폴링하지만 처리 로직에서 예외가 나고 있을 가능성이 높다.
- 확인: Loki에서 `AssetAnalysisConsumer` / `AssetAlertConsumer` 관련 예외 스택 검색.

### AssetRadarApi5xxBurst / AssetRadarApiLatencyP99High

- 의미: API가 에러를 내거나(5xx) 느려짐(P99 > 1s).
- 확인: Grafana "API — HTTP 요청 속도"와 "API — 응답시간 P99" 패널에서 어떤 URI인지 특정.
- 확인: 해당 URI가 외부 API를 호출하는 경로(`/api/compare`, `/api/recommendations` 등)라면
  Tempo에서 해당 시간대 trace를 열어 외부 WebClient 호출 구간이 병목인지 확인.
- 확인: 외부 호출이 아니라면 JVM heap/CPU 패널과 PostgreSQL/Redis 응답 지연 가능성을 함께 본다.

### AssetRadarKafkaConsumerLagHigh

- 의미: 특정 consumer group/topic의 처리 속도가 발행 속도를 못 따라감.
- 확인: Grafana "Kafka — Consumer Lag" 패널에서 어떤 `client_id`/`topic`인지 확인.
- 잦은 원인: 분석/알림 consumer 쪽 처리 로직이 느려짐, 외부 알림 전송(Slack/Discord/Webhook)
  타임아웃으로 인한 처리 지연.

### AssetRadarAlertTriggerRatioAnomaly

- 의미: 평가된 알림 대비 트리거된 알림 비율이 비정상적으로 높음(10분 기준 50% 초과).
- 확인: `asset.alert.*-change-rate-threshold` 설정값이 최근에 바뀌었는지 확인
  (`src/main/resources/application.yml`의 `asset-radar.alert.*-change-rate-threshold`).
- 확인: 설정 변경이 아니라면 특정 자산의 실제 급등락(시장 이벤트) 가능성도 함께 본다.

### AssetRadarJvmHeapUsageHigh

- 의미: JVM heap 사용률이 85%를 넘어 5분 이상 지속됨.
- 확인: Grafana "JVM — Heap 사용량" 패널에서 추세(점진적 증가 = 누수 의심,
  스파이크성 = 일시적 부하) 판단.
- 확인: 누수 의심이면 collector/consumer 쪽 버퍼링 로직에서 무한정 쌓이는 컬렉션이
  있는지 코드 리뷰가 필요.

## 알려진 갭

- Redis/PostgreSQL 연결 장애를 직접 감지하는 alert rule은 아직 없다. 현재
  스택에는 `redis_exporter`/`postgres_exporter`가 없고, R2DBC pool 메트릭도
  노출되지 않기 때문이다. 추가하려면 두 exporter를 `docker-compose.yml`에
  붙이거나, `r2dbc-pool`을 도입해 커넥션 풀 메트릭을 Micrometer에 등록해야 한다.
- Alertmanager 알림 라우팅(`alertmanager/render-config.sh`)은 `ASSET_RADAR_ALERT_SLACK_WEBHOOK_URL`,
  `ASSET_RADAR_ALERT_WEBHOOK_URL`이 비어 있으면 no-op 리시버로 빠진다. 로컬에서
  실제 알림 전달을 확인하려면 `.env`에 값을 채운 뒤 `docker compose up -d alertmanager`로
  재기동한다.
