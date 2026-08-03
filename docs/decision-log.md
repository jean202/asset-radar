# Decision Log

## ADR-001

- 결정: Spring Boot + WebFlux를 기본 런타임으로 사용한다.
- 이유: 실시간/준실시간 수집과 스트림 처리 흐름을 일관되게 가져가기 쉽다.

## ADR-002

- 결정: Kafka, Redis, PostgreSQL, Grafana를 로컬 인프라 기준점으로 둔다.
- 이유: 메시징, 캐시, 이력 저장, 관측성을 초기에 분리해 두는 편이 이후 구조 설명에 유리하다.

## ADR-003

- 결정: 프론트엔드는 React + Vite SPA로 분리하고, 배포 시 Nginx 정적 서빙을 사용한다.
- 이유: 실시간 대시보드와 분석 화면을 빠르게 반복 개발할 수 있고, 백엔드 API와 배포 책임을 분리하기 쉽다.

## ADR-004

- 결정: 이동평균, 변동성, 상관관계, 요약 통계는 PostgreSQL 이력 조회 후 애플리케이션 계층에서 계산한다.
- 이유: 현재 단계에서는 지표 정의를 자주 바꿀 가능성이 높아 DB 집계나 사전 계산보다 애플리케이션 계산이 변경 비용이 낮다.

## ADR-005

- 결정: 운영 배포는 GitHub Actions에서 API/프론트엔드 이미지를 GHCR에 푸시하고, 운영 서버에서 Docker Compose로 이미지를 교체하는 방식으로 자동화한다.
- 이유: 포트폴리오 운영 환경을 특정 클라우드 서비스에 묶지 않으면서도, 이미지 빌드와 런타임 배포를 분리해 재현성을 확보하기 쉽다.

## ADR-006

- 결정: 애플리케이션 secret은 Docker 이미지나 GitHub Actions 빌드 환경에 넣지 않고, 운영 서버의 `.env.prod`에서 런타임 환경변수로만 주입한다.
- 이유: KIS, Alpha Vantage, Finnhub, 알림 webhook, DB 비밀번호가 이미지 레이어나 CI 로그에 남지 않게 하고, 운영 타깃별 값 교체를 배포 워크플로우와 분리하기 위함이다.

## ADR-007

- 결정: `webhook-notify` 의존은 `includeBuild("../webhook-notify")` 복합 빌드를 걷어내고, GitHub Packages에 배포된 `io.github.jean202:webhook-notify-core:0.1.0` 정식 아티팩트로 참조한다.
- 이유: 복합 빌드는 형제 디렉터리 `../webhook-notify`가 존재하는 로컬 워크스페이스에서만 동작해서, 신규 클론과 CI, Docker 이미지 빌드가 모두 `Included build does not exist`로 실패했다. 실제로 CI는 2026-07-20 이후 계속 red 상태였다. 버전이 박힌 아티팩트로 바꾸면 저장소 하나만 클론해도 빌드가 재현된다.
- 트레이드오프: GitHub Packages는 public 패키지도 인증을 요구하므로 빌드에 `read:packages` 토큰이 필요해졌다. CI/Actions에서는 기본 `GITHUB_TOKEN`으로 해결되지만, 외부 사용자는 PAT를 만들어야 한다. 익명 접근이 필요해지면 Maven Central로 옮긴다 — 배포 설정에 Central 필수 메타데이터와 서명 경로는 이미 준비해 두었다.
