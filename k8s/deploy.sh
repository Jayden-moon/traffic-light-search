#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
CLUSTER_NAME="traffic-light"

echo "============================================"
echo "  Traffic Light Search - K8s 로컬 배포"
echo "============================================"

# 1. Kind 클러스터 생성
echo ""
echo "[1/6] Kind 클러스터 확인..."
if kind get clusters 2>/dev/null | grep -q "$CLUSTER_NAME"; then
    echo "  클러스터 '$CLUSTER_NAME' 이미 존재. 스킵."
else
    echo "  클러스터 생성 중..."
    kind create cluster --config "$SCRIPT_DIR/kind-config.yaml"
    echo "  클러스터 생성 완료."
fi

kubectl cluster-info --context "kind-$CLUSTER_NAME"

# 2. Backend 이미지 빌드
echo ""
echo "[2/6] Backend Docker 이미지 빌드..."

# 데이터 파일을 backend/data로 복사 (Docker 빌드 컨텍스트에 포함)
if [ -d "$PROJECT_DIR/data" ]; then
    mkdir -p "$PROJECT_DIR/backend/data"
    cp -f "$PROJECT_DIR/data/"*.json "$PROJECT_DIR/backend/data/" 2>/dev/null || true
    echo "  데이터 파일 복사 완료."
fi

docker build -t traffic-light-backend:latest "$PROJECT_DIR/backend"

# 빌드 후 임시 데이터 정리
rm -rf "$PROJECT_DIR/backend/data"
kind load docker-image traffic-light-backend:latest --name "$CLUSTER_NAME"
echo "  Backend 이미지 로드 완료."

# 3. Frontend 이미지 빌드
echo ""
echo "[3/6] Frontend Docker 이미지 빌드..."
docker build \
    --build-arg NEXT_PUBLIC_API_URL=http://localhost:30080/api \
    -t traffic-light-frontend:latest \
    "$PROJECT_DIR/frontend"
kind load docker-image traffic-light-frontend:latest --name "$CLUSTER_NAME"
echo "  Frontend 이미지 로드 완료."

# 4. K8s 리소스 배포
echo ""
echo "[4/6] Kubernetes 리소스 배포..."
kubectl apply -f "$SCRIPT_DIR/namespace.yaml"
kubectl apply -f "$SCRIPT_DIR/elasticsearch.yaml"

echo "  Elasticsearch 준비 대기 중 (nori 플러그인 설치 포함, 최대 5분)..."
kubectl -n traffic-light wait --for=condition=ready pod -l app=elasticsearch --timeout=300s

kubectl apply -f "$SCRIPT_DIR/backend.yaml"

echo "  Backend 준비 대기 중..."
kubectl -n traffic-light wait --for=condition=ready pod -l app=backend --timeout=120s

kubectl apply -f "$SCRIPT_DIR/frontend.yaml"

echo "  Frontend 준비 대기 중..."
kubectl -n traffic-light wait --for=condition=ready pod -l app=frontend --timeout=60s

# 5. 데이터 인덱싱 (K8s Job)
echo ""
echo "[5/6] 인덱싱 Job 실행..."
kubectl delete job es-indexing -n traffic-light --ignore-not-found > /dev/null 2>&1
kubectl apply -f "$SCRIPT_DIR/indexing-job.yaml"

echo "  인덱싱 완료 대기 중 (최대 3분)..."
kubectl -n traffic-light wait --for=condition=complete job/es-indexing --timeout=180s 2>/dev/null && {
    echo "  인덱싱 결과:"
    kubectl -n traffic-light logs job/es-indexing -c indexing --tail=5
} || {
    echo "  인덱싱 Job 로그:"
    kubectl -n traffic-light logs job/es-indexing --all-containers --tail=10
}

# 6. 상태 확인
echo ""
echo "[6/6] 배포 상태 확인..."
echo ""
kubectl -n traffic-light get pods
echo ""
kubectl -n traffic-light get svc

echo ""
echo "============================================"
echo "  배포 완료!"
echo ""
echo "  Frontend:      http://localhost:30000"
echo "  Backend API:   http://localhost:30080/api"
echo "  Elasticsearch: http://localhost:39200"
echo ""
echo "  상태 확인:  kubectl -n traffic-light get pods"
echo "  로그 확인:  kubectl -n traffic-light logs -f deploy/backend"
echo "  클러스터 삭제: kind delete cluster --name $CLUSTER_NAME"
echo "============================================"
