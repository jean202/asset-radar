#!/usr/bin/env python3
from http.server import HTTPServer, BaseHTTPRequestHandler
import json
from datetime import datetime

class Handler(BaseHTTPRequestHandler):
    def do_POST(self):
        content_len = int(self.headers.get('content-length', 0))
        body = self.rfile.read(content_len)

        print("\n" + "="*80)
        print(f"⏰ {datetime.now().strftime('%H:%M:%S')} | 🔔 웹훅 수신!")
        print(f"📍 경로: {self.path}")
        print("\n📦 페이로드:")
        try:
            data = json.loads(body)
            print(json.dumps(data, indent=2, ensure_ascii=False))
        except:
            print(body.decode('utf-8', errors='ignore'))
        print("="*80 + "\n")

        self.send_response(200)
        self.send_header('content-type', 'application/json')
        self.end_headers()
        self.wfile.write(json.dumps({"status": "ok"}).encode())

    def log_message(self, *args):
        pass

if __name__ == '__main__':
    import sys
    port = int(sys.argv[1]) if len(sys.argv) > 1 else 9090
    server = HTTPServer(('127.0.0.1', port), Handler)
    print(f"\n🚀 웹훅 서버 시작: http://localhost:{port}\n")
    server.serve_forever()
