# Traffic Light Search - 보안 취약점 점검 보고서

**점검일**: 2026-03-28
**점검 범위**: Backend (Java/Spring Boot), Frontend (Next.js/React), Infrastructure (Docker/ES)
**점검 기준**: OWASP Top 10, CWE, 시큐어 코딩 가이드

---

## 1. 요약

| 등급 | 건수 | 상태 |
|------|------|------|
| **CRITICAL** | 2건 | 즉시 조치 필요 |
| **HIGH** | 5건 | 배포 전 조치 필요 |
| **MEDIUM** | 7건 | 다음 릴리스 전 조치 권장 |
| **LOW** | 6건 | 개선 권장 |
| **합계** | **20건** | |

---

## 2. CRITICAL 취약점

### SEC-001. Elasticsearch 인증/암호화 미적용

| 항목 | 내용 |
|------|------|
| **등급** | CRITICAL |
| **CWE** | CWE-306 (Missing Authentication for Critical Function) |
| **위치** | `docker-compose.yml:9-10` |
| **OWASP** | A07:2021 - Identification and Authentication Failures |

**현재 코드:**
```yaml
environment:
  - xpack.security.enabled=false       # Line 9
  - xpack.security.http.ssl.enabled=false  # Line 10
```

**위험:**
- Elasticsearch가 **인증 없이** 9200 포트에 노출
- 동일 네트워크의 누구나 `curl http://host:9200/_cat/indices`로 전체 데이터 조회 가능
- `DELETE /traffic-lights`로 인덱스 삭제 가능
- 통신이 평문(HTTP)이므로 패킷 스니핑으로 전체 데이터 탈취 가능

**조치 방안:**
```yaml
environment:
  - xpack.security.enabled=true
  - xpack.security.http.ssl.enabled=true
  - ELASTIC_PASSWORD=${ELASTIC_PASSWORD}
```

---

### SEC-002. 관리자 API 엔드포인트 인증/인가 부재

| 항목 | 내용 |
|------|------|
| **등급** | CRITICAL |
| **CWE** | CWE-862 (Missing Authorization) |
| **위치** | `IndexController.java:17-39`, `DataSyncController.java:22` |
| **OWASP** | A01:2021 - Broken Access Control |

**현재 코드:**
```java
// IndexController.java
@DeleteMapping("/delete")
public ResponseEntity<String> deleteIndex() {
    indexUseCase.deleteIndex();           // 누구나 인덱스 삭제 가능
    return ResponseEntity.ok("Index deleted");
}

@PostMapping("/load")
public ResponseEntity<IndexStatusResponse> loadData() {
    IndexStatusResult result = indexUseCase.loadData();  // 누구나 데이터 재로드 가능
}

// DataSyncController.java
@PostMapping
public ResponseEntity<IndexStatusResponse> syncFromApi() {
    IndexStatusResult result = dataSyncUseCase.syncFromApi();  // 누구나 API 동기화 트리거 가능
}
```

**위험:**
- 인증 없이 **인덱스 삭제**(데이터 전체 소실)
- 인증 없이 **데이터 리로드**(서버 리소스 소진, DoS)
- 인증 없이 **외부 API 동기화**(공공데이터 API 할당량 소진)

**노출 엔드포인트:**
| 엔드포인트 | 메서드 | 위험도 |
|-----------|--------|--------|
| `/api/index/delete` | DELETE | 데이터 전체 삭제 |
| `/api/index/create` | POST | 인덱스 재생성 |
| `/api/index/load` | POST | 대량 데이터 로드 (서버 부하) |
| `/api/sync` | POST | 외부 API 대량 호출 |

**조치 방안:**
```java
// Spring Security 의존성 추가 후
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(auth -> auth
            .requestMatchers(HttpMethod.GET, "/api/search/**").permitAll()
            .requestMatchers(HttpMethod.GET, "/api/aggregations/**").permitAll()
            .requestMatchers(HttpMethod.GET, "/api/index/status").permitAll()
            .requestMatchers("/api/index/**", "/api/sync/**").hasRole("ADMIN")
            .anyRequest().authenticated()
        );
        return http.build();
    }
}
```

---

## 3. HIGH 취약점

### SEC-003. 입력값 검증 부재 — 페이지네이션 파라미터

| 항목 | 내용 |
|------|------|
| **등급** | HIGH |
| **CWE** | CWE-20 (Improper Input Validation) |
| **위치** | `SearchController.java:30-31,52-53` |
| **OWASP** | A03:2021 - Injection |

**현재 코드:**
```java
@RequestParam(defaultValue = "0") int page,    // Line 30 — 음수 가능
@RequestParam(defaultValue = "20") int size     // Line 31 — 무제한 가능
```

**공격 시나리오:**
```
GET /api/search?size=999999999  → Elasticsearch에 100만 건 요청 → OOM
GET /api/search?page=-1&size=20 → page * size = 음수 → ES 오류 또는 예기치 않은 동작
```

**Elasticsearch 내부 연산:**
```java
// ElasticsearchSearchAdapter.java:160
.from(page * size)   // page=-1, size=20 → from=-20
.size(size)          // size=999999999 → 메모리 폭발
```

**조치 방안:**
```java
@GetMapping
public ResponseEntity<SearchResponse> search(
    @RequestParam(defaultValue = "0") @Min(0) int page,
    @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
```

---

### SEC-004. 입력값 검증 부재 — 위치 좌표

| 항목 | 내용 |
|------|------|
| **등급** | HIGH |
| **CWE** | CWE-20 (Improper Input Validation) |
| **위치** | `SearchController.java:49-50` |

**현재 코드:**
```java
@RequestParam double lat,   // Line 49 — 범위 제한 없음
@RequestParam double lon,   // Line 50 — 범위 제한 없음
```

**공격 시나리오:**
```
GET /api/search/geo?lat=99999&lon=99999&distance=99999km
→ Elasticsearch에 비정상적 geo_distance 쿼리 → 예측 불가 동작

GET /api/search/geo?lat=NaN&lon=Infinity
→ double 파싱 시 특수값 주입
```

**조치 방안:**
```java
@GetMapping("/geo")
public ResponseEntity<SearchResponse> geoSearch(
    @RequestParam @DecimalMin("-90") @DecimalMax("90") double lat,
    @RequestParam @DecimalMin("-180") @DecimalMax("180") double lon,
    @RequestParam(defaultValue = "1km") @Pattern(regexp = "^\\d+(\\.\\d+)?(km|m)$") String distance) {
```

---

### SEC-005. 필터 필드명 화이트리스트 미적용

| 항목 | 내용 |
|------|------|
| **등급** | HIGH |
| **CWE** | CWE-20 (Improper Input Validation) |
| **위치** | `SearchController.java:68-69`, `ElasticsearchSearchAdapter.java:130-132,195-200` |

**현재 코드:**
```java
// SearchController.java:68
@GetMapping("/filters/{field}")
public ResponseEntity<List<String>> getFilterOptions(@PathVariable String field, ...) {
    // field는 사용자 입력 — 검증 없이 Elasticsearch 집계에 사용
}

// ElasticsearchSearchAdapter.java:195
private String keywordField(String field) {
    if (KEYWORD_FIELDS.contains(field)) {
        return field;                   // 화이트리스트에 있으면 그대로
    }
    return field + ".keyword";          // 없으면 .keyword 접미사 추가 후 전달
}
```

**공격 시나리오:**
```
GET /api/search/filters/__proto__
GET /api/search/filters/../../etc/passwd
GET /api/search/filters/_source
→ 예상치 못한 Elasticsearch 필드 접근
```

**조치 방안:**
```java
private static final Set<String> ALLOWED_FILTER_FIELDS = Set.of(
    "sidoName", "sigunguName", "roadType", "trafficLightCategory"
);

@GetMapping("/filters/{field}")
public ResponseEntity<List<String>> getFilterOptions(@PathVariable String field, ...) {
    if (!ALLOWED_FILTER_FIELDS.contains(field)) {
        return ResponseEntity.badRequest().build();
    }
    // ...
}
```

---

### SEC-006. 외부 API 통신 평문(HTTP) 사용

| 항목 | 내용 |
|------|------|
| **등급** | HIGH |
| **CWE** | CWE-319 (Cleartext Transmission of Sensitive Information) |
| **위치** | `application.yml:13`, `PublicDataApiAdapter.java:131` |
| **OWASP** | A02:2021 - Cryptographic Failures |

**현재 코드:**
```yaml
# application.yml:13
base-url: http://api.data.go.kr/openapi/tn_pubr_public_traffic_light_api
```

```java
// PublicDataApiAdapter.java:131
return baseUrl + "?serviceKey=" + serviceKey + "&pageNo=" + pageNo + ...
// → http://api.data.go.kr/...?serviceKey=7cb08b3a... (평문 전송)
```

**위험:**
- API 키가 HTTP 평문으로 전송 → 네트워크 스니핑으로 탈취 가능
- MITM 공격으로 응답 데이터 변조 가능

**조치 방안:**
```yaml
base-url: https://api.data.go.kr/openapi/tn_pubr_public_traffic_light_api
```

---

### SEC-007. 예외 정보 누출 — RuntimeException 체이닝

| 항목 | 내용 |
|------|------|
| **등급** | HIGH |
| **CWE** | CWE-209 (Information Exposure Through an Error Message) |
| **위치** | `ElasticsearchSearchAdapter.java:58,69,106,125,152`, `ElasticsearchIndexAdapter.java:61,93,106` |
| **OWASP** | A04:2021 - Insecure Design |

**현재 코드:**
```java
// ElasticsearchSearchAdapter.java:58
} catch (IOException e) {
    throw new RuntimeException("Search failed", e);
    // cause 체인에 ES 연결정보, 쿼리 구조, 인덱스명 등 포함
}
```

**위험:**
- Spring Boot 기본 에러 핸들러가 스택트레이스를 JSON 응답으로 노출
- Elasticsearch 버전, 인덱스 구조, 내부 쿼리 등 공격에 유용한 정보 유출

**응답 예시 (현재):**
```json
{
  "timestamp": "2026-03-28T10:00:00",
  "status": 500,
  "error": "Internal Server Error",
  "trace": "java.lang.RuntimeException: Search failed\n\tat ...ElasticsearchSearchAdapter.search(...)
    Caused by: java.io.IOException: Connection refused (Connection refused)
    ...co.elastic.clients.transport.rest_client.RestClientTransport..."
}
```

**조치 방안:**
```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(TrafficLightDomainException.class)
    public ResponseEntity<Map<String, String>> handleDomainException(TrafficLightDomainException e) {
        return ResponseEntity.status(500)
            .body(Map.of("error", "요청을 처리할 수 없습니다."));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGenericException(Exception e) {
        log.error("Unhandled exception", e);  // 서버 로그에만 기록
        return ResponseEntity.status(500)
            .body(Map.of("error", "서버 오류가 발생했습니다."));
    }
}
```

---

## 4. MEDIUM 취약점

### SEC-008. 리소스 누수 — FileInputStream 미해제

| 항목 | 내용 |
|------|------|
| **등급** | MEDIUM |
| **CWE** | CWE-404 (Improper Resource Shutdown or Release) |
| **위치** | `JsonDataLoader.java:86-88` |

**현재 코드:**
```java
RecordIterator(String filePath) throws IOException {
    InputStream is = new FileInputStream(filePath);  // try-with-resources 없음
    JsonFactory factory = new JsonFactory();
    this.parser = factory.createParser(is);          // parser에 위임하지만...
    navigateToRecordsArray();                        // 여기서 예외 시 is 누수
    advance();
}
```

**위험:** `navigateToRecordsArray()` 또는 `advance()`에서 예외 발생 시 `FileInputStream`이 닫히지 않아 파일 디스크립터 누수.

**조치 방안:**
```java
RecordIterator(String filePath) throws IOException {
    try {
        InputStream is = new FileInputStream(filePath);
        JsonFactory factory = new JsonFactory();
        this.parser = factory.createParser(is);
        navigateToRecordsArray();
        advance();
    } catch (Exception e) {
        if (this.parser != null) this.parser.close();
        throw e;
    }
}
```

---

### SEC-009. CORS 설정 — allowedHeaders 와일드카드

| 항목 | 내용 |
|------|------|
| **등급** | MEDIUM |
| **CWE** | CWE-942 (Permissive Cross-domain Policy) |
| **위치** | `CorsConfig.java:14` |

**현재 코드:**
```java
registry.addMapping("/api/**")
    .allowedOrigins("http://localhost:3000")
    .allowedMethods("GET", "POST", "DELETE")
    .allowedHeaders("*");   // 모든 헤더 허용
```

**위험:**
- `allowedHeaders("*")`는 임의의 커스텀 헤더 전송 허용
- 프로덕션 배포 시 `allowedOrigins`이 localhost로 고정되어 프론트엔드 접근 불가 또는 `*`로 변경 시 보안 해제

**조치 방안:**
```java
registry.addMapping("/api/**")
    .allowedOrigins(corsAllowedOrigins)  // 환경변수로 주입
    .allowedMethods("GET", "POST", "DELETE")
    .allowedHeaders("Content-Type", "Accept", "Authorization")
    .maxAge(3600);
```

---

### SEC-010. Elasticsearch URI 파싱 취약

| 항목 | 내용 |
|------|------|
| **등급** | MEDIUM |
| **CWE** | CWE-20 (Improper Input Validation) |
| **위치** | `ElasticsearchConfig.java:21-22` |

**현재 코드:**
```java
String host = elasticsearchUri.replaceAll("https?://", "").split(":")[0];
int port = Integer.parseInt(elasticsearchUri.replaceAll("https?://", "").split(":")[1]);
```

**위험:**
- `elasticsearchUri`에 포트가 없으면 `ArrayIndexOutOfBoundsException`
- `"http://host:abc"` → `NumberFormatException`
- 에러 메시지에 URI 노출

**조치 방안:**
```java
URI uri = URI.create(elasticsearchUri);
String host = uri.getHost();
int port = uri.getPort() > 0 ? uri.getPort() : 9200;
String scheme = uri.getScheme();
```

---

### SEC-011. 요청 크기 제한 미설정

| 항목 | 내용 |
|------|------|
| **등급** | MEDIUM |
| **CWE** | CWE-770 (Allocation of Resources Without Limits) |
| **위치** | `application.yml` |

**위험:** 기본 Tomcat 설정으로 대용량 요청 허용 → 메모리 소진 공격 가능.

**조치 방안:**
```yaml
server:
  port: 8080
  tomcat:
    max-http-form-post-size: 2MB
    max-swallow-size: 2MB
  max-http-request-header-size: 8KB
```

---

### SEC-012. 보안 헤더 미설정

| 항목 | 내용 |
|------|------|
| **등급** | MEDIUM |
| **CWE** | CWE-693 (Protection Mechanism Failure) |
| **위치** | `application.yml`, 프론트엔드 `next.config.mjs` |

**누락 헤더:**
| 헤더 | 목적 | 현재 |
|------|------|------|
| `X-Content-Type-Options: nosniff` | MIME 타입 스니핑 방지 | 미설정 |
| `X-Frame-Options: DENY` | Clickjacking 방지 | 미설정 |
| `Content-Security-Policy` | XSS 방어 | 미설정 |
| `Strict-Transport-Security` | HTTPS 강제 | 미설정 |
| `X-XSS-Protection: 0` | 브라우저 XSS 필터 | 미설정 |

**조치 방안 (Backend):**
```java
@Configuration
public class SecurityHeadersConfig implements WebMvcConfigurer {
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new HandlerInterceptor() {
            @Override
            public boolean preHandle(HttpServletRequest req, HttpServletResponse res, Object handler) {
                res.setHeader("X-Content-Type-Options", "nosniff");
                res.setHeader("X-Frame-Options", "DENY");
                res.setHeader("X-XSS-Protection", "0");
                return true;
            }
        });
    }
}
```

**조치 방안 (Frontend — next.config.mjs):**
```javascript
const nextConfig = {
  async headers() {
    return [{
      source: '/(.*)',
      headers: [
        { key: 'X-Content-Type-Options', value: 'nosniff' },
        { key: 'X-Frame-Options', value: 'DENY' },
        { key: 'Content-Security-Policy', value: "default-src 'self'; img-src 'self' https://*.tile.openstreetmap.org https://cdnjs.cloudflare.com; style-src 'self' 'unsafe-inline'" },
      ],
    }];
  },
};
```

---

### SEC-013. Rate Limiting 미적용

| 항목 | 내용 |
|------|------|
| **등급** | MEDIUM |
| **CWE** | CWE-770 (Allocation of Resources Without Limits) |
| **위치** | 전체 Controller |
| **OWASP** | A04:2021 - Insecure Design |

**위험:**
- `/api/search` 무제한 호출 → Elasticsearch 과부하
- `/api/sync` 반복 호출 → 공공데이터 API 할당량 소진
- `/api/index/load` 반복 호출 → 서버 CPU/메모리 소진

**조치 방안:**
```java
// Bucket4j 또는 Spring Cloud Gateway 활용
@Bean
public FilterRegistrationBean<RateLimitFilter> rateLimitFilter() {
    // 검색: IP당 분당 60회
    // 관리: IP당 시간당 10회
}
```

---

### SEC-014. 프론트엔드 에러 메시지 원본 노출

| 항목 | 내용 |
|------|------|
| **등급** | MEDIUM |
| **CWE** | CWE-209 (Information Exposure Through an Error Message) |
| **위치** | `search/page.tsx:31`, `map/page.tsx:47`, `dashboard/page.tsx:34`, `Header.tsx:43` |

**현재 코드 (4곳 동일 패턴):**
```typescript
const message = err instanceof Error
    ? err.message                              // 백엔드 에러 메시지 그대로 노출
    : 'API 서버에 연결할 수 없습니다.';
setError(message);
```

**위험:** 백엔드가 상세 에러를 반환하면 Elasticsearch 내부 정보, 스택트레이스 등이 사용자에게 표시됨.

**조치 방안:**
```typescript
const message = err instanceof Error && process.env.NODE_ENV === 'development'
    ? err.message
    : '요청을 처리할 수 없습니다. 잠시 후 다시 시도해주세요.';
```

---

## 5. LOW 취약점

### SEC-015. Dockerfile — root 사용자 실행

| 항목 | 내용 |
|------|------|
| **등급** | LOW |
| **위치** | `backend/Dockerfile:20` |

**현재 코드:**
```dockerfile
ENTRYPOINT ["java", "-jar", "app.jar"]
# root 사용자로 실행됨
```

**조치 방안:**
```dockerfile
RUN addgroup --system appgroup && adduser --system --ingroup appgroup appuser
USER appuser
ENTRYPOINT ["java", "-jar", "app.jar"]
```

---

### SEC-016. 외부 CDN 리소스 무결성 미검증

| 항목 | 내용 |
|------|------|
| **등급** | LOW |
| **위치** | `MapView.tsx:21-25` |

**현재 코드:**
```typescript
iconRetinaUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/images/marker-icon-2x.png',
iconUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/images/marker-icon.png',
shadowUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/images/marker-shadow.png',
```

**위험:** CDN 침해 시 악성 리소스 로딩 가능. Subresource Integrity(SRI) 미적용.

---

### SEC-017. API 키 URL 쿼리 파라미터 노출

| 항목 | 내용 |
|------|------|
| **등급** | LOW |
| **위치** | `PublicDataApiAdapter.java:130-134` |

**현재 코드:**
```java
return baseUrl + "?serviceKey=" + serviceKey + "&pageNo=" + pageNo + ...
```

**위험:** 서비스 키가 URL 쿼리 스트링에 포함되어 웹 서버 액세스 로그, 프록시 로그 등에 기록됨.

---

### SEC-018. Elasticsearch 동적 매핑 허용

| 항목 | 내용 |
|------|------|
| **등급** | LOW |
| **위치** | `es-mapping.json` |

**위험:** `"dynamic": "strict"` 미설정으로 정의되지 않은 필드가 자동 인덱싱됨.

**조치 방안:**
```json
{
  "mappings": {
    "dynamic": "strict",
    "properties": { ... }
  }
}
```

---

### SEC-019. 데이터 동기화 타임아웃/취소 메커니즘 부재

| 항목 | 내용 |
|------|------|
| **등급** | LOW |
| **위치** | `DataSyncUseCase.java:25-75` |

**위험:** 5만 건 이상 동기화 시 수십 분 소요 가능. 요청 타임아웃이나 취소 메커니즘이 없어 서버 리소스가 장기간 점유됨.

---

### SEC-020. 로컬 파일 경로 설정 하드코딩

| 항목 | 내용 |
|------|------|
| **등급** | LOW |
| **위치** | `application.yml:9` |

**현재 코드:**
```yaml
data-file: /Users/cjmoon/dev/workspace/traffic-light-search/data/전국신호등표준데이터.json
```

**위험:** 개발자 로컬 경로가 설정 파일에 하드코딩. 사용자명과 디렉토리 구조 노출.

---

## 6. 보안 현황 긍정 사항

다음 항목은 올바르게 적용되어 있어 추가 조치가 필요하지 않습니다.

| 항목 | 상태 | 위치 |
|------|------|------|
| `.env` 파일 Git 미추적 | **양호** | `.gitignore`에 `.env*` 패턴 포함 |
| API 키 환경변수 참조 | **양호** | `application.yml:14` — `${SERVICE_KEY}` |
| XSS 방어 (프론트엔드) | **양호** | `dangerouslySetInnerHTML` 미사용, React 자동 이스케이프 |
| SQL Injection 해당 없음 | **양호** | RDBMS 미사용, Elasticsearch typed API 사용 |
| SSRF 방어 | **양호** | 사용자 입력이 URL 구성에 사용되지 않음 |
| Open Redirect 방어 | **양호** | Next.js `Link` 컴포넌트만 사용, 동적 리다이렉트 없음 |
| TypeScript strict 모드 | **양호** | `tsconfig.json` — `"strict": true` |
| CORS origin 제한 | **부분 양호** | `localhost:3000`으로 제한 (프로덕션 설정 필요) |
| 공공데이터 API 키 분리 | **양호** | `.env` → `application.yml` → `${SERVICE_KEY}` |

---

## 7. 조치 우선순위 로드맵

### Phase 1: 즉시 (프로덕션 배포 차단 기준)

| # | 취약점 | 조치 내용 | 예상 공수 |
|---|--------|-----------|-----------|
| SEC-001 | ES 인증 미적용 | xpack.security 활성화 + 비밀번호 설정 | 2시간 |
| SEC-002 | 관리 API 인증 부재 | Spring Security + API Key/Basic Auth 적용 | 4시간 |
| SEC-007 | 예외 정보 누출 | GlobalExceptionHandler 추가 | 1시간 |

### Phase 2: 배포 전 필수 (1주 내)

| # | 취약점 | 조치 내용 | 예상 공수 |
|---|--------|-----------|-----------|
| SEC-003 | 페이지네이션 검증 | @Min/@Max 어노테이션 + Validator | 1시간 |
| SEC-004 | 좌표 검증 | @DecimalMin/@DecimalMax 적용 | 1시간 |
| SEC-005 | 필드명 화이트리스트 | 허용 필드 Set 검증 | 30분 |
| SEC-006 | HTTP → HTTPS | base-url 변경 | 10분 |
| SEC-012 | 보안 헤더 | Interceptor/next.config 설정 | 1시간 |

### Phase 3: 다음 릴리스 (2주 내)

| # | 취약점 | 조치 내용 | 예상 공수 |
|---|--------|-----------|-----------|
| SEC-008 | 리소스 누수 | try-with-resources 적용 | 30분 |
| SEC-009 | CORS 강화 | 환경별 설정 + 헤더 명시 | 1시간 |
| SEC-010 | URI 파싱 | java.net.URI 사용 | 30분 |
| SEC-011 | 요청 크기 제한 | Tomcat 설정 | 15분 |
| SEC-013 | Rate Limiting | Bucket4j 또는 필터 적용 | 4시간 |
| SEC-014 | FE 에러 메시지 | 환경별 분기 처리 | 30분 |
| SEC-015 | Dockerfile USER | non-root 사용자 추가 | 15분 |

---

## 8. OWASP Top 10 (2021) 매핑

| OWASP 카테고리 | 해당 취약점 | 등급 |
|---------------|-----------|------|
| A01: Broken Access Control | SEC-002, SEC-005 | CRITICAL, HIGH |
| A02: Cryptographic Failures | SEC-001, SEC-006 | CRITICAL, HIGH |
| A03: Injection | SEC-003, SEC-004 | HIGH |
| A04: Insecure Design | SEC-007, SEC-013 | HIGH, MEDIUM |
| A05: Security Misconfiguration | SEC-009, SEC-010, SEC-011, SEC-012, SEC-018 | MEDIUM, LOW |
| A06: Vulnerable Components | 해당 없음 (최신 버전 사용) | — |
| A07: Auth Failures | SEC-001, SEC-002 | CRITICAL |
| A08: Data Integrity Failures | SEC-016 | LOW |
| A09: Logging Failures | SEC-017 | LOW |
| A10: SSRF | 해당 없음 | — |
