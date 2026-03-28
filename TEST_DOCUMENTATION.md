# Traffic Light Search - 테스트 문서

## 1. 개요

본 문서는 `traffic-light-search` 프로젝트의 테스트 코드 구조, 작성 전략, 실행 방법 및 각 테스트의 설계 의도를 설명한다.

### 1.1 테스트 현황 요약

| 구분 | 테스트 파일 | 테스트 케이스 | 결과 |
|------|------------|-------------|------|
| Backend - Domain | 2개 | 19개 | ALL PASS |
| Backend - UseCase | 4개 | 27개 | ALL PASS |
| Backend - API Controller | 4개 | 17개 | ALL PASS |
| Backend - API DTO | 3개 | 7개 | ALL PASS |
| Frontend - API 유틸리티 | 1개 | 13개 | ALL PASS |
| Frontend - 컴포넌트 | 3개 | 20개 | ALL PASS |
| **합계** | **17개 파일** | **103개 케이스** | **ALL PASS** |

### 1.2 테스트 실행 방법

```bash
# 백엔드 전체 테스트
cd backend
./gradlew test

# 백엔드 모듈별 테스트
./gradlew :trafficlight-domain:test
./gradlew :trafficlight-usecase:test
./gradlew :trafficlight-api:test

# 프론트엔드 전체 테스트
cd frontend
npm test

# 프론트엔드 watch 모드
npm run test:watch

# 프론트엔드 커버리지
npm run test:coverage
```

---

## 2. 테스트 전략

### 2.1 테스트 피라미드

본 프로젝트는 클린 아키텍처의 레이어별 특성에 맞춘 테스트 전략을 적용한다.

```
        ┌──────────────┐
        │  Controller   │  ← MockMvc 슬라이스 테스트 (HTTP 계층)
        │  (API Layer)  │
        ├──────────────┤
        │   UseCase     │  ← Mockito 단위 테스트 (비즈니스 오케스트레이션)
        │ (Application) │
        ├──────────────┤
        │   Domain      │  ← 순수 단위 테스트 (외부 의존성 없음)
        │   Model       │
        └──────────────┘

        ┌──────────────┐
        │  Component    │  ← React Testing Library (사용자 관점)
        │  (Frontend)   │
        ├──────────────┤
        │  API Utility  │  ← Jest Mock (HTTP 클라이언트)
        └──────────────┘
```

### 2.2 레이어별 테스트 원칙

| 레이어 | 원칙 | Mock 대상 | 검증 대상 |
|--------|------|-----------|-----------|
| Domain | 프레임워크 의존성 없이 순수 Java로 테스트 | 없음 | 모델 로직, 유효성 검사, 예외 계층 |
| UseCase | 도메인 서비스 인터페이스를 Mock | SearchService, IndexService 등 | 필터 변환, 결과 조합, 흐름 제어 |
| Controller | UseCase를 Mock, HTTP 계층만 테스트 | SearchUseCase, IndexUseCase 등 | HTTP 상태코드, JSON 응답 구조, 파라미터 바인딩 |
| DTO | 변환 팩토리 메서드 단위 테스트 | 없음 | UseCase ↔ API 간 DTO 매핑 정확성 |
| Frontend API | axios 클라이언트를 Mock | axios.create() 인스턴스 | 엔드포인트 경로, 파라미터 구성, 응답 파싱 |
| Frontend Component | API 호출 없이 렌더링 테스트 | 없음 (props 기반) | UI 렌더링, 사용자 인터랙션, 상태 변화 |

---

## 3. 백엔드 테스트 상세

### 3.1 Domain Layer 테스트

#### `TrafficLightTest.java`

**위치**: `trafficlight-domain/src/test/java/com/trafficlight/domain/model/`

도메인 모델의 핵심 로직을 검증한다. 외부 의존성이 전혀 없는 순수 단위 테스트이다.

**테스트 그룹 및 케이스:**

| 그룹 | 테스트 | 검증 내용 |
|------|--------|-----------|
| `hasValidLocation` | `validKoreaCoordinates` | 서울 좌표(37.5665, 126.978)가 유효 판정 |
| | `latitudeLowerBoundary` | 위도 하한 경계값 33.0 → true |
| | `latitudeUpperBoundary` | 위도 상한 경계값 43.0 → true |
| | `longitudeLowerBoundary` | 경도 하한 경계값 124.0 → true |
| | `longitudeUpperBoundary` | 경도 상한 경계값 132.0 → true |
| | `latitudeBelowRange` | 위도 32.9 → false |
| | `latitudeAboveRange` | 위도 43.1 → false |
| | `longitudeBelowRange` | 경도 123.9 → false |
| | `longitudeAboveRange` | 경도 132.1 → false |
| | `zeroCoordinates` | 좌표 (0, 0) → false |
| `fromMap - 영문키` | `mapsBasicFieldsFromEnglishKeys` | 영문 키 기반 필드 매핑 정확성 |
| | `parsesLatLonFromString` | 문자열 위경도 → double 파싱 |
| | `emptyLatLonRemainsZero` | 빈 문자열 → 0.0 유지 |
| | `invalidLatLonIgnored` | "invalid" 문자열 → 파싱 실패 시 0.0 |
| `fromMap - 한글키` | `mapsFieldsFromKoreanKeys` | "시도명", "위도" 등 한글 키 매핑 |
| | `englishKeyTakesPrecedence` | 영문/한글 키 동시 존재 시 영문 우선 |
| `fromMap - location` | `parsesLocationObject` | `{lat, lon}` Map에서 좌표 추출 |
| | `locationObjectOverridesIndividualFields` | location 객체가 개별 필드보다 우선 |
| | `nonMapLocationIgnored` | location이 String이면 무시 |
| `fromMap - 엣지케이스` | `emptyMapReturnsDefaults` | 빈 Map → 모든 필드 null/0.0 |
| | `nullValuesMapToNull` | null 값 → null 유지 |
| | `allFieldsMapped` | 전체 33개 필드 매핑 정합성 확인 |

**설계 의도:**
- `hasValidLocation()`은 대한민국 좌표 범위(위도 33~43, 경도 124~132)를 검증하는 도메인 규칙이다. 경계값 분석(Boundary Value Analysis) 기법을 적용하여 경계, 경계-1, 경계+1 값을 모두 테스트했다.
- `fromMap()`은 외부 데이터(JSON/API)를 도메인 객체로 변환하는 팩토리 메서드이다. 영문 키, 한글 키, location 객체, null/빈값 등 실제 데이터에서 발생할 수 있는 모든 입력 패턴을 커버한다.

#### `ExceptionTest.java`

**위치**: `trafficlight-domain/src/test/java/com/trafficlight/domain/core/exception/`

| 테스트 | 검증 내용 |
|--------|-----------|
| `domainExceptionMessage` | 메시지 설정 및 RuntimeException 상속 |
| `domainExceptionWithCause` | 원인 예외 체이닝 |
| `indexNotFoundExceptionIncludesIndexName` | 인덱스명 포함 메시지 생성 |
| `dataLoadExceptionMessage` | 메시지 설정 |
| `dataLoadExceptionWithCause` | IOException 원인 체이닝 |

**설계 의도:**
- 도메인 예외 계층의 상속 구조(`RuntimeException` → `TrafficLightDomainException` → `IndexNotFoundException`/`DataLoadException`)를 검증한다.
- 예외 메시지와 cause가 올바르게 전파되는지 확인한다.

---

### 3.2 UseCase Layer 테스트

UseCase 레이어는 도메인 서비스 인터페이스를 Mockito로 Mock하여, 비즈니스 오케스트레이션 로직만 격리 테스트한다. UseCase가 POJO(Spring 어노테이션 없음)이므로 Spring Context 없이 빠르게 실행된다.

#### `SearchUseCaseTest.java`

**위치**: `trafficlight-usecase/src/test/java/com/trafficlight/usecase/`

| 그룹 | 테스트 | 검증 내용 |
|------|--------|-----------|
| `search` | `searchWithQueryAndFilters` | 검색어+필터 → SearchService에 올바르게 위임, 결과 조합 |
| | `searchWithoutFilters` | 필터 없음 → 빈 Map 전달 |
| | `searchWithSidoNameFilter` | sidoName 단일 필터 변환 검증 |
| | `searchWithAllFilters` | 4개 필터 모두 전달 검증 |
| | `emptyStringFiltersIgnored` | 빈 문자열("") 필터 → Map에서 제외 |
| | `paginationPassedCorrectly` | page/size 값 전달 및 결과 반영 |
| `geoSearch` | `geoSearchReturnsResults` | 위치 기반 검색 위임 및 결과 조합 |
| | `distanceParameterPassedCorrectly` | distance 파라미터 전달 검증 |
| `getFilterOptions` | `returnsFilterOptions` | 필터 옵션 목록 반환 |
| | `returnsFilterOptionsWithParentFilter` | 부모 필터 포함 하위 옵션 조회 |

**핵심 검증 포인트:**
- `SearchCommand`의 필터 값(sidoName, sigunguName, roadType, trafficLightCategory)이 `Map<String, String>`으로 올바르게 변환되는가?
- null/빈 문자열 필터가 Map에서 제외되는가?
- `search()`와 `count()` 두 메서드를 모두 호출하여 total과 results를 조합하는가?

#### `IndexUseCaseTest.java`

| 그룹 | 테스트 | 검증 내용 |
|------|--------|-----------|
| `createIndex` | `delegatesToIndexService` | IndexService.createIndex() 위임 확인 |
| `loadData` | `returnsStatusAfterLoad` | 로드 후 indexed=995, failed=5 계산 |
| | `failedCountCalculatedCorrectly` | 실패 건수 = loadData 반환값 - documentCount |
| `deleteIndex` | `delegatesToIndexService` | IndexService.deleteIndex() 위임 확인 |
| `getStatus` | `returnsDocCountWhenIndexExists` | 인덱스 존재 시 문서 수 조회 |
| | `returnsZeroWhenIndexNotExists` | 인덱스 없음 → documentCount 호출하지 않음 |

**핵심 검증 포인트:**
- `loadData()`의 failed 계산 로직: `failed = loadData() - documentCount()`
- `getStatus()`에서 인덱스가 없을 때 `documentCount()`를 호출하지 않는 최적화 검증 (`verify(never())`)

#### `AggregationUseCaseTest.java`

| 그룹 | 테스트 | 검증 내용 |
|------|--------|-----------|
| `aggregateByRegion` | `convertsRegionAggregation` | Map → AggregationResult 변환 |
| | `convertsSubBuckets` | 중첩 버킷 재귀 변환 |
| | `handlesEmptyBuckets` | 빈 buckets 리스트 처리 |
| | `handlesNoBucketsKey` | buckets 키 없음 → 빈 리스트 |
| `aggregateByRoadType` | `convertsRoadTypeAggregation` | 도로종류 집계 변환 |
| `aggregateBySignalType` | `convertsSignalTypeAggregation` | 신호등구분 집계 변환 |
| `getSummary` | `returnsSummaryOfMultipleAggregations` | 다중 집계 요약 |
| | `ignoresNonMapValues` | Map이 아닌 값 무시 |
| | `nonNumberCountTreatedAsZero` | count가 숫자가 아니면 0 처리 |

**핵심 검증 포인트:**
- `Map<String, Object>` → `AggregationResult` 변환 로직의 방어적 프로그래밍 검증
- `@SuppressWarnings("unchecked")` 캐스팅이 안전하게 동작하는지 다양한 입력으로 확인
- subBuckets 재귀 변환의 정확성

#### `DataSyncUseCaseTest.java`

| 그룹 | 테스트 | 검증 내용 |
|------|--------|-----------|
| `syncFromApi` | `createsIndexIfNotExists` | 인덱스 없으면 createIndex 호출 |
| | `skipsIndexCreationIfExists` | 인덱스 있으면 createIndex 호출 안 함 |
| | `returnsImmediatelyWhenZeroCount` | API 0건 → fetchPage 호출 안 함 |
| | `processesMultiplePages` | 3페이지 순차 처리, indexed/failed 집계 |
| | `stopsOnEmptyPage` | 빈 페이지 반환 시 루프 중단 |
| | `failedCountAccumulatedCorrectly` | 5건 전송 중 3건 성공 → failed=2 |

**핵심 검증 포인트:**
- 페이징 동기화 흐름: `getTotalCount()` → 페이지 계산 → `fetchPage()` 루프 → `bulkUpsert()`
- 조기 종료 조건: totalCount=0, 빈 페이지 반환
- 인덱스 자동 생성 로직

---

### 3.3 API Layer 테스트

Controller 테스트는 `@WebMvcTest`를 사용하여 Spring MVC 슬라이스만 로드한다. UseCase는 `@MockBean`으로 Mock하여 HTTP 요청/응답 계층만 검증한다.

#### `SearchControllerTest.java`

| 그룹 | 테스트 | HTTP | 검증 내용 |
|------|--------|------|-----------|
| `GET /api/search` | `searchWithQuery` | `?q=강남` | 200 + JSON 응답 구조 |
| | `searchWithoutParams` | 파라미터 없음 | 200 + 빈 결과 |
| | `searchWithFiltersAndPagination` | 전체 필터+page+size | 파라미터 바인딩 |
| `GET /api/search/geo` | `geoSearch` | `?lat=37&lon=126&distance=1km` | 200 + 좌표 포함 응답 |
| | `geoSearchMissingLat` | lon만 전달 | 400 Bad Request |
| | `geoSearchMissingLon` | lat만 전달 | 400 Bad Request |
| `GET /api/search/filters/{field}` | `getFilterOptions` | `/sidoName` | 200 + 문자열 배열 |
| | `getFilterOptionsWithParent` | `?sidoName=서울특별시` | 부모 필터 전달 |

**핵심 검증 포인트:**
- `@RequestParam(required = false)` vs `@RequestParam` (필수) 동작 차이
- geo 검색에서 lat/lon 필수 파라미터 누락 시 400 반환
- 한글 파라미터의 올바른 인코딩/디코딩

#### `IndexControllerTest.java`

| 테스트 | HTTP Method | 검증 내용 |
|--------|-------------|-----------|
| `createIndexSuccess` | POST `/create` | 200 + "Index created" 문자열 |
| `loadDataReturnsStatus` | POST `/load` | 200 + IndexStatusResponse JSON |
| `deleteIndexSuccess` | DELETE `/delete` | 200 + "Index deleted" 문자열 |
| `getStatusReturnsIndexInfo` | GET `/status` | 200 + indexExists=true |
| `getStatusWhenNoIndex` | GET `/status` | 200 + indexExists=false |

#### `AggregationControllerTest.java`

| 테스트 | 엔드포인트 | 검증 내용 |
|--------|-----------|-----------|
| `returnsRegionAggregation` | `/by-region` | 중첩 subBuckets 포함 JSON |
| `returnsRoadTypeAggregation` | `/by-road-type` | buckets 배열 크기 |
| `returnsSignalTypeAggregation` | `/by-signal-type` | bucket key 값 |
| `returnsSummary` | `/summary` | 다중 키(sidoName, roadType) JSON 구조 |

#### `DataSyncControllerTest.java`

| 테스트 | HTTP | 검증 내용 |
|--------|------|-----------|
| `syncFromApiReturnsResult` | POST `/api/sync` | 200 + 전체 필드(totalRecords, indexed, failed, durationMs, indexExists) |

### 3.4 DTO 변환 테스트

DTO 팩토리 메서드(`from()`)의 매핑 정확성을 검증한다.

#### `SearchResponseTest.java`
- UseCase의 `SearchResult` → API의 `SearchResponse` 변환
- 빈 결과 변환 처리

#### `AggregationResponseTest.java`
- `AggregationResult` → `AggregationResponse` 변환
- subBuckets 재귀 변환 정확성
- null subBuckets → null 유지

#### `IndexStatusResponseTest.java`
- `IndexStatusResult` → `IndexStatusResponse` 변환
- boolean 필드(`indexExists`) 매핑

---

## 4. 프론트엔드 테스트 상세

### 4.1 API 유틸리티 테스트

#### `api.test.ts`

**위치**: `frontend/src/__tests__/lib/`

axios 인스턴스를 Jest Mock으로 대체하여 HTTP 요청 없이 API 함수의 동작을 검증한다.

| 그룹 | 테스트 | 검증 내용 |
|------|--------|-----------|
| `searchTrafficLights` | 검색어와 파라미터를 올바르게 전달한다 | `/search` + `{q, sidoName, page, size}` |
| | 검색어가 없으면 q 파라미터를 포함하지 않는다 | `query: undefined` → params에 `q` 없음 |
| | 빈 검색어는 q 파라미터를 포함하지 않는다 | `query: ""` → falsy 처리 |
| `geoSearch` | 위치 기반 검색 파라미터를 올바르게 전달한다 | `/search/geo` + `{lat, lon, distance}` |
| | 기본값이 올바르게 적용된다 | `page=0, size=100` 기본값 |
| `getFilterOptions` | 필터 옵션을 요청한다 | `/search/filters/sidoName` |
| | 부모 필터와 함께 요청한다 | `params: { sidoName: '서울특별시' }` |
| `getAggregations` | 집계 타입별로 요청한다 | `/aggregations/by-region` |
| `getAggregationSummary` | 요약 집계를 요청한다 | `/aggregations/summary` |
| 인덱스 관리 | createIndex → POST `/index/create` | HTTP 메서드 검증 |
| | loadData → POST `/index/load` | 응답 파싱 검증 |
| | syncFromApi → POST `/sync` | 응답 필드 검증 |
| | getIndexStatus → GET `/index/status` | boolean 필드 검증 |

**Mock 전략:**
```typescript
jest.mock('axios', () => ({
  default: { create: jest.fn(() => mockAxiosInstance) },
}));
```
`axios.create()`가 반환하는 인스턴스를 Mock하여, `api.ts` 내부 `client.get()` / `client.post()` 호출을 가로챈다.

---

### 4.2 컴포넌트 테스트

React Testing Library를 사용하여 **사용자 관점**에서 테스트한다. 구현 세부사항(state, ref 등)이 아닌 화면에 보이는 텍스트와 사용자 인터랙션을 검증한다.

#### `SearchForm.test.tsx`

**위치**: `frontend/src/__tests__/components/search/`

| 테스트 | 검증 내용 |
|--------|-----------|
| 검색 입력창이 렌더링된다 | placeholder 텍스트 존재 확인 |
| 검색 버튼이 렌더링된다 | role="button" + name="검색" |
| 폼 제출 시 onSearch가 검색어와 함께 호출된다 | userEvent.type → submit → callback 인자 |
| 빈 검색어로도 제출 가능하다 | 빈 문자열 전달 확인 |
| 검색 버튼 클릭으로 폼이 제출된다 | click 이벤트 → onSearch 호출 |

**테스트 접근법:**
- `userEvent.type()`: 실제 사용자 타이핑을 시뮬레이션 (키보드 이벤트 발생)
- `fireEvent.submit()`: 폼 제출 이벤트
- `screen.getByPlaceholderText()`: 접근성 기반 엘리먼트 조회

#### `ResultsTable.test.tsx`

**위치**: `frontend/src/__tests__/components/search/`

| 테스트 | 검증 내용 |
|--------|-----------|
| 로딩 중일 때 스피너를 표시한다 | `loading=true` → "검색 중..." 텍스트 |
| 결과가 null이면 안내 메시지를 표시한다 | `results=null` → 초기 안내 메시지 |
| 결과가 0건이면 없음 메시지를 표시한다 | `results=[]` → "검색 결과가 없습니다." |
| 결과가 있으면 테이블을 렌더링한다 | 7개 컬럼 데이터 표시 검증 |
| 총 건수를 표시한다 | `total=12345` → "12,345" (천단위 구분자) |
| 이전 버튼은 첫 페이지에서 비활성화된다 | `page=0` → 이전 disabled, 다음 enabled |
| 다음 버튼 클릭 시 onPageChange가 호출된다 | click → `onPageChange(1)` |
| 마지막 페이지에서 다음 버튼이 비활성화된다 | `total=20, size=20` → 다음 disabled |
| 페이지 정보를 올바르게 표시한다 | `page=2, total=100, size=20` → "페이지 3 / 5" |

**핵심 검증 포인트:**
- 3가지 빈 상태(loading, null, empty) 각각의 UI 분기
- 페이지네이션 버튼 활성화/비활성화 로직
- 0-based page → 1-based 표시 변환

#### `StatsCard.test.tsx`

**위치**: `frontend/src/__tests__/components/dashboard/`

| 테스트 | 검증 내용 |
|--------|-----------|
| 제목과 숫자 값을 렌더링한다 | title + `toLocaleString()` 포맷 |
| 문자열 값을 렌더링한다 | 문자열 그대로 표시 |
| 아이콘이 제공되면 표시한다 | icon prop → 텍스트 노드 |
| 아이콘이 없으면 아이콘 영역이 없다 | icon 미제공 → `.text-3xl` 없음 |
| 0 값을 올바르게 표시한다 | falsy 값 0 표시 확인 |
| 큰 숫자에 천 단위 구분자가 적용된다 | 1234567 → "1,234,567" |

---

## 5. 빌드 설정 변경 사항

### 5.1 Backend (`build.gradle`)

```groovy
// root build.gradle에 추가된 테스트 의존성
subprojects {
    dependencies {
        // 기존 lombok...

        testCompileOnly 'org.projectlombok:lombok:1.18.30'
        testAnnotationProcessor 'org.projectlombok:lombok:1.18.30'
        testImplementation 'org.junit.jupiter:junit-jupiter:5.10.2'
        testImplementation 'org.mockito:mockito-core:5.14.2'
        testImplementation 'org.mockito:mockito-junit-jupiter:5.14.2'
        testImplementation 'net.bytebuddy:byte-buddy:1.15.10'
        testImplementation 'org.assertj:assertj-core:3.25.3'
    }

    test {
        useJUnitPlatform()
        jvmArgs '-XX:+EnableDynamicAgentLoading'  // Java 23 호환
    }
}
```

**변경 이유:**
- **Mockito 5.14.2 + ByteBuddy 1.15.10**: Java 23 런타임과의 바이트코드 호환성 확보. 기존 Mockito 5.7.0은 Java 23의 `InlineBytecodeGenerator` 변경으로 `IllegalArgumentException` 발생
- **`-XX:+EnableDynamicAgentLoading`**: Java 21+ 에서 Mockito inline agent의 동적 로딩 경고 해소
- **AssertJ**: 가독성 높은 fluent assertion (`assertThat(x).isEqualTo(y)`)

### 5.2 Backend API Module (`trafficlight-api/build.gradle`)

```groovy
testImplementation 'org.springframework.boot:spring-boot-starter-test'
```

**포함 내용**: Spring MockMvc, `@WebMvcTest`, `@MockBean`, JUnit 5 Spring 확장

### 5.3 Frontend (`package.json`)

```json
{
  "scripts": {
    "test": "jest",
    "test:watch": "jest --watch",
    "test:coverage": "jest --coverage"
  },
  "devDependencies": {
    "jest": "^30.3.0",
    "jest-environment-jsdom": "^30.3.0",
    "ts-jest": "^29.4.6",
    "@testing-library/react": "^16.3.2",
    "@testing-library/jest-dom": "^6.9.1",
    "@testing-library/user-event": "^14.6.1",
    "@types/jest": "^30.0.0",
    "identity-obj-proxy": "^3.0.0",
    "axios-mock-adapter": "^2.1.0"
  }
}
```

### 5.4 Frontend Jest 설정 (`jest.config.ts`)

```typescript
{
  testEnvironment: 'jsdom',              // DOM API 시뮬레이션
  transform: {
    '^.+\\.tsx?$': ['ts-jest', {          // TypeScript → JS 변환
      tsconfig: { jsx: 'react-jsx' }      // JSX 변환 (React 17+ new JSX transform)
    }]
  },
  moduleNameMapper: {
    '^@/(.*)$': '<rootDir>/src/$1',       // @/ 경로 별칭 해석
    '\\.(css)$': 'identity-obj-proxy'     // CSS import → 빈 객체
  },
  setupFilesAfterEnv: ['./jest.setup.ts'] // toBeInTheDocument() 등 커스텀 matcher 등록
}
```

---

## 6. 테스트 파일 구조

```
traffic-light-search/
├── backend/
│   ├── trafficlight-domain/
│   │   └── src/test/java/com/trafficlight/domain/
│   │       ├── model/
│   │       │   └── TrafficLightTest.java          # 도메인 모델 테스트 (14 cases)
│   │       └── core/exception/
│   │           └── ExceptionTest.java             # 예외 계층 테스트 (5 cases)
│   │
│   ├── trafficlight-usecase/
│   │   └── src/test/java/com/trafficlight/usecase/
│   │       ├── SearchUseCaseTest.java             # 검색 UseCase (8 cases)
│   │       ├── IndexUseCaseTest.java              # 인덱스 UseCase (5 cases)
│   │       ├── AggregationUseCaseTest.java        # 집계 UseCase (8 cases)
│   │       └── DataSyncUseCaseTest.java           # 동기화 UseCase (6 cases)
│   │
│   └── trafficlight-api/
│       └── src/test/java/com/trafficlight/api/
│           ├── controller/
│           │   ├── SearchControllerTest.java      # 검색 API (7 cases)
│           │   ├── IndexControllerTest.java       # 인덱스 API (5 cases)
│           │   ├── AggregationControllerTest.java # 집계 API (4 cases)
│           │   └── DataSyncControllerTest.java    # 동기화 API (1 case)
│           └── dto/
│               ├── SearchResponseTest.java        # 응답 DTO (2 cases)
│               ├── AggregationResponseTest.java   # 집계 DTO (3 cases)
│               └── IndexStatusResponseTest.java   # 상태 DTO (2 cases)
│
└── frontend/
    └── src/__tests__/
        ├── lib/
        │   └── api.test.ts                        # API 유틸리티 (13 cases)
        └── components/
            ├── search/
            │   ├── SearchForm.test.tsx             # 검색폼 (5 cases)
            │   └── ResultsTable.test.tsx           # 결과 테이블 (9 cases)
            └── dashboard/
                └── StatsCard.test.tsx              # 통계 카드 (6 cases)
```

---

## 7. 향후 테스트 확장 권장 사항

### 7.1 추가 권장 테스트

| 우선순위 | 대상 | 유형 | 설명 |
|---------|------|------|------|
| 높음 | `ElasticsearchSearchAdapter` | 통합 테스트 | Testcontainers + ES로 실제 검색 동작 검증 |
| 높음 | `ElasticsearchIndexAdapter` | 통합 테스트 | 벌크 인덱싱, 매핑 적용 검증 |
| 높음 | `JsonDataLoader` | 단위 테스트 | 스트리밍 파서의 메모리 효율성, 필드 매핑 |
| 중간 | `FilterPanel` | 컴포넌트 테스트 | 시도 선택 → 시군구 연동, 초기화 |
| 중간 | `SearchPage` | 통합 테스트 | 검색 → 필터 → 페이지네이션 흐름 |
| 중간 | `DashboardPage` | 컴포넌트 테스트 | 집계 데이터 로딩 → 차트 렌더링 |
| 낮음 | `PublicDataApiAdapter` | WireMock 테스트 | 외부 API 호출/에러 처리 |
| 낮음 | `MapView` | 컴포넌트 테스트 | Leaflet 마커 렌더링 (jest-canvas-mock 필요) |

### 7.2 E2E 테스트

Playwright 또는 Cypress를 도입하여 전체 사용자 플로우를 검증할 수 있다:
- 검색어 입력 → 결과 테이블 표시 → 페이지 이동
- 필터 선택 → 검색 결과 갱신
- 지도 페이지에서 좌표 입력 → 마커 표시

### 7.3 성능 테스트

- `JsonDataLoader`: 55MB JSON 파일의 스트리밍 파싱 시간 측정
- `bulkUpsert`: 10,000건 벌크 인덱싱 처리량 측정
- `MapView`: 500개 마커 렌더링 시간 측정
