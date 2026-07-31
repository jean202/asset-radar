#!/bin/bash

# APISIX 스터디 2단계 — Route / Service / Upstream 을 Admin API 로 구성한다.
# Prerequisites: docker-compose.apisix.yml 로 apisix/etcd 가 떠 있을 것

set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ADMIN_BASE="${APISIX_ADMIN_BASE:-http://127.0.0.1:9180/apisix/admin}"

# 키는 환경변수 우선, 없으면 프로젝트 루트 .env 에서 읽는다.
if [[ -z "${APISIX_ADMIN_KEY:-}" && -f "$PROJECT_DIR/.env" ]]; then
  APISIX_ADMIN_KEY="$(grep -E '^APISIX_ADMIN_KEY=' "$PROJECT_DIR/.env" | tail -1 | cut -d= -f2-)"
fi

if [[ -z "${APISIX_ADMIN_KEY:-}" ]]; then
  echo "❌ APISIX_ADMIN_KEY 가 없습니다. apisix/README.md 의 '준비' 절을 먼저 수행하세요."
  exit 1
fi

RESP_FILE="$(mktemp)"
trap 'rm -f "$RESP_FILE"' EXIT

# 사전 점검 — Admin API 가 응답하지 않으면 curl 에러 대신 무엇을 해야 하는지 알려준다.
# curl 은 연결 실패 시에도 %{http_code} 로 000 을 찍는다. || 뒤에 또 echo 하면 겹친다.
PREFLIGHT="$(curl -sS -o /dev/null -w '%{http_code}' -H "X-API-KEY: $APISIX_ADMIN_KEY" \
  "$ADMIN_BASE/routes" 2>/dev/null || true)"
PREFLIGHT="${PREFLIGHT:-000}"
case "$PREFLIGHT" in
  2*) ;;
  401|403)
    echo "❌ Admin API 인증 실패 (HTTP $PREFLIGHT)."
    echo "   .env 의 APISIX_ADMIN_KEY 와 apisix 컨테이너가 기동될 때의 값이 다릅니다."
    echo "   키를 바꿨다면 컨테이너를 다시 만드세요: docker compose ... up -d --force-recreate apisix"
    exit 1
    ;;
  000)
    echo "❌ Admin API($ADMIN_BASE)에 연결할 수 없습니다."
    echo "   먼저 게이트웨이를 띄우세요:"
    echo "   docker compose -f docker-compose.yml -f docker-compose.demo.yml -f docker-compose.apisix.yml up -d"
    exit 1
    ;;
  *)
    echo "❌ Admin API 가 예상치 못한 응답을 했습니다 (HTTP $PREFLIGHT)."
    exit 1
    ;;
esac

admin_put() {
  local path="$1" desc="$2" body="$3"
  local code
  code="$(curl -sS -o "$RESP_FILE" -w '%{http_code}' \
    -X PUT "$ADMIN_BASE$path" \
    -H "X-API-KEY: $APISIX_ADMIN_KEY" \
    -H 'Content-Type: application/json' \
    -d "$body")"

  if [[ "$code" == 2* ]]; then
    echo "   ✅ $desc"
  else
    echo "   ❌ $desc (HTTP $code)"
    cat "$RESP_FILE"
    echo ""
    exit 1
  fi
}

echo "🧭 APISIX 2단계 — Route / Service / Upstream 구성"
echo "================================================"
echo ""

# ── 1. Upstream — "어디로 보내는가" ────────────────────────────────────────
# 라우트에 upstream 을 인라인으로 박을 수도 있지만, 별도 객체로 빼면 6단계에서
# 카나리 노드를 추가할 때 라우트를 건드리지 않아도 된다.
echo "1️⃣  Upstream 등록 (app_stable)..."
admin_put "/upstreams/app_stable" "upstream app_stable → app:8080" '{
  "type": "roundrobin",
  "nodes": { "app:8080": 1 }
}'
# 여기에 timeout 을 주지 않은 것은 의도적이다.
# 3단계에서 upstream.timeout.read 를 짧게 줘 보고 SSE 스트림이 끊기는지 확인한다.
echo ""

# ── 2. Service — 여러 Route 가 공유하는 묶음 ──────────────────────────────
# 지금은 upstream 참조만 담고 있어 비어 보이지만, 4단계에서 cors 처럼
# /api/* 전체에 공통으로 걸 플러그인이 여기 들어간다.
echo "2️⃣  Service 등록 (asset_api)..."
admin_put "/services/asset_api" "service asset_api → upstream app_stable" '{
  "upstream_id": "app_stable"
}'
echo ""

# ── 3. Route — "어떤 요청인가" ────────────────────────────────────────────
# 라우트를 셋으로 쪼갠 이유는 각각 다른 취급이 필요하기 때문이다.
#   - /api/dashboard/stream : SSE. 버퍼링/타임아웃/캐시를 따로 다뤄야 한다 (3단계)
#   - /api/statistics/*     : 무거운 연산. rate limit 과 캐시 대상 (4단계)
#   - /api/*                : 나머지 전부
#
# ⚠️ priority 를 여기 박아두었지만, 처음에는 priority 를 빼고 등록해서
#    "/api/* 와 /api/statistics/* 가 동시에 맞을 때 무엇이 이기는지"를 먼저 실험할 것.
#    더 긴 prefix 가 이기는지, 등록 순서가 이기는지 직접 확인한 뒤에 이 값을 넣는 게 순서다.
echo "3️⃣  Route 등록 (3개)..."

admin_put "/routes/dashboard-stream" "route /api/dashboard/stream (priority 20)" '{
  "name": "dashboard-stream",
  "desc": "SSE 스트림. 3단계에서 버퍼링을 다룬다.",
  "uri": "/api/dashboard/stream",
  "priority": 20,
  "service_id": "asset_api"
}'

admin_put "/routes/statistics" "route /api/statistics/* (priority 10)" '{
  "name": "statistics",
  "desc": "무거운 통계 연산. 4단계 rate limit / proxy-cache 대상.",
  "uri": "/api/statistics/*",
  "priority": 10,
  "service_id": "asset_api"
}'

admin_put "/routes/api-all" "route /api/* (priority 0)" '{
  "name": "api-all",
  "desc": "나머지 REST 전부.",
  "uri": "/api/*",
  "priority": 0,
  "service_id": "asset_api"
}'
echo ""

# ── 4. 검증 ───────────────────────────────────────────────────────────────
echo "4️⃣  검증..."

LATEST="$(curl -sS "http://127.0.0.1:9080/api/latest" || echo "FAIL")"
if [[ "$LATEST" == *"["* ]]; then
  echo "   ✅ GET :9080/api/latest 응답"
else
  echo "   ❌ GET :9080/api/latest 실패 — app 컨테이너가 떠 있는지 확인하세요"
  exit 1
fi

DASHBOARD="$(curl -sS "http://127.0.0.1:9080/api/dashboard" || echo "FAIL")"
if [[ "$DASHBOARD" == *"assets"* ]]; then
  echo "   ✅ GET :9080/api/dashboard 응답"
else
  echo "   ❌ GET :9080/api/dashboard 실패"
  exit 1
fi

echo ""
echo "✅ 2단계 구성 완료"
echo ""
echo "다음으로 해 볼 것:"
echo "  # 등록된 라우트를 우선순위와 함께 보기"
echo "  curl -s -H \"X-API-KEY: \$APISIX_ADMIN_KEY\" $ADMIN_BASE/routes | jq '.list[].value | {id, uri, priority}'"
echo ""
echo "  # ★ etcd 에 실제로 어떻게 저장됐는지 직접 보기 (1단계 하이라이트)"
echo "  docker compose exec etcd etcdctl get --prefix /apisix --keys-only"
echo "  docker compose exec etcd etcdctl get /apisix/routes/api-all"
echo ""
echo "  # 3단계 예고 — 직접 호출과 게이트웨이 경유의 차이를 본다"
echo "  curl -N http://localhost:8081/api/dashboard/stream   # 직접"
echo "  curl -N http://localhost:9080/api/dashboard/stream   # 게이트웨이 경유"
