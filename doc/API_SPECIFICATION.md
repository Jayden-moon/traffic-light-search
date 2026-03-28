# Traffic Light Search API 명세서

**Version**: 1.0.0
**Base URL**: `http://localhost:8080/api`
**인증**: 관리 엔드포인트는 `X-Admin-Api-Key` 헤더 필요

---

## 목차

1. [인증](#1-인증)
2. [검색 API](#2-검색-api)
3. [집계 API](#3-집계-api)
4. [인덱스 관리 API](#4-인덱스-관리-api)
5. [데이터 동기화 API](#5-데이터-동기화-api)
6. [공통 모델](#6-공통-모델)
7. [에러 응답](#7-에러-응답)

---

## 1. 인증

### 인증 방식

관리 엔드포인트(인덱스 생성/삭제/로드, 데이터 동기화)는 API Key 인증이 필요합니다.

| 헤더 | 값 |
|------|-----|
| `X-Admin-Api-Key` | 서버 설정의 `ADMIN_API_KEY` 환경변수 값 |

### 엔드포인트별 인증 요구사항

| 엔드포인트 | 인증 | 비고 |
|-----------|------|------|
| `GET /api/search/**` | 불필요 | 공개 |
| `GET /api/aggregations/**` | 불필요 | 공개 |
| `GET /api/index/status` | 불필요 | 공개 |
| `POST /api/index/create` | **필요** | ADMIN 역할 |
| `POST /api/index/load` | **필요** | ADMIN 역할 |
| `DELETE /api/index/delete` | **필요** | ADMIN 역할 |
| `POST /api/sync` | **필요** | ADMIN 역할 |

### Rate Limiting

| 대상 | 제한 | 초과 시 |
|------|------|---------|
| 공개 엔드포인트 | IP당 60회/분 | `429 Too Many Requests` |
| 관리 엔드포인트 | IP당 10회/분 | `429 Too Many Requests` |

응답 헤더:
```
X-RateLimit-Limit: 60
X-RateLimit-Remaining: 58
```

---

## 2. 검색 API

### 2.1 전문 검색

신호등 데이터를 검색어와 필터 조건으로 검색합니다.

```
GET /api/search
```

#### 요청 파라미터

| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|---------|------|------|--------|------|
| `q` | String | - | - | 검색어 (도로명, 주소, 관리기관 등) |
| `sidoName` | String | - | - | 시도명 필터 (예: "서울특별시") |
| `sigunguName` | String | - | - | 시군구명 필터 (예: "강남구") |
| `roadType` | String | - | - | 도로종류 필터 (예: "일반국도") |
| `trafficLightCategory` | String | - | - | 신호등구분 필터 (예: "차량신호등") |
| `page` | int | - | `0` | 페이지 번호 (0-based) |
| `size` | int | - | `20` | 페이지 크기 (최소 1, 최대 100) |

#### 요청 예시

```bash
# 기본 검색
curl "http://localhost:8080/api/search?q=강남"

# 필터 + 페이지네이션
curl "http://localhost:8080/api/search?sidoName=서울특별시&roadType=일반국도&page=0&size=10"

# 검색어 + 필터 조합
curl "http://localhost:8080/api/search?q=테헤란로&sidoName=서울특별시&sigunguName=강남구"
```

#### 응답

```json
{
  "total": 1523,
  "page": 0,
  "size": 20,
  "results": [
    {
      "sidoName": "서울특별시",
      "sigunguName": "강남구",
      "roadType": "일반국도",
      "roadRouteNumber": "1",
      "roadRouteName": "경부선",
      "roadRouteDirection": "상행",
      "roadNameAddress": "테헤란로 123",
      "lotNumberAddress": "역삼동 123-4",
      "latitude": 37.5665,
      "longitude": 126.978,
      "signalInstallType": "기둥식",
      "roadShape": "교차로",
      "isMainRoad": "Y",
      "trafficLightId": "TL-001",
      "trafficLightCategory": "차량신호등",
      "lightColorType": "3색",
      "signalMethod": "점등",
      "signalSequence": "녹-황-적",
      "signalDuration": "60초",
      "lightSourceType": "LED",
      "signalControlMethod": "자동",
      "signalTimeDecisionMethod": "정주기",
      "flashingLightEnabled": "Y",
      "flashingStartTime": "23:00",
      "flashingEndTime": "06:00",
      "hasPedestrianSignal": "Y",
      "hasRemainingTimeDisplay": "Y",
      "hasAudioSignal": "N",
      "roadSignSerialNumber": "RS-001",
      "managementAgency": "서울시청",
      "managementPhone": "02-1234-5678",
      "dataReferenceDate": "2024-01-01",
      "providerCode": "3000000",
      "providerName": "서울특별시"
    }
  ]
}
```

| 응답 필드 | 타입 | 설명 |
|----------|------|------|
| `total` | long | 전체 검색 결과 수 |
| `page` | int | 현재 페이지 번호 (0-based) |
| `size` | int | 페이지 크기 |
| `results` | TrafficLight[] | 신호등 데이터 배열 ([TrafficLight 모델](#61-trafficlight) 참조) |

---

### 2.2 위치 기반 검색

GPS 좌표와 반경으로 주변 신호등을 검색합니다.

```
GET /api/search/geo
```

#### 요청 파라미터

| 파라미터 | 타입 | 필수 | 기본값 | 설명 | 제약 |
|---------|------|------|--------|------|------|
| `lat` | double | **필수** | - | 위도 | -90 ~ 90 |
| `lon` | double | **필수** | - | 경도 | -180 ~ 180 |
| `distance` | String | - | `1km` | 검색 반경 | `{숫자}km` 또는 `{숫자}m` 형식 |
| `page` | int | - | `0` | 페이지 번호 | 0 이상 |
| `size` | int | - | `20` | 페이지 크기 | 1 ~ 100 |

#### 요청 예시

```bash
# 서울 시청 반경 1km
curl "http://localhost:8080/api/search/geo?lat=37.5665&lon=126.978&distance=1km"

# 부산역 반경 500m, 페이지 크기 50
curl "http://localhost:8080/api/search/geo?lat=35.1146&lon=129.0422&distance=500m&size=50"
```

#### 응답

`GET /api/search`와 동일한 `SearchResponse` 구조. 결과는 좌표 기준 가까운 순서로 정렬됩니다.

---

### 2.3 필터 옵션 조회

검색 필터에서 사용할 수 있는 옵션 목록을 조회합니다.

```
GET /api/search/filters/{field}
```

#### 경로 파라미터

| 파라미터 | 타입 | 설명 |
|---------|------|------|
| `field` | String | 필터 필드명 |

**허용되는 필드명:**
- `sidoName` — 시도명
- `sigunguName` — 시군구명
- `roadType` — 도로종류
- `trafficLightCategory` — 신호등구분
- `lightColorType` — 신호등색종류
- `signalMethod` — 신호등화방식
- `roadShape` — 도로형태

#### 요청 파라미터

| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| `sidoName` | String | - | 부모 필터 (시군구 조회 시 시도명 지정) |

#### 요청 예시

```bash
# 시도 목록 조회
curl "http://localhost:8080/api/search/filters/sidoName"

# 서울특별시의 시군구 목록 조회
curl "http://localhost:8080/api/search/filters/sigunguName?sidoName=서울특별시"

# 도로종류 목록
curl "http://localhost:8080/api/search/filters/roadType"
```

#### 응답

```json
["서울특별시", "부산광역시", "대구광역시", "인천광역시", "광주광역시"]
```

| 타입 | 설명 |
|------|------|
| String[] | 해당 필드의 고유 값 목록 (최대 100개) |

---

## 3. 집계 API

### 3.1 지역별 집계

시도/시군구 기준으로 신호등 수를 집계합니다.

```
GET /api/aggregations/by-region
```

#### 응답

```json
{
  "buckets": [
    {
      "key": "서울특별시",
      "count": 15230,
      "subBuckets": [
        { "key": "강남구", "count": 2100, "subBuckets": null },
        { "key": "서초구", "count": 1850, "subBuckets": null },
        { "key": "송파구", "count": 1620, "subBuckets": null }
      ]
    },
    {
      "key": "부산광역시",
      "count": 8920,
      "subBuckets": [
        { "key": "해운대구", "count": 1200, "subBuckets": null }
      ]
    }
  ]
}
```

---

### 3.2 도로종류별 집계

```
GET /api/aggregations/by-road-type
```

#### 응답

```json
{
  "buckets": [
    { "key": "일반국도", "count": 12500, "subBuckets": null },
    { "key": "시도", "count": 9800, "subBuckets": null },
    { "key": "군도", "count": 3200, "subBuckets": null }
  ]
}
```

---

### 3.3 신호등구분별 집계

```
GET /api/aggregations/by-signal-type
```

#### 응답

```json
{
  "buckets": [
    { "key": "차량신호등", "count": 25000, "subBuckets": null },
    { "key": "보행자신호등", "count": 18000, "subBuckets": null }
  ]
}
```

---

### 3.4 요약 집계

여러 필드의 집계를 한 번에 조회합니다.

```
GET /api/aggregations/summary
```

#### 응답

```json
{
  "sidoName": {
    "buckets": [
      { "key": "서울특별시", "count": 15230, "subBuckets": null },
      { "key": "부산광역시", "count": 8920, "subBuckets": null }
    ]
  },
  "roadType": {
    "buckets": [
      { "key": "일반국도", "count": 12500, "subBuckets": null }
    ]
  },
  "trafficLightCategory": {
    "buckets": [
      { "key": "차량신호등", "count": 25000, "subBuckets": null }
    ]
  },
  "lightColorType": {
    "buckets": [
      { "key": "3색", "count": 30000, "subBuckets": null }
    ]
  }
}
```

| 필드 | 타입 | 설명 |
|------|------|------|
| `{fieldName}` | AggregationResponse | 필드별 집계 결과 |
| `buckets[].key` | String | 집계 키 (필드 값) |
| `buckets[].count` | long | 문서 수 |
| `buckets[].subBuckets` | BucketResponse[] \| null | 하위 집계 (by-region에서만 사용) |

---

## 4. 인덱스 관리 API

> 모든 엔드포인트에 `X-Admin-Api-Key` 헤더 필요 (GET /status 제외)

### 4.1 인덱스 생성

Elasticsearch에 신호등 인덱스를 생성합니다. 이미 존재하면 무시합니다.

```
POST /api/index/create
```

#### 요청

```bash
curl -X POST http://localhost:8080/api/index/create \
  -H "X-Admin-Api-Key: your-api-key"
```

#### 응답

```
200 OK
"Index created"
```

---

### 4.2 데이터 로드

로컬 JSON 파일에서 데이터를 읽어 Elasticsearch에 벌크 인덱싱합니다.

```
POST /api/index/load
```

#### 요청

```bash
curl -X POST http://localhost:8080/api/index/load \
  -H "X-Admin-Api-Key: your-api-key"
```

#### 응답

```json
{
  "totalRecords": 52341,
  "indexed": 52100,
  "failed": 241,
  "durationMs": 15230,
  "indexExists": true
}
```

| 필드 | 타입 | 설명 |
|------|------|------|
| `totalRecords` | long | 파일에서 읽은 총 레코드 수 |
| `indexed` | long | 인덱싱 성공 문서 수 |
| `failed` | long | 인덱싱 실패 문서 수 |
| `durationMs` | long | 처리 소요 시간 (밀리초) |
| `indexExists` | boolean | 인덱스 존재 여부 |

---

### 4.3 인덱스 삭제

Elasticsearch 인덱스를 삭제합니다. 존재하지 않으면 무시합니다.

```
DELETE /api/index/delete
```

#### 요청

```bash
curl -X DELETE http://localhost:8080/api/index/delete \
  -H "X-Admin-Api-Key: your-api-key"
```

#### 응답

```
200 OK
"Index deleted"
```

---

### 4.4 인덱스 상태 조회

현재 인덱스의 존재 여부와 문서 수를 조회합니다. **인증 불필요.**

```
GET /api/index/status
```

#### 요청

```bash
curl http://localhost:8080/api/index/status
```

#### 응답

```json
{
  "totalRecords": 52100,
  "indexed": 52100,
  "failed": 0,
  "durationMs": 0,
  "indexExists": true
}
```

---

## 5. 데이터 동기화 API

> `X-Admin-Api-Key` 헤더 필요

### 5.1 공공데이터 API 동기화

공공데이터포털 API에서 최신 신호등 데이터를 가져와 Elasticsearch에 동기화합니다.

- 인덱스가 없으면 자동 생성
- Upsert 방식 — 중복 없이 여러 번 호출 가능
- 1000건 단위 배치 처리

```
POST /api/sync
```

#### 요청

```bash
curl -X POST http://localhost:8080/api/sync \
  -H "X-Admin-Api-Key: your-api-key"
```

#### 응답

```json
{
  "totalRecords": 54000,
  "indexed": 53800,
  "failed": 200,
  "durationMs": 125000,
  "indexExists": true
}
```

---

## 6. 공통 모델

### 6.1 TrafficLight

신호등 데이터 모델입니다. 검색 결과의 `results` 배열에 포함됩니다.

| 필드 | 타입 | 설명 | 예시 |
|------|------|------|------|
| `sidoName` | String | 시도명 | "서울특별시" |
| `sigunguName` | String | 시군구명 | "강남구" |
| `roadType` | String | 도로종류 | "일반국도" |
| `roadRouteNumber` | String | 도로노선번호 | "1" |
| `roadRouteName` | String | 도로노선명 | "경부선" |
| `roadRouteDirection` | String | 도로노선방향 | "상행" |
| `roadNameAddress` | String | 소재지도로명주소 | "테헤란로 123" |
| `lotNumberAddress` | String | 소재지지번주소 | "역삼동 123-4" |
| `latitude` | double | 위도 | 37.5665 |
| `longitude` | double | 경도 | 126.978 |
| `signalInstallType` | String | 신호기설치방식 | "기둥식" |
| `roadShape` | String | 도로형태 | "교차로" |
| `isMainRoad` | String | 주도로여부 | "Y" |
| `trafficLightId` | String | 신호등관리번호 | "TL-001" |
| `trafficLightCategory` | String | 신호등구분 | "차량신호등" |
| `lightColorType` | String | 신호등색종류 | "3색" |
| `signalMethod` | String | 신호등화방식 | "점등" |
| `signalSequence` | String | 신호등화순서 | "녹-황-적" |
| `signalDuration` | String | 신호등화시간 | "60초" |
| `lightSourceType` | String | 광원종류 | "LED" |
| `signalControlMethod` | String | 신호제어방식 | "자동" |
| `signalTimeDecisionMethod` | String | 신호시간결정방식 | "정주기" |
| `flashingLightEnabled` | String | 점멸등운영여부 | "Y" |
| `flashingStartTime` | String | 점멸등운영시작시각 | "23:00" |
| `flashingEndTime` | String | 점멸등운영종료시각 | "06:00" |
| `hasPedestrianSignal` | String | 보행자작동신호기유무 | "Y" |
| `hasRemainingTimeDisplay` | String | 잔여시간표시기유무 | "Y" |
| `hasAudioSignal` | String | 시각장애인용음향신호기유무 | "N" |
| `roadSignSerialNumber` | String | 도로안내표지일련번호 | "RS-001" |
| `managementAgency` | String | 관리기관명 | "서울시청" |
| `managementPhone` | String | 관리기관전화번호 | "02-1234-5678" |
| `dataReferenceDate` | String | 데이터기준일자 | "2024-01-01" |
| `providerCode` | String | 제공기관코드 | "3000000" |
| `providerName` | String | 제공기관명 | "서울특별시" |

### 6.2 SearchResponse

```json
{
  "total": "long — 전체 결과 수",
  "page": "int — 현재 페이지 (0-based)",
  "size": "int — 페이지 크기",
  "results": "TrafficLight[] — 결과 배열"
}
```

### 6.3 IndexStatusResponse

```json
{
  "totalRecords": "long — 총 레코드 수",
  "indexed": "long — 인덱싱 성공 수",
  "failed": "long — 인덱싱 실패 수",
  "durationMs": "long — 처리 시간 (ms)",
  "indexExists": "boolean — 인덱스 존재 여부"
}
```

### 6.4 AggregationResponse

```json
{
  "buckets": [
    {
      "key": "String — 집계 키",
      "count": "long — 문서 수",
      "subBuckets": "BucketResponse[] | null — 하위 집계"
    }
  ]
}
```

---

## 7. 에러 응답

모든 에러는 통일된 JSON 형식으로 반환됩니다. 내부 스택트레이스는 노출되지 않습니다.

### 에러 응답 형식

```json
{
  "error": "사용자에게 안전한 에러 메시지"
}
```

### HTTP 상태 코드

| 코드 | 상황 | 응답 예시 |
|------|------|----------|
| `200` | 성공 | 정상 응답 |
| `400` | 잘못된 요청 | `{"error": "입력값이 유효하지 않습니다."}` |
| `400` | 필수 파라미터 누락 | `{"error": "필수 파라미터가 누락되었습니다: lat"}` |
| `400` | 파라미터 형식 오류 | `{"error": "파라미터 형식이 올바르지 않습니다: lat"}` |
| `400` | 허용되지 않은 필터 | `{"error": "허용되지 않은 필터 필드입니다: unknown"}` |
| `403` | 인증 실패 | (빈 응답 또는 `Forbidden`) |
| `404` | 인덱스 없음 | `{"error": "요청한 인덱스를 찾을 수 없습니다."}` |
| `429` | 요청 초과 | `{"error": "요청이 너무 많습니다. 잠시 후 다시 시도해주세요."}` |
| `500` | 서버 내부 오류 | `{"error": "서버 오류가 발생했습니다."}` |

---

## 부록: 빠른 시작 가이드

### 1. Elasticsearch 실행

```bash
docker-compose up -d
```

### 2. 백엔드 실행

```bash
cd backend
export ADMIN_API_KEY=my-secure-key-here
export SERVICE_KEY=공공데이터포털-서비스키
./gradlew bootRun
```

### 3. 인덱스 생성 + 데이터 로드

```bash
# 인덱스 생성
curl -X POST http://localhost:8080/api/index/create \
  -H "X-Admin-Api-Key: my-secure-key-here"

# 로컬 JSON 데이터 로드
curl -X POST http://localhost:8080/api/index/load \
  -H "X-Admin-Api-Key: my-secure-key-here"

# 또는 공공데이터 API에서 직접 동기화
curl -X POST http://localhost:8080/api/sync \
  -H "X-Admin-Api-Key: my-secure-key-here"
```

### 4. 검색

```bash
# 전문 검색
curl "http://localhost:8080/api/search?q=강남"

# 위치 기반 검색
curl "http://localhost:8080/api/search/geo?lat=37.5665&lon=126.978&distance=1km"

# 집계
curl "http://localhost:8080/api/aggregations/summary"
```
