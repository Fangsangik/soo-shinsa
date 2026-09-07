#!/bin/bash

echo "📊 Grafana 대시보드 생성 중..."

# Dashboard JSON 생성
DASHBOARD_JSON='{
  "dashboard": {
    "id": null,
    "title": "🔥 SooShinsa 실시간 TPS 모니터링",
    "tags": ["performance", "jmeter", "spring-boot"],
    "timezone": "browser",
    "refresh": "1s",
    "time": {
      "from": "now-5m",
      "to": "now"
    },
    "panels": [
      {
        "id": 1,
        "title": "🚀 실시간 TPS",
        "type": "stat",
        "targets": [
          {
            "expr": "rate(http_server_requests_seconds_count{application=\"soo-shinsa\"}[10s])",
            "legendFormat": "TPS",
            "refId": "A"
          }
        ],
        "fieldConfig": {
          "defaults": {
            "unit": "reqps",
            "min": 0,
            "decimals": 1,
            "color": {
              "mode": "thresholds"
            },
            "thresholds": {
              "mode": "absolute",
              "steps": [
                {"color": "red", "value": 0},
                {"color": "yellow", "value": 20},
                {"color": "green", "value": 50},
                {"color": "super-light-green", "value": 80}
              ]
            }
          }
        },
        "options": {
          "colorMode": "background",
          "graphMode": "area",
          "justifyMode": "center"
        },
        "gridPos": {"h": 8, "w": 12, "x": 0, "y": 0}
      },
      {
        "id": 2,
        "title": "⚡ 평균 응답시간",
        "type": "stat",
        "targets": [
          {
            "expr": "rate(http_server_requests_seconds_sum{application=\"soo-shinsa\"}[10s]) / rate(http_server_requests_seconds_count{application=\"soo-shinsa\"}[10s]) * 1000",
            "legendFormat": "평균 응답시간",
            "refId": "A"
          }
        ],
        "fieldConfig": {
          "defaults": {
            "unit": "ms",
            "min": 0,
            "decimals": 1,
            "color": {
              "mode": "thresholds"
            },
            "thresholds": {
              "mode": "absolute",
              "steps": [
                {"color": "green", "value": 0},
                {"color": "yellow", "value": 100},
                {"color": "red", "value": 500}
              ]
            }
          }
        },
        "options": {
          "colorMode": "background",
          "graphMode": "area",
          "justifyMode": "center"
        },
        "gridPos": {"h": 8, "w": 12, "x": 12, "y": 0}
      },
      {
        "id": 3,
        "title": "📈 TPS 실시간 그래프",
        "type": "timeseries",
        "targets": [
          {
            "expr": "rate(http_server_requests_seconds_count{application=\"soo-shinsa\"}[10s])",
            "legendFormat": "TPS",
            "refId": "A"
          }
        ],
        "fieldConfig": {
          "defaults": {
            "unit": "reqps",
            "min": 0,
            "color": {
              "mode": "palette-classic"
            }
          }
        },
        "options": {
          "legend": {
            "displayMode": "table",
            "placement": "right",
            "calcs": ["lastNotNull", "max", "mean"]
          }
        },
        "gridPos": {"h": 8, "w": 24, "x": 0, "y": 8}
      },
      {
        "id": 4,
        "title": "🎯 HTTP 상태별 요청",
        "type": "timeseries",
        "targets": [
          {
            "expr": "rate(http_server_requests_seconds_count{application=\"soo-shinsa\", status=\"200\"}[10s])",
            "legendFormat": "성공 (200)",
            "refId": "A"
          },
          {
            "expr": "rate(http_server_requests_seconds_count{application=\"soo-shinsa\", status!=\"200\"}[10s])",
            "legendFormat": "오류 (!200)",
            "refId": "B"
          }
        ],
        "fieldConfig": {
          "defaults": {
            "unit": "reqps",
            "min": 0
          }
        },
        "gridPos": {"h": 8, "w": 24, "x": 0, "y": 16}
      }
    ]
  },
  "message": "SooShinsa 실시간 성능 모니터링 대시보드",
  "overwrite": true
}'

# 대시보드 생성
curl -X POST \
  -H "Content-Type: application/json" \
  -d "$DASHBOARD_JSON" \
  http://admin:admin123@localhost:3000/api/dashboards/db

echo ""
echo "✅ 대시보드 생성 완료!"
echo "🌐 Grafana: http://localhost:3000"
echo "👤 사용자: admin"
echo "🔐 비밀번호: admin123"