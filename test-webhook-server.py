#!/usr/bin/env python3
"""
로컬 웹훅 테스트 서버
asset-radar의 Slack/Discord 알림을 받아서 출력하기
"""
from http.server import HTTPServer, BaseHTTPRequestHandler
import json
from datetime import datetime
import sys

class WebhookHandler(BaseHTTPRequestHandler):
    def do_POST(self):
        content_length = int(self.headers.get('Content-Length', 0))
        body = self.rfile.read(content_length)

        print("\n" + "="*80)
        print(f"⏰ {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
        print(f"📨 경로: {self.path}")
        print(f"📍 클라이언트: {self.client_address[0]}:{self.client_address[1]}")
        print("\n📋 헤더:")
        for header, value in self.headers.items():
            print(f"  {header}: {value}")

        print("\n📦 바디:")
        try:
            payload = json.loads(body)
            print(json.dumps(payload, indent=2, ensure_ascii=False))
        except:
            print(body.decode('utf-8', errors='ignore'))

        print("="*80 + "\n")

        # 200 OK 응답
        self.send_response(200)
        self.send_header('Content-type', 'application/json')
        self.end_headers()
        self.wfile.write(json.dumps({"status": "ok"}).encode())

    def log_message(self, format, *args):
        # 기본 로그 억제
        pass

if __name__ == '__main__':
    port = int(sys.argv[1]) if len(sys.argv) > 1 else 9090
    server = HTTPServer(('127.0.0.1', port), WebhookHandler)
    print(f"""
╔════════════════════════════════════════════════════════════════╗
║          🔔 Asset-Radar 웹훅 테스트 서버 시작                    ║
╚════════════════════════════════════════════════════════════════╝

📍 웹훅 URL: http://localhost:{port}/webhook

이 URL을 아래처럼 설정하세요:

  export ASSET_RADAR_ALERT_SLACK_WEBHOOK_URL='http://localhost:{port}/webhook'
  export ASSET_RADAR_ALERT_DISCORD_WEBHOOK_URL='http://localhost:{port}/webhook'
  export ASSET_RADAR_ALERT_NOTIFIER_SLACK_ENABLED=true
  export ASSET_RADAR_ALERT_NOTIFIER_DISCORD_ENABLED=true

그 다음 asset-radar를 실행하면 여기로 알림이 전송됩니다.

Ctrl+C로 종료

    """)

    try:
        server.serve_forever()
    except KeyboardInterrupt:
        print("\n\n👋 서버 종료")
        sys.exit(0)
