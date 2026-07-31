# APISIX 스터디 오버레이 (1~2단계)

asset-radar 앞에 Apache APISIX 를 세워보는 학습용 구성입니다.
**기존 `docker-compose.yml` 과 애플리케이션 코드는 건드리지 않습니다.** 게이트웨이가 없는
상태와 있는 상태를 언제든 비교할 수 있어야 하기 때문입니다.

| 파일 | 역할 |
|------|------|
| `../docker-compose.apisix.yml` | etcd + apisix 컨테이너 오버레이 |
| `config.yaml` | APISIX 설정. 컨테이너의 `config-default.yaml` 위에 병합된다 |
| `setup-routes.sh` | 2단계 — Admin API 로 Upstream / Service / Route 등록 |

포트: `9080` 프록시 · `9180` Admin API · `9091` 메트릭(5단계) · `2379` etcd
기존 스택(8081 앱, 3001 프론트, 3000 Grafana, 9090 Prometheus …)과 충돌하지 않습니다.

---

## 준비

Admin API 키를 직접 생성합니다. 문서나 블로그의 예시 키를 그대로 쓰지 않기 위해
설정 파일이 아니라 환경변수로 주입합니다.

```bash
cp .env.example .env    # 이미 있으면 생략
echo "APISIX_ADMIN_KEY=$(openssl rand -hex 24)" >> .env
```

값이 비어 있으면 apisix 컨테이너가 아예 뜨지 않습니다. 의도된 동작입니다.

---

## 1단계 — 띄우고, 설정이 어디에 저장되는지 본다

```bash
# 기준선(게이트웨이 없음)을 먼저 띄워서 눈에 익힌다
docker compose -f docker-compose.yml -f docker-compose.demo.yml up --build -d
curl -s localhost:8081/api/latest | head -c 300

# 게이트웨이 추가
docker compose -f docker-compose.yml -f docker-compose.demo.yml -f docker-compose.apisix.yml up -d
```

### 검증

```bash
set -a; source .env; set +a

# Admin API 가 살아있나 — 아직 라우트가 없으니 빈 목록이어야 한다
curl -s -H "X-API-KEY: $APISIX_ADMIN_KEY" http://127.0.0.1:9180/apisix/admin/routes
# → {"list":[],"total":0}

# 라우트가 없으므로 프록시는 404 를 낸다. 이것도 정상 동작 확인이다.
curl -i -s localhost:9080/api/latest | head -1
```

### 이 단계에서 볼 것

**설정이 etcd 에 어떻게 앉는지 직접 확인합니다.** 이걸 한 번 보면 나머지가 쉬워집니다.

```bash
docker compose exec etcd etcdctl get --prefix /apisix --keys-only
```

2단계를 끝낸 뒤 같은 명령을 다시 쳐서, 내가 보낸 JSON 이 어떤 키로 어떤 모양이 되는지
비교하세요.

**기본값을 문서가 아니라 소스에서 확인하는 습관:**

```bash
docker compose exec apisix cat /usr/local/apisix/conf/config-default.yaml | less
```

`config.yaml` 에 적은 것만이 "내가 기본값에서 바꾼 값"입니다. 라우터 구현체
(`router.http`)가 무엇인지도 여기서 확인합니다.

### 과제

- [ ] `deployment.role` 을 `data_plane` + `config_provider: yaml` 로 바꿔 **etcd 없이** 띄워보고,
      etcd 모드와의 차이를 적는다
- [ ] 라우트를 하나 넣고 etcd 를 다시 들여다본다
- [ ] APISIX 를 재시작하지 않고 라우트를 바꿔서, 반영에 리로드가 필요 없다는 걸 확인한다

---

## 2단계 — Route / Service / Upstream 의 경계를 잡는다

```bash
./apisix/setup-routes.sh
```

만들어지는 구성:

```
Route /api/dashboard/stream (priority 20) ─┐
Route /api/statistics/*     (priority 10) ─┼─→ Service asset_api ─→ Upstream app_stable ─→ app:8080
Route /api/*                (priority  0) ─┘
```

| 객체 | 책임 | 라우트를 셋으로 쪼갠 이유 |
|------|------|--------------------------|
| **Route** | 어떤 요청인가 (uri/host/method/vars 매칭 + 이 요청에만 붙는 플러그인) | `/api/dashboard/stream` 은 SSE, `/api/statistics/*` 는 무겁고, 나머지는 평범하다. 앞으로 각각 다른 플러그인이 붙는다 |
| **Service** | Route 들이 공유하는 upstream + 공통 플러그인 | 4단계에서 `cors` 처럼 `/api/*` 전체에 걸 것이 여기로 들어간다 |
| **Upstream** | 어디로 보내는가 (노드, LB, 헬스체크, 타임아웃) | 6단계에서 카나리 노드를 추가할 때 라우트를 안 건드려도 된다 |
| **Consumer** | 누가 호출하는가 | 아직 없음. 4단계에서 생성 |

### 검증

```bash
curl -s localhost:9080/api/latest | head -c 300
curl -s localhost:9080/api/dashboard | head -c 300

# 우선순위와 함께 라우트 목록 보기
curl -s -H "X-API-KEY: $APISIX_ADMIN_KEY" \
  http://127.0.0.1:9180/apisix/admin/routes | jq '.list[].value | {id, uri, priority}'
```

### 과제

`setup-routes.sh` 는 **정답 형태**를 미리 만들어 둔 것입니다. 실제 학습은 이걸 부수면서
합니다.

- [ ] **같은 라우팅을 3가지 방식으로 다시 만들어 본다**
  1. Route 에 `upstream` 인라인
  2. `upstream_id` 참조 (스크립트가 하는 방식의 중간 단계)
  3. `service_id` 참조 (스크립트의 최종 형태)
  → 각각 etcd 에 저장되는 모양이 어떻게 다른가?

- [ ] **priority 를 다 빼고 등록해서** `/api/*` 와 `/api/statistics/*` 가 동시에 맞을 때
      무엇이 이기는지 확인한다. 더 긴 prefix 가 이기는가, 등록 순서가 이기는가?

- [ ] `uri` 의 `*` 는 prefix 매칭이지 정규식이 아니다. 정규식이 필요하면
      `vars` 에 `["uri", "~~", "패턴"]` 을 쓴다. `/api/recommendations/symbol/{symbol}` 을
      정규식으로 잡아본다.

- [ ] **플러그인 병합 순서를 실험으로 확인한다.** Service 와 Route 양쪽에
      `response-rewrite` 를 서로 다른 값으로 걸고 어느 쪽이 이기는지 본다.
      (Consumer > Route > Plugin Config > Service 로 알려져 있지만, 외우지 말고 확인할 것)

- [ ] 프론트엔드(`frontend/nginx.conf`)가 프록시하는 `/swagger-ui`, `/v3/api-docs` 도
      게이트웨이 라우트로 추가해 본다

---

## 3단계 예고 — 여기서부터가 본론

```bash
curl -N http://localhost:8081/api/dashboard/stream   # 앱 직접
curl -N http://localhost:9080/api/dashboard/stream   # 게이트웨이 경유
```

`DashboardController.stream()` 은 `Flux<ServerSentEvent<AssetPrice>>` 를 무한히 내보냅니다.
게이트웨이를 앞에 세우면 높은 확률로 멈춥니다. 응답이 안 오는 게 아니라 프록시가
버퍼에 모으고 있어서입니다.

그리고 이 저장소에는 **이미 같은 문제를 겪은 흔적**이 있습니다 — `frontend/nginx.conf` 의
`proxy_buffering off; chunked_transfer_encoding off;`. APISIX 도 내부가 OpenResty 이므로,
3단계는 "APISIX 설정 배우기"가 아니라 **"APISIX 밑에 깔린 nginx 이해하기"** 가 됩니다.

---

## 정리

```bash
# 게이트웨이만 내리기 (기준선 스택은 유지)
docker compose -f docker-compose.yml -f docker-compose.demo.yml -f docker-compose.apisix.yml \
  stop apisix etcd

# 전부 내리기
docker compose -f docker-compose.yml -f docker-compose.demo.yml -f docker-compose.apisix.yml down
```

---

## 주의

이 오버레이는 **로컬 학습 전용**입니다. 운영에 그대로 쓰면 안 되는 부분:

- etcd 인증 비활성화 (`ALLOW_NONE_AUTHENTICATION=yes`)
- `allow_admin: 0.0.0.0/0` — compose 가 9180 을 루프백에만 바인딩해서 막고 있을 뿐
- etcd 데이터가 볼륨 없이 컨테이너에 있음 → `down` 하면 라우트가 전부 사라짐
  (그래서 `setup-routes.sh` 가 멱등하게 `PUT` 으로 작성돼 있음)
