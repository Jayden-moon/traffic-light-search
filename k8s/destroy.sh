#!/bin/bash
set -e

CLUSTER_NAME="traffic-light"

echo "============================================"
echo "  Traffic Light Search - K8s 환경 삭제"
echo "============================================"

if kind get clusters 2>/dev/null | grep -q "$CLUSTER_NAME"; then
    echo "클러스터 '$CLUSTER_NAME' 삭제 중..."
    kind delete cluster --name "$CLUSTER_NAME"
    echo "삭제 완료."
else
    echo "클러스터 '$CLUSTER_NAME'가 존재하지 않습니다."
fi

echo ""
echo "Docker 이미지 정리..."
docker rmi traffic-light-backend:latest 2>/dev/null || true
docker rmi traffic-light-frontend:latest 2>/dev/null || true
echo "완료."
