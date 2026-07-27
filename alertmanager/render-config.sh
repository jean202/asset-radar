#!/bin/sh
set -eu

SLACK_URL="${ASSET_RADAR_ALERT_SLACK_WEBHOOK_URL:-}"
WEBHOOK_URL="${ASSET_RADAR_ALERT_WEBHOOK_URL:-}"
OUT=/tmp/alertmanager.generated.yml

cat > "$OUT" <<'EOF'
global:
  resolve_timeout: 5m

route:
  receiver: default
  group_by: ['alertname', 'job']
  group_wait: 30s
  group_interval: 5m
  repeat_interval: 3h
  routes:
    - matchers:
        - severity = "critical"
      receiver: critical
    - matchers:
        - severity = "warning"
      receiver: warning

receivers:
  - name: default
  - name: warning
EOF

if [ -n "$SLACK_URL" ]; then
  cat >> "$OUT" <<EOF
    slack_configs:
      - api_url: '$SLACK_URL'
        channel: '#asset-radar-ops'
        send_resolved: true
        title: '[{{ .Status | toUpper }}] {{ .CommonLabels.alertname }}'
        text: '{{ range .Alerts }}{{ .Annotations.description }}
{{ end }}'
EOF
fi

echo "  - name: critical" >> "$OUT"

if [ -n "$SLACK_URL" ]; then
  cat >> "$OUT" <<EOF
    slack_configs:
      - api_url: '$SLACK_URL'
        channel: '#asset-radar-ops'
        send_resolved: true
        title: '[{{ .Status | toUpper }}] {{ .CommonLabels.alertname }}'
        text: '{{ range .Alerts }}{{ .Annotations.description }}
{{ end }}'
EOF
fi

if [ -n "$WEBHOOK_URL" ]; then
  cat >> "$OUT" <<EOF
    webhook_configs:
      - url: '$WEBHOOK_URL'
        send_resolved: true
EOF
fi

if [ -z "$SLACK_URL" ] && [ -z "$WEBHOOK_URL" ]; then
  echo "no ASSET_RADAR_ALERT_SLACK_WEBHOOK_URL / ASSET_RADAR_ALERT_WEBHOOK_URL set, alerts route to a no-op receiver" >&2
fi

exec /bin/alertmanager --config.file="$OUT" --storage.path=/alertmanager
