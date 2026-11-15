# 야간 작업 발견 사항 (2026-08-19)

`NIGHT_RUN_SPEC.md`대로 infra 계층 Testcontainers 통합 테스트를 작성하면서 남긴 기록이다.
프로덕션 코드는 한 줄도 고치지 않았다.

## 결과 요약

- 1~4순위 항목 전부 완료. 건너뛴 항목 없음.
- `./gradlew clean test` — **tests=197, skipped=0, failures=0, errors=0**
- **프로덕션 버그는 발견되지 않았다.** 작성한 테스트는 전부 첫 실행에서 통과했다.

## 기준선 숫자 정정

명세의 기준선은 `tests=140, skipped=4`였지만, 작업 시작 시점에 실제로 측정한 값은
**tests=142, skipped=4, failures=0, errors=0**이었다.
(이미 커밋되어 있던 `RedisAssetPriceRoundTripIntegrationTest` 4개 + `ContainerSmokeTest` 2개 포함)
어느 쪽 기준으로도 숫자는 줄지 않고 197까지 늘었다.

## CI 관련 — 사람 확인이 필요한 항목

4순위 세 번째 항목("`.github/workflows/ci.yml`에서 별도 설정 없이 돌아가는지 확인")은
**로컬에서 검증할 수 있는 범위까지만 확인했다.** 실제 GitHub Actions 실행으로는 확인하지 못했다
(명세가 main 브랜치 접근과 머지를 금지하므로 push/PR을 만들지 않았다).

로컬에서 확인한 것:

- `ci.yml`의 `Build & Test` 스텝은 `./gradlew build`를 `INTEGRATION_TEST` 없이 실행한다.
  게이트를 제거했으므로 이제 `PipelineIntegrationTest`가 CI에서 실제로 돌게 된다.
- 컨테이너는 Testcontainers가 직접 띄우므로 워크플로에 추가할 설정은 없다.

사람이 확인해야 할 잠재적 걸림돌 두 가지:

1. **`settings.gradle.kts`의 `includeBuild("../webhook-notify")`**
   이 참조는 main 브랜치에도 이미 있는 선재 조건이다(`f451f53`, `4554f38`).
   GitHub Actions는 이 저장소만 checkout하므로 형제 디렉토리 `../webhook-notify`가 없다.
   현재 CI가 통과하고 있다면 별도 조치가 되어 있는 것이고, 아니라면 이 통합 테스트와 무관하게
   이미 깨져 있던 문제다. 이번 작업에서 건드리지 않았다.

2. **`build.gradle.kts`의 `api.version` 기본값 `1.44`**
   로컬 Docker Engine 29.x(MinAPIVersion 1.40) 대응으로 설정된 값이다.
   GitHub Actions 러너의 Docker가 API 1.44보다 낮으면 반대로 "client version too new"로 거부될 수 있다.
   현재 ubuntu 러너 이미지의 Docker는 1.44 이상을 지원하므로 문제될 가능성은 낮지만,
   러너 이미지가 롤백되면 `DOCKER_API_VERSION` 환경변수로 CI에서 낮춰줄 수 있게 되어 있다.

## 명세와 다르게 처리한 부분

명세는 "컨테이너는 클래스당 `@Container static`으로 재사용한다"고 했지만,
Postgres 테스트 6개 항목과 Kafka 테스트 2개 항목이 여러 클래스로 갈라지면서
`@Container`(클래스 단위 start/stop)를 그대로 쓰면 클래스마다 컨테이너를 새로 띄우게 된다.

명세의 의도가 "부팅 시간을 쓰지 마라"이므로, 다음 두 베이스 클래스에서
Testcontainers 싱글턴 패턴(JVM당 1회 기동, 정리는 Ryuk)을 썼다:

- `AbstractPostgresIntegrationTest` — `schema.sql` 적용 + 테스트마다 `TRUNCATE`
- `AbstractKafkaIntegrationTest` — 테스트마다 새 토픽/새 컨슈머 그룹으로 격리

Redis 테스트 3개와 `PipelineIntegrationTest`는 명세대로 `@Container static`을 유지했다.
전체 스위트 실행 시간은 clean 포함 45초다.

## 추가된 테스트 (총 55개)

| 클래스 | 개수 |
| --- | --- |
| `RedisAssetAnalysisRoundTripIntegrationTest` | 4 |
| `RedisAssetAlertRoundTripIntegrationTest` | 5 |
| `PostgresAssetPriceHistoryIntegrationTest` | 6 |
| `PostgresAssetAnalysisHistoryIntegrationTest` | 6 |
| `PostgresAssetAlertHistoryIntegrationTest` | 7 |
| `PostgresAssetStatisticsReaderIntegrationTest` | 6 |
| `PostgresAssetCompareWindowReaderIntegrationTest` | 9 |
| `PostgresDashboardHistoryMetricsReaderIntegrationTest` | 3 |
| `KafkaAssetPricePublisherIntegrationTest` | 4 |
| `KafkaAssetAnalysisPublisherIntegrationTest` | 5 |

여기에 `PipelineIntegrationTest` 4개가 skipped에서 실행으로 전환됐다.
