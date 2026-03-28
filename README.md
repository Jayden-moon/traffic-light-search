# Traffic Light Search System

> 전국 신호등 표준데이터 50,000건을 Elasticsearch로 검색/시각화하는 풀스택 프로젝트
>
> Built with [Claude Code](https://claude.ai/claude-code) - Anthropic's AI coding assistant

공공데이터포털의 전국신호등표준데이터를 활용하여, 텍스트 검색 / 위치 기반 검색 / 지도 시각화 / 통계 대시보드를 제공합니다.

## Tech Stack

**Backend**

![Java](https://img.shields.io/badge/Java-17-ED8B00?logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2-6DB33F?logo=springboot&logoColor=white)
![Gradle](https://img.shields.io/badge/Gradle-Multi--Module-02303A?logo=gradle&logoColor=white)
![Elasticsearch](https://img.shields.io/badge/Elasticsearch-8.13-005571?logo=elasticsearch&logoColor=white)

**Frontend**

![Next.js](https://img.shields.io/badge/Next.js-14-000000?logo=nextdotjs&logoColor=white)
![TypeScript](https://img.shields.io/badge/TypeScript-5-3178C6?logo=typescript&logoColor=white)
![Tailwind CSS](https://img.shields.io/badge/Tailwind%20CSS-3-06B6D4?logo=tailwindcss&logoColor=white)
![Leaflet](https://img.shields.io/badge/Leaflet-Map-199900?logo=leaflet&logoColor=white)

**Infra**

![Docker](https://img.shields.io/badge/Docker%20Compose-ES%20%2B%20Kibana-2496ED?logo=docker&logoColor=white)

**Security & Testing**

![Spring Security](https://img.shields.io/badge/Spring%20Security-API%20Key-6DB33F?logo=springsecurity&logoColor=white)
![JUnit5](https://img.shields.io/badge/JUnit%205-71%20tests-25A162?logo=junit5&logoColor=white)
![Jest](https://img.shields.io/badge/Jest-33%20tests-C21325?logo=jest&logoColor=white)

## Features

- **전문 검색** - Elasticsearch nori 분석기를 활용한 한국어 주소 검색
- **복합 필터** - 시도/시군구/도로종류/신호등구분 캐스케이딩 필터
- **위치 검색** - geo_point 기반 반경 검색 + 거리순 정렬
- **지도 시각화** - Leaflet + 마커 클러스터링으로 신호등 위치 표시
- **통계 대시보드** - 시도별/도로종류별 분포 차트 (Recharts)
- **벌크 인덱싱** - 50,000건 스트리밍 파싱 + 1,000건 단위 벌크 처리
- **API 연동 준비** - 공공데이터 API upsert 기반 동기화 구조
- **인증/인가** - Spring Security + API Key 기반 관리 엔드포인트 보호
- **Rate Limiting** - IP 기반 요청 제한 (공개 60회/분, 관리 10회/분)
- **입력 검증** - 페이지네이션/좌표/거리 파라미터 서버사이드 검증
- **테스트** - 백엔드 38개 + 프론트엔드 33개 = 71개 테스트 케이스

## Architecture

### System Overview

```
┌──────────────┐     ┌──────────────────┐     ┌─────────────────┐
│   Next.js    │────→│   Spring Boot    │────→│  Elasticsearch  │
│  :3000       │ API │  :8080           │ ES  │  :9200          │
│              │     │                  │     │                 │
│ - 검색 페이지 │     │ - REST API       │     │ - 전문 검색      │
│ - 지도 (Leaf)│     │ - 클린아키텍처    │     │ - 집계          │
│ - 대시보드   │     │ - 멀티모듈 Gradle │     │ - geo_point     │
└──────────────┘     └──────────────────┘     └─────────────────┘
```

### Backend Clean Architecture (Multi-Module Gradle)

```
┌─────────────────────────────────────────────────────────┐
│ trafficlight-api          Presentation Layer             │
│ Spring Boot App, Controllers, CORS, UseCase Bean 등록    │
│ 의존: domain + usecase / runtimeOnly: data               │
└────────────────────────────┬────────────────────────────┘
                             │
┌────────────────────────────▼────────────────────────────┐
│ trafficlight-usecase       Application Layer             │
│ SearchUseCase, IndexUseCase, AggregationUseCase (POJO)  │
│ 의존: domain만                                           │
└────────────────────────────┬────────────────────────────┘
                             │
┌────────────────────────────▼────────────────────────────┐
│ trafficlight-domain        Domain Layer                  │
│ TrafficLight 엔티티, Service 인터페이스(포트), 도메인 예외  │
│ 의존: 없음 (순수 Java)                                    │
└────────────────────────────▲────────────────────────────┘
                             │
┌────────────────────────────┴────────────────────────────┐
│ trafficlight-data          Infrastructure Layer          │
│ ES Adapters, JsonDataLoader, PublicDataApiAdapter        │
│ 의존: domain만                                           │
└─────────────────────────────────────────────────────────┘
```

**의존성 규칙**: `api → usecase → domain ← data` (빌드 시스템이 강제)

| 원칙 | 구현 |
|------|------|
| Domain은 프레임워크 비의존 | Spring, ES 등 외부 import 없음 |
| UseCase는 POJO | `@Component` 없이 순수 클래스, api의 Config에서 `@Bean` 등록 |
| Data는 runtimeOnly | api에서 컴파일 시 직접 참조 불가 |
| 의존성 역전 | Domain이 인터페이스 정의, Data가 구현 |

## Project Structure

```
traffic-light-search/
├── docker-compose.yml                  # ES 8.13 + Kibana + nori
├── data/
│   └── 전국신호등표준데이터.json          # 원본 데이터 (50,000건)
│
├── backend/
│   ├── build.gradle                    # 루트 (공통 설정)
│   ├── settings.gradle                 # 4개 모듈 include
│   │
│   ├── trafficlight-domain/            # Domain Layer
│   │   └── com.trafficlight.domain
│   │       ├── model/TrafficLight      # 엔티티 (34개 필드 + 좌표 검증)
│   │       ├── service/                # 포트 인터페이스 4개
│   │       └── core/exception/         # 도메인 예외 3개
│   │
│   ├── trafficlight-usecase/           # Application Layer
│   │   └── com.trafficlight.usecase
│   │       ├── SearchUseCase           # 검색 + 필터 오케스트레이션
│   │       ├── IndexUseCase            # 인덱싱 + 상태 조회
│   │       ├── AggregationUseCase      # 집계 결과 변환
│   │       ├── DataSyncUseCase         # API 동기화 (upsert)
│   │       └── dto/                    # Command, Result 5개
│   │
│   ├── trafficlight-data/              # Infrastructure Layer
│   │   └── com.trafficlight.data
│   │       ├── elasticsearch/          # ES 어댑터 3개 (Search, Index, Aggregation)
│   │       ├── loader/JsonDataLoader   # 스트리밍 파서 + 한→영 변환
│   │       ├── api/PublicDataApiAdapter # 공공데이터 API 클라이언트
│   │       └── config/                 # ES 클라이언트 설정
│   │
│   └── trafficlight-api/               # Presentation Layer
│       └── com.trafficlight.api
│           ├── controller/             # REST 컨트롤러 4개
│           ├── dto/                    # 응답 DTO 3개
│           └── config/                 # Security, RateLimit, CORS, ExceptionHandler
│
├── frontend/
│   └── src/
│       ├── app/                        # Pages (search, map, dashboard)
│       ├── components/                 # UI 컴포넌트 8개
│       ├── lib/api.ts                  # API 클라이언트
│       ├── types/trafficLight.ts       # TypeScript 인터페이스
│       └── __tests__/                  # Jest 테스트 (33개)
│
├── doc/
│   └── API_SPECIFICATION.md            # REST API 명세서
├── TEST_DOCUMENTATION.md               # 테스트 설계 문서
└── SECURITY_AUDIT.md                   # 보안 점검 보고서
```

## Getting Started

### Prerequisites

- Docker & Docker Compose
- Java 17+
- Node.js 18+

### 0. Download Data

`data/` 디렉토리에 원본 JSON 파일을 다운로드합니다. (55MB, .gitignore 대상)

```bash
mkdir -p data
# 공공데이터포털에서 다운로드:
# https://www.data.go.kr/data/15007122/standard.do
# → JSON 다운로드 → data/전국신호등표준데이터.json 으로 저장
```

> 다운로드 링크: [전국신호등표준데이터 (공공데이터포털)](https://www.data.go.kr/data/15007122/standard.do)

### 1. Run Elasticsearch

```bash
docker-compose up -d
# ES: http://localhost:9200 | Kibana: http://localhost:5601
```

### 2. Run Backend

```bash
cd backend
```

프로젝트 루트(`backend/`)에 `.env` 파일을 생성하고 환경변수를 설정합니다:

```bash
# backend/.env
SERVICE_KEY=your_public_data_api_service_key
```

> `SERVICE_KEY`는 [공공데이터포털](https://www.data.go.kr)에서 발급받은 API 인증키입니다.

```bash
./gradlew :trafficlight-api:bootRun
# API: http://localhost:8080
```

### 3. Run Frontend

```bash
cd frontend
npm install && npm run dev
# Web: http://localhost:3000
```

### 4. Index Data

관리 엔드포인트는 `X-Admin-Api-Key` 헤더가 필요합니다.

```bash
curl -X POST http://localhost:8080/api/index/create \
  -H "X-Admin-Api-Key: dev-admin-key-change-in-production"

curl -X POST http://localhost:8080/api/index/load \
  -H "X-Admin-Api-Key: dev-admin-key-change-in-production"
# 50,000건 인덱싱 (약 3~5초)
```

또는 웹 헤더의 **데이터 인덱싱** 버튼 클릭.

> 프로덕션 배포 시 `ADMIN_API_KEY` 환경변수를 반드시 변경하세요.

## API Endpoints

### Search

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/search?q=강남&sidoName=서울특별시&page=0&size=20` | 텍스트 검색 + 필터 |
| `GET` | `/api/search/geo?lat=37.5&lon=127.0&distance=5km` | 위치 기반 반경 검색 |
| `GET` | `/api/search/filters/{field}` | 필터 옵션 목록 |

### Index Management (인증 필요)

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/index/create` | 인덱스 생성 (nori 매핑 적용) |
| `POST` | `/api/index/load` | JSON 파일 벌크 인덱싱 |
| `DELETE` | `/api/index/delete` | 인덱스 삭제 |
| `GET` | `/api/index/status` | 인덱스 상태 조회 (인증 불필요) |

### Aggregations

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/aggregations/by-region` | 시도별 (시군구 하위 집계 포함) |
| `GET` | `/api/aggregations/by-road-type` | 도로종류별 |
| `GET` | `/api/aggregations/by-signal-type` | 신호등구분별 |
| `GET` | `/api/aggregations/summary` | 통합 집계 |

### Data Sync (인증 필요)

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/sync` | 공공데이터 API 동기화 (ID 기반 upsert) |

> 전체 API 명세는 [doc/API_SPECIFICATION.md](doc/API_SPECIFICATION.md)를 참조하세요.

## Elasticsearch Design

### Index Mapping

```
traffic-lights/
├── keyword (26개)  : sidoName, sigunguName, roadType, trafficLightCategory ...
├── text+nori (5개) : roadNameAddress, lotNumberAddress, roadRouteName, managementAgency ...
├── geo_point (1개) : location {lat, lon}
├── float (2개)     : latitude, longitude
└── date (1개)      : dataReferenceDate
```

### Query Patterns

**복합 검색**: `BoolQuery = must(multi_match) + filter(term)`

```json
{
  "bool": {
    "must": [{ "multi_match": { "query": "강남", "fields": ["roadNameAddress", "lotNumberAddress"] }}],
    "filter": [{ "term": { "sidoName": "서울특별시" }}]
  }
}
```

**위치 검색**: `GeoDistanceQuery + sort(_geo_distance)`

**집계**: `terms(sidoName) → sub-agg terms(sigunguName)`

## Data Pipeline

```
원본 JSON (55MB, 한글 키)
  │ JsonDataLoader (스트리밍)
  ├── 한글 → 영문 필드 변환 (34개)
  ├── 위도/경도 → geo_point 변환
  └── 좌표 유효성 검증 (한국 범위)
  │
  ▼ BulkRequest (1,000건/batch)
Elasticsearch 인덱스 (50,000 문서)
```

## Data Source

- **출처**: [공공데이터포털 - 전국신호등표준데이터](https://www.data.go.kr)
- **규모**: 50,000건 / 11개 시도 / 34개 필드
- **주요 필드**: 시도명, 시군구명, 도로종류, 위도/경도, 신호등구분, 관리기관명

## Key Technical Decisions

| 결정 | 이유 |
|------|------|
| 멀티모듈 Gradle | 빌드 시스템으로 클린 아키텍처 의존성 규칙 강제 |
| UseCase를 POJO로 | Spring 비의존 → 단위 테스트 용이, 프레임워크 교체 가능 |
| ES Java Client (새 API) | deprecated된 RestHighLevelClient 대신 co.elastic.clients 사용 |
| Jackson 스트리밍 파서 | 55MB JSON 전체 메모리 로드 방지 |
| geo_point + 클러스터링 | 50,000개 마커 렌더링 성능 해결 |
| 한글→영문 필드 변환 | API/프론트 전구간 영문 통일, 원본은 JsonDataLoader에서 변환 |
| ID 기반 upsert | API 동기화 시 중복 인덱싱 방지 |
| nori 분석기 | 한국어 주소 형태소 분석 (띄어쓰기 무관 검색) |
| Spring Security + API Key | 관리 엔드포인트 인증, constant-time 비교로 타이밍 공격 방어 |
| Rate Limiting | ConcurrentHashMap 기반 IP별 요청 제한, 분산 환경 시 Redis로 교체 가능 |
| GlobalExceptionHandler | 스택트레이스 노출 차단, 안전한 에러 메시지만 반환 |
| FieldNameMapping 공유 클래스 | 한글→영문 필드 매핑 중복 제거, domain 모듈에 배치 |

## Security

OWASP Top 10 기반 보안 점검을 수행하고 취약점을 수정했습니다.

| 항목 | 대응 |
|------|------|
| A01: Broken Access Control | API Key 인증 + 역할 기반 엔드포인트 보호 |
| A02: Cryptographic Failures | 환경변수 기반 비밀 관리, HTTPS 외부 API 호출 |
| A03: Injection | `@Min`/`@Max`/`@DecimalMin`/`@DecimalMax`/`@Pattern` 입력 검증 |
| A04: Insecure Design | GlobalExceptionHandler, 안전한 에러 메시지 |
| A05: Security Misconfiguration | 보안 헤더, CORS 명시적 설정, ES `dynamic: strict` |
| A07: Auth Failures | 시작 시 Admin Key 검증, fail-fast |

## Testing

### 실행

```bash
# 백엔드 (38개 테스트)
cd backend && ./gradlew test

# 프론트엔드 (33개 테스트)
cd frontend && npm test
```

### 테스트 구조

| 레이어 | 유형 | 테스트 수 |
|--------|------|-----------|
| Domain (Model, Exception, FieldNameMapping) | 순수 단위 테스트 | 25개 |
| UseCase (Search, Index, Aggregation, DataSync) | Mockito 단위 테스트 | 27개 |
| Controller (4개 + Security 통합) | MockMvc 슬라이스 테스트 | 24개 |
| DTO (SearchResponse, AggregationResponse, IndexStatus) | 변환 테스트 | 7개 |
| Frontend API 유틸리티 | Jest Mock 테스트 | 13개 |
| Frontend 컴포넌트 (SearchForm, ResultsTable, StatsCard) | React Testing Library | 20개 |

상세: [TEST_DOCUMENTATION.md](TEST_DOCUMENTATION.md)

## Documents

| 문서 | 설명 |
|------|------|
| [doc/API_SPECIFICATION.md](doc/API_SPECIFICATION.md) | REST API 명세서 (14개 엔드포인트, 요청/응답 예시) |
| [TEST_DOCUMENTATION.md](TEST_DOCUMENTATION.md) | 테스트 설계 전략, 71개 케이스 상세, 실행 방법 |
| [doc/KUBERNETES_GUIDE.md](doc/KUBERNETES_GUIDE.md) | K8s 이론, 핵심 오브젝트, 실습 예제, 운영 패턴 |

## Kubernetes (Kind) 로컬 배포

Kind 기반 K8s 로컬 환경에서 전체 서비스를 실행할 수 있습니다.

### 원클릭 배포

```bash
./k8s/deploy.sh
```

### 수동 배포

```bash
# 1. 클러스터 생성
kind create cluster --config k8s/kind-config.yaml

# 2. 이미지 빌드 & 로드
docker build -t traffic-light-backend:latest backend
docker build --build-arg NEXT_PUBLIC_API_URL=http://localhost:30080/api -t traffic-light-frontend:latest frontend
kind load docker-image traffic-light-backend:latest traffic-light-frontend:latest --name traffic-light

# 3. 배포
kubectl apply -f k8s/namespace.yaml
kubectl apply -f k8s/elasticsearch.yaml
kubectl apply -f k8s/backend.yaml
kubectl apply -f k8s/frontend.yaml
kubectl apply -f k8s/indexing-job.yaml
```

### 접속 정보

| 서비스 | URL |
|--------|-----|
| Frontend | http://localhost:30000 |
| Backend API | http://localhost:30080/api |
| Elasticsearch | http://localhost:30920 |

### 주요 명령어

```bash
kubectl -n traffic-light get pods          # 상태 확인
kubectl -n traffic-light logs -f deploy/backend   # 로그
kubectl -n traffic-light scale deploy/backend --replicas=3  # 스케일링
kind delete cluster --name traffic-light   # 삭제
```

상세: [doc/KUBERNETES_GUIDE.md](doc/KUBERNETES_GUIDE.md)
