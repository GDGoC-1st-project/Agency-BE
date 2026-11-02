# GDGoC 인력사무소 백엔드 구현 지시서

> 이 문서는 Claude Code(터미널)에 그대로 넘겨서 백엔드 전체를 구현하게 하기 위한 지시서다.
> 경로, 필드명, 응답 코드, 메시지는 **글자 그대로** 구현한다. 문서에 없는 API는 만들지 않는다.
> 문서와 충돌하거나 모호한 부분은 임의로 판단하지 말고 `ASSUMPTIONS.md`에 기록한 뒤 가장 단순한 쪽으로 구현한다.

---

## 0. 작업 방식

1. 이 문서를 끝까지 읽는다. 작업 폴더가 비어 있으면 **§2.1 초기 세팅을 가장 먼저** 하고, 그다음 **§10 구현 순서**대로 단계별로 진행한다.
2. 각 단계가 끝날 때마다 `./gradlew build`가 통과하는지 확인하고, 통과하면 다음 단계로 넘어간다.
3. 모든 컨트롤러에 Swagger 문서화(§3.7)를 처음부터 함께 작성한다. 나중에 몰아서 하지 않는다.
4. 이미 존재하는 설정/클래스가 있으면 삭제하지 말고 맞춰서 확장한다.

---

## 1. 서비스 개요

GDGoC 동아리원이 자신의 능력·관심사를 키워드(할 수 있어요 / 배우고 있어요 / 관심 있어요)로 등록하고, 키워드로 멤버를 찾아 **같이 해요 제안** 또는 **모집글 지원**을 보내고, 수락하면 **연결(Connection)** 과 **1:1 채팅방**이 자동 생성되는 폐쇄형 서비스다.

- 가입은 운영진이 발급한 초대 코드로만 가능하고, 가입 후 운영진 승인(`APPROVED`)을 받아야 주요 기능을 쓸 수 있다.
- 핵심 흐름: 회원가입 → 프로필 작성 → 키워드 등록 → 승인 대기 → (승인) → 멤버 탐색 / 모집글 → 제안·지원 → 수락 → 연결 + 채팅

---

## 2. 기술 스택 및 프로젝트 구조

| 항목 | 내용 |
| --- | --- |
| 언어/빌드 | Java 17, Gradle |
| 프레임워크 | Spring Boot 3.5.x 최신 패치 (이미 프로젝트가 있으면 그 버전 사용), Spring Web, Spring Data JPA, Spring Security, Validation |
| DB | MySQL (로컬), ddl-auto: local은 `update`, 그 외 `validate` |
| 인증 | JWT (Access + Refresh), 비밀번호는 BCrypt |
| 문서화 | springdoc-openapi 2.8.x (Boot 3.5 기준, Boot 버전이 다르면 호환되는 버전 선택) |
| 실시간 | Spring WebSocket (채팅 수신 전용, §8.6) |
| 기타 | Lombok |
| 기본 패키지 | `com.gdgoc.office` (프로젝트에 이미 정해져 있으면 그것을 따른다) |

### 2.1 프로젝트 초기 세팅 (빈 폴더에서 시작)

작업 폴더가 비어 있으면 아래를 전부 직접 만든다. 이미 있는 파일은 덮어쓰지 말고 필요한 부분만 맞춘다.

1. **Gradle 프로젝트 생성**
   - 네트워크가 되면 Spring Initializr(start.spring.io)로 받고, 안 되면 `build.gradle`, `settings.gradle`, Gradle Wrapper(`gradlew`, `gradle/wrapper/*`)를 직접 작성한다.
   - Group `com.gdgoc`, Artifact `office`, Name `gdgoc-office`, Packaging Jar, Groovy DSL, **Java 17**, Spring Boot 3.5.x
2. **Java 17 고정**: `build.gradle`에 아래를 선언한다.
   ```groovy
   java {
       toolchain {
           languageVersion = JavaLanguageVersion.of(17)
       }
   }
   ```
3. **의존성**
   - `spring-boot-starter-web`, `spring-boot-starter-data-jpa`, `spring-boot-starter-security`, `spring-boot-starter-validation`, `spring-boot-starter-websocket`
   - `org.springdoc:springdoc-openapi-starter-webmvc-ui` (2.8.x)
   - `io.jsonwebtoken:jjwt-api`, `jjwt-impl`(runtime), `jjwt-jackson`(runtime) (0.12.x)
   - `com.mysql:mysql-connector-j` (runtimeOnly)
   - `lombok` (`compileOnly` + `annotationProcessor`)
   - 테스트: `spring-boot-starter-test`, `spring-security-test`, H2(testRuntimeOnly)
4. **메인 클래스**: `com.gdgoc.office.OfficeApplication` (`@SpringBootApplication`)
5. **설정 파일**: `src/main/resources/application.yml`(공통)과 `application-local.yml`(로컬). 비밀값은 환경변수로 받는다.

   `application.yml`
   ```yaml
   spring:
     application:
       name: gdgoc-office
     profiles:
       default: local
     jpa:
       open-in-view: false
       hibernate:
         ddl-auto: validate
       properties:
         hibernate:
           jdbc:
             time_zone: Asia/Seoul
     jackson:
       time-zone: Asia/Seoul
   jwt:
     secret: ${JWT_SECRET}
     access-expiration-minutes: 30
     refresh-expiration-days: 14
   ```

   `application-local.yml`
   ```yaml
   spring:
     datasource:
       url: jdbc:mysql://localhost:3306/gdgoc_office?serverTimezone=Asia/Seoul&characterEncoding=UTF-8
       username: ${DB_USERNAME:root}
       password: ${DB_PASSWORD:password}
     jpa:
       hibernate:
         ddl-auto: update
       show-sql: true
   jwt:
     secret: ${JWT_SECRET:local-dev-secret-key-must-be-at-least-32-bytes-long}
   app:
     seed:
       admin-login-id: admin
       admin-password: ${ADMIN_PASSWORD:admin1234!}
       admin-name: 운영진
   ```
6. **로컬 DB (MySQL 8)**: 루트에 `docker-compose.yml`을 만든다. Docker가 없으면 로컬 MySQL에서 `CREATE DATABASE gdgoc_office DEFAULT CHARACTER SET utf8mb4;`를 직접 실행하면 된다고 README에 적는다.
   ```yaml
   services:
     mysql:
       image: mysql:8.4
       container_name: gdgoc-office-mysql
       environment:
         MYSQL_ROOT_PASSWORD: password
         MYSQL_DATABASE: gdgoc_office
         TZ: Asia/Seoul
       command: --character-set-server=utf8mb4 --collation-server=utf8mb4_unicode_ci
       ports:
         - "3306:3306"
       volumes:
         - mysql-data:/var/lib/mysql
   volumes:
     mysql-data:
   ```
7. **기타 파일**: `.gitignore`(`build/`, `.gradle/`, `.idea/`, `*.env`), `.env.example`(`DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, `ADMIN_PASSWORD`), `README.md`(실행 방법: `docker compose up -d` → `./gradlew bootRun` → `http://localhost:8080/swagger-ui/index.html`)
8. **세팅 완료 확인**: `./gradlew build` 통과, `./gradlew bootRun`으로 기동, Swagger UI 접속까지 확인한 뒤 §10 단계 1의 나머지(`global` 패키지)로 넘어간다.

### 2.2 패키지 구조

```
com.gdgoc.office
├── global
│   ├── common        # CustomResponse, PageResult, BaseEntity
│   ├── exception     # ErrorCode, BusinessException, GlobalExceptionHandler
│   ├── security      # JwtProvider, JwtAuthenticationFilter, SecurityConfig, ApprovalFilter, 핸들러
│   ├── config        # OpenApiConfig, WebSocketConfig, JpaConfig
│   └── util
└── domain
    ├── auth          # 회원가입/로그인/토큰/승인상태, InviteCode, RefreshToken
    ├── user          # User, Profile, 내 정보
    ├── keyword       # Keyword, UserKeyword
    ├── member        # 멤버 검색/프로필
    ├── post          # Post, 모집글, Application(지원)
    ├── request       # 요청 목록/수락/거절, Proposal(제안)
    ├── connection    # Connection
    ├── chat          # ChatRoom, ChatParticipant, Message, WebSocket
    ├── block         # Block
    ├── report        # Report
    └── admin         # 운영 관리
```

각 domain은 `controller / service / repository / entity / dto` 하위 패키지를 가진다.

---

## 3. 공통 규칙

### 3.1 응답 포맷 (`CustomResponse<T>`)

모든 응답(성공/실패)은 이 포맷이다. JSON 필드명은 `isSuccess`, `code`, `message`, `result`이며, `result`가 null이면 필드를 생략한다(`@JsonInclude(NON_NULL)`). boolean 필드는 `@JsonProperty("isSuccess")`로 이름을 고정한다.

```json
{ "isSuccess": true, "code": "200 OK", "message": "요청 성공", "result": { } }
```
```json
{ "isSuccess": false, "code": "AUTH401", "message": "유효하지 않은 토큰입니다." }
```

- 정적 메서드: `onSuccess(code, message, result)`, `onSuccess(code, message)`, `onFailure(code, message)`
- 성공 code 문자열은 `"200 OK"`, `"201 Created"`만 사용한다. HTTP 상태도 동일하게 맞춘다.
- 실패 code는 `{도메인}{HTTP상태}` 형식이며 §3.3의 `ErrorCode` enum으로 관리한다.

### 3.2 페이지네이션 (`PageResult<T>`)

목록 API는 `page`(0부터, 기본 0), `size`(기본 20, 최대 100)를 받고 아래 형태로 반환한다. 별도 표기가 없는 한 `hasNext`는 Slice 방식(size+1 조회)으로 계산한다.

```json
{ "items": [ ], "page": 0, "size": 20, "hasNext": false }
```

채팅 메시지 목록만 커서 방식(`cursor`, `size`, `hasNext`, `nextCursor`)이다.

### 3.3 에러 처리

`ErrorCode` enum(code, httpStatus, message) + `BusinessException(ErrorCode)` + `@RestControllerAdvice GlobalExceptionHandler`.

**전역 공통 에러** (모든 API에 해당)

| code | HTTP | message | 발생 조건 |
| --- | --- | --- | --- |
| COMMON400 | 400 | 요청 값이 올바르지 않습니다. | 필수값 누락, 형식 오류, Validation 실패, Enum 파싱 실패, 타입 불일치 |
| AUTH401 | 401 | 유효하지 않은 토큰입니다. | 토큰 없음/만료/위조 (인증 필요 API) |
| AUTH403 | 403 | 운영진 승인 후 이용할 수 있습니다. | `PENDING` 사용자가 승인 필요 API 호출 |
| ADMIN403 | 403 | 운영진만 접근할 수 있습니다. | ADMIN이 아닌 사용자가 `/api/v1/admin/**` 호출 |
| COMMON500 | 500 | 서버 오류가 발생했습니다. | 그 외 예외 |

`MethodArgumentNotValidException`, `ConstraintViolationException`, `HttpMessageNotReadableException`, `MissingServletRequestParameterException`, `MethodArgumentTypeMismatchException`은 모두 `COMMON400`으로 변환한다. 도메인별 에러는 §7의 각 API에 적혀 있으며 전부 `ErrorCode`에 등록한다.

### 3.4 인증/인가

- 인증 불필요: `POST /api/v1/auth/signup`, `POST /api/v1/auth/login`, `POST /api/v1/auth/refresh`, Swagger 경로(`/swagger-ui/**`, `/v3/api-docs/**`), `/ws/**`(핸드셰이크에서 토큰 검증)
- 나머지는 `Authorization: Bearer {accessToken}` 필수. 실패 시 `AUTH401` JSON을 반환한다(`AuthenticationEntryPoint`).
- JWT claim: `userId`, `role`. Access 만료 30분, Refresh 만료 14일(설정값으로 분리).
- Security에서 발생하는 401/403도 반드시 §3.1 포맷의 JSON으로 응답한다.

### 3.5 승인 정책

사용자 `status`는 `PENDING`(가입 직후) → `APPROVED`(운영진 승인). `PENDING` 사용자가 호출 가능한 API는 아래뿐이고, 그 외는 `403 AUTH403`이다.

- `/api/v1/auth/**` 전체
- `POST /api/v1/users/me/profile`
- `GET /api/v1/users/me`
- `GET /api/v1/keywords`
- `PUT /api/v1/users/me/keywords`

`/api/v1/admin/**`는 `role = ADMIN`만 호출 가능하며 아니면 `403 ADMIN403`이다.

### 3.6 날짜/시간

`LocalDateTime`, ISO 8601, 타임존 없이 `2026-10-05T10:00:00` 형식으로 직렬화한다. 서버 타임존은 `Asia/Seoul`.

### 3.7 Swagger (springdoc-openapi)

- Swagger UI: `/swagger-ui/index.html`, 문서: `/v3/api-docs`
- `OpenApiConfig`에서 Bearer JWT 보안 스키마(`bearerAuth`)를 등록하고, 인증이 필요한 API에는 `@SecurityRequirement`를 건다.
- 모든 Controller에 `@Tag`, 모든 메서드에 `@Operation(summary, description)`, 모든 파라미터에 `@Parameter(description)`, 모든 DTO 필드에 `@Schema(description, example)`을 단다.
- 성공/실패 응답은 `@ApiResponse`로 문서화하고, 실패는 이 문서의 code/message를 예시로 넣는다.
- **모든 description은 한글**로 통일한다.

---

## 4. Enum

| Enum | 값 | 설명 |
| --- | --- | --- |
| Department | `DEV`, `AI`, `DESIGN` | 소속 분야 |
| Relation | `CAN`, `LEARNING`, `INTERESTED` | 할 수 있어요 / 배우고 있어요 / 관심 있어요 |
| ReasonType | `PROJECT`, `STUDY`, `CONTEST`, `COFFEE_CHAT`, `HOBBY`, `ETC` | 같이 해요 제안 사유 (라벨: 프로젝트 / 스터디 / 해커톤·공모전 / 커피챗 / 취미·관심사 / 기타) |
| RejectReasonType | `SCHEDULE`, `NOT_MATCHED`, `CLOSED`, `ETC` | 거절 사유 (일정 불가 / 관심사 불일치 / 모집 마감 / 기타) |
| RequestType | `PROPOSAL`, `APPLICATION` | 같이 해요 / 지원 |
| RequestStatus | `PENDING`, `ACCEPTED`, `REJECTED` | 대기 중 / 수락 / 거절 |
| UserStatus | `PENDING`, `APPROVED` | 승인 대기 / 승인 완료 |
| Role | `MEMBER`, `ADMIN` | 일반 / 운영진 |
| KeywordStatus | `APPROVED`, `PENDING` | 공식 키워드 / 검토 중 |
| ReportTargetType | `USER`, `POST`, `MESSAGE` | 신고 대상 유형 |
| ReportStatus | `PENDING`, `RESOLVED` | 처리 대기 / 처리 완료 |

---

## 5. 데이터 모델 (JPA 엔티티)

모든 PK는 `Long` + `GenerationType.IDENTITY`. 공통으로 `createdAt`(필요한 곳은 `updatedAt`)을 `BaseEntity`로 관리한다. Enum은 `@Enumerated(EnumType.STRING)`.

| 테이블 | 컬럼 | 제약/비고 |
| --- | --- | --- |
| users | id, login_id, password, name, status, role, keyword_registered(boolean, 기본 false), created_at | login_id UNIQUE. name은 초대 코드에 등록된 이름 |
| invite_codes | id, code, name, used(boolean), used_by(user id, nullable), created_at | code UNIQUE |
| refresh_tokens | id, user_id, token, expires_at | token UNIQUE. 재발급 시 폐기(Rotation) |
| profiles | id, user_id, bio, major, department, contact | user_id UNIQUE (1:1). 프로필 존재 여부 = 작성 여부 |
| keywords | id, name, status | name UNIQUE |
| user_keywords | id, user_id, keyword_id, relation | UNIQUE(user_id, keyword_id, relation) |
| posts | id, author_id, title, description(TEXT), deleted(boolean), created_at, updated_at | soft delete |
| post_required_roles | post_id, role(String) | `@ElementCollection` |
| post_keywords | id, post_id, keyword_id | UNIQUE(post_id, keyword_id) |
| applications | id, post_id, applicant_id, status, reject_type, reject_detail, created_at | UNIQUE(post_id, applicant_id) |
| proposals | id, sender_id, receiver_id, reason_type, message, status, reject_type, reject_detail, created_at | |
| connections | id, user_a_id, user_b_id, source_type(RequestType), source_id, created_at | 두 사용자 쌍 UNIQUE (id 작은 쪽을 user_a로 정규화) |
| chat_rooms | id, connection_id, created_at | connection_id UNIQUE (1:1) |
| chat_participants | id, chat_room_id, user_id, last_read_message_id(nullable) | UNIQUE(chat_room_id, user_id). 그룹 채팅 확장 대비 |
| messages | id, chat_room_id, sender_id, content, created_at | |
| blocks | id, blocker_id, blocked_id, created_at | UNIQUE(blocker_id, blocked_id) |
| reports | id, reporter_id, target_type, target_id, reason, status, memo, created_at, resolved_at | UNIQUE(reporter_id, target_type, target_id) |

연관관계는 `@ManyToOne(fetch = LAZY)`를 기본으로 하고, N+1이 생기는 목록 조회는 fetch join 또는 batch size로 해결한다.

---

## 6. 공통 비즈니스 규칙

- **대표 키워드(최대 3개)**: 멤버 검색·연결 목록·가입 승인 목록에서 쓴다. `CAN` → `LEARNING` → `INTERESTED` 순으로 채우고 각 관계 내에서는 id 오름차순. 단 멤버 검색에서 `keyword`가 있으면 검색어와 일치하는 키워드를 먼저 담는다.
- **차단**: 내가 차단한 멤버는 멤버 검색, 모집글 목록, 받은/보낸 요청 목록, 채팅방 목록에서 제외한다.
- **요청 목록 통합**: 받은/보낸 요청은 `Proposal`과 `Application` 두 테이블을 각각 조회해 `createdAt` 내림차순으로 병합한 뒤 페이지를 자른다. (각 테이블에서 `(page+1)*size+1`건씩 조회 후 병합·슬라이스). `requestId`는 테이블별로 따로 채번되므로 항상 `requestType`과 함께 다룬다.
  - 받은 요청: Proposal(receiver = 나) + Application(post 작성자 = 나, 삭제되지 않은 post, PENDING이 아닌 건도 포함)
  - 보낸 요청: Proposal(sender = 나) + Application(applicant = 나). Application의 receiver는 post 작성자.
  - `content`: PROPOSAL은 제안 메시지, APPLICATION은 지원한 모집글 제목. `reasonType`은 PROPOSAL만 값이 있고 APPLICATION은 null.
- **연결/채팅방 생성**: 요청 수락 시 한 트랜잭션에서 요청 상태를 `ACCEPTED`로 바꾸고, Connection 1개 + ChatRoom 1개 + ChatParticipant 2개를 만든다. 같은 두 사용자 사이에 이미 Connection이 있으면 새로 만들지 않고 기존 Connection/ChatRoom을 재사용해 그 id를 반환한다.
- **모집글 삭제**: soft delete. 이미 성사된 Connection/채팅방은 유지하고, 해당 글의 PENDING 지원은 요청 목록에서 더 이상 노출하지 않는다.

---

## 7. API 명세 (총 37개)

표기: 인증 열의 `PENDING 허용`은 §3.5 화이트리스트 API다. 별도 표기가 없으면 `APPROVED` 사용자 전용이다. "result 없음"이면 `{isSuccess, code, message}`만 반환한다. §3.3의 전역 공통 에러는 각 API에 다시 적지 않는다.

### 7.1 회원가입 · 로그인 (`domain/auth`)

#### [POST] /api/v1/auth/signup — 회원가입
- 인증: 불필요
- Body: `loginId`(String, 필수, 영문/숫자 4~20자), `password`(String, 필수, 8자 이상), `inviteCode`(String, 필수)
- 처리: 초대 코드가 존재하고 미사용인지 검증 → User 생성(`status=PENDING`, `role=MEMBER`, `name`=초대 코드에 등록된 이름, 비밀번호 BCrypt) → 초대 코드 사용 처리 → Access/Refresh 발급
- 성공: `201 Created` / "회원가입 성공" / result `{ userId:Long, accessToken:String, refreshToken:String, status:"PENDING" }`
- 실패: `INVITE400`(400) "유효하지 않거나 이미 사용된 초대 코드입니다." / `AUTH409`(409) "이미 사용 중인 아이디입니다."

#### [POST] /api/v1/auth/login — 로그인
- 인증: 불필요
- Body: `loginId`(필수), `password`(필수)
- 성공: `200 OK` / "로그인 성공" / result `{ accessToken, refreshToken, status, role, onboardingStep }`
- `onboardingStep` 결정 순서: 프로필 없음 → `PROFILE`, `keyword_registered=false` → `KEYWORD`, `status=PENDING` → `WAITING`, 그 외 `DONE`
- 실패: `LOGIN401`(401) "아이디 또는 비밀번호가 올바르지 않습니다."

#### [POST] /api/v1/auth/refresh — 토큰 재발급
- 인증: 불필요
- Body: `refreshToken`(String, 필수)
- 처리: DB의 Refresh Token 유효성/만료 검증 → 새 Access + 새 Refresh 발급, 이전 Refresh 폐기
- 성공: `200 OK` / "토큰 재발급 성공" / result `{ accessToken, refreshToken }`
- 실패: `REFRESH401`(401) "유효하지 않거나 만료된 Refresh Token입니다."

### 7.2 프로필 작성 · 키워드 등록 · 승인 대기 (`domain/user`, `keyword`, `auth`)

#### [POST] /api/v1/users/me/profile — 프로필 작성
- 인증: PENDING 허용
- Body: `bio`(String, 필수, 100자 이내), `major`(String, 필수), `department`(Department, 필수), `contact`(String, 필수)
- 성공: `201 Created` / "프로필 작성 성공" / result 없음
- 실패: `PROFILE409`(409) "이미 프로필이 작성되었습니다."

#### [GET] /api/v1/keywords — 키워드 검색(자동완성)
- 인증: PENDING 허용
- Query: `q`(String, 필수, 1자 이상), `size`(Integer, 기본 10)
- 처리: `status=APPROVED` 키워드만, 이름 부분 일치(대소문자 무시), 앞부분 일치를 먼저 정렬
- 성공: `200 OK` / "키워드 검색 성공" / result `{ items:[{ keywordId:Long, name:String }] }`
- 실패: 개별 실패 없음

#### [PUT] /api/v1/users/me/keywords — 내 키워드 저장
- 인증: PENDING 허용
- Body: `can`, `learning`, `interested` (각 `List<Long>` keywordId, 필수, 빈 배열 허용, 각각 최대 20개)
- 처리: 전달한 목록으로 관계별 전체 교체. 같은 키워드를 서로 다른 관계에 중복 등록 가능. 호출에 성공하면 `users.keyword_registered=true`.
- 성공: `200 OK` / "내 키워드 저장 성공" / result 없음
- 실패: `KEYWORD400`(400) "관계별 키워드는 최대 20개까지 등록할 수 있습니다." / `KEYWORD404`(404) "존재하지 않는 키워드가 포함되어 있습니다."

#### [GET] /api/v1/auth/approval-status — 승인 상태 조회
- 인증: PENDING 허용
- 성공: `200 OK` / "승인 상태 조회 성공" / result `{ status:"PENDING"|"APPROVED" }`
- 실패: 개별 실패 없음

### 7.3 멤버 탐색 · 멤버 프로필 · 같이 해요 · 신고 · 차단 (`domain/member`, `request`, `report`, `block`)

#### [GET] /api/v1/members — 멤버 검색
- Query: `keyword`(String, 선택), `relation`(Relation, 선택, keyword와 함께 사용), `department`(Department, 선택), `page`, `size`
- 처리: `APPROVED` 멤버만. 본인 제외, 내가 차단한 멤버 제외. `keyword`가 있으면 해당 키워드(부분 일치)를 가진 멤버, `relation`이 있으면 그 관계로 등록한 경우만. 정렬은 id 내림차순.
- 성공: `200 OK` / "멤버 검색 성공" / result `PageResult` items `[{ memberId:Long, name:String, department:String, keywords:List<String>(대표 키워드 ≤3) }]`
- 실패: 개별 실패 없음

#### [GET] /api/v1/members/{memberId} — 멤버 프로필 조회
- Path: `memberId`(Long)
- 성공: `200 OK` / "멤버 프로필 조회 성공" / result `{ memberId, name, bio, major, department, contact, keywords:{ can:[{keywordId,name}], learning:[…], interested:[…] } }`
- 실패: `MEMBER404`(404) "존재하지 않는 멤버입니다." (미승인 멤버도 동일)

#### [POST] /api/v1/proposals — 같이 해요 제안
- Body: `receiverId`(Long, 필수), `reasonType`(ReasonType, 필수), `message`(String, 필수, 200자 이내)
- 성공: `201 Created` / "제안 전송 성공" / result `{ proposalId:Long }`
- 실패: `PROPOSAL400`(400) "본인에게는 제안할 수 없습니다." / `PROPOSAL409`(409) "이미 연결되었거나 대기 중인 제안이 있습니다." / `MEMBER404`(404) "존재하지 않는 멤버입니다."

#### [POST] /api/v1/reports — 신고
- Body: `targetType`(ReportTargetType, 필수), `targetId`(Long, 필수), `reason`(String, 필수, 200자 이내)
- 처리: 대상 존재 확인(USER=멤버, POST=모집글, MESSAGE=메시지). 같은 신고자가 같은 대상을 중복 신고 불가.
- 성공: `201 Created` / "신고 접수 성공" / result `{ reportId:Long }`
- 실패: `REPORT404`(404) "신고 대상을 찾을 수 없습니다." / `REPORT409`(409) "이미 신고한 대상입니다."

#### [POST] /api/v1/blocks — 차단
- Body: `targetMemberId`(Long, 필수)
- 성공: `201 Created` / "차단 성공" / result 없음
- 실패: `BLOCK400`(400) "본인은 차단할 수 없습니다." / `BLOCK409`(409) "이미 차단한 멤버입니다." / `MEMBER404`(404) "존재하지 않는 멤버입니다."

#### [DELETE] /api/v1/blocks/{targetMemberId} — 차단 해제
- Path: `targetMemberId`(Long)
- 성공: `200 OK` / "차단 해제 성공" / result 없음
- 실패: `BLOCK404`(404) "차단한 내역이 없습니다."

### 7.4 모집 (`domain/post`)

#### [GET] /api/v1/posts — 모집글 목록 조회
- Query: `sort`(String, 기본 `LATEST`, 현재 LATEST만 지원), `page`, `size`
- 처리: 삭제되지 않은 글만, 내가 차단한 멤버의 글 제외, 최신순
- 성공: `200 OK` / "모집글 목록 조회 성공" / result `PageResult` items `[{ postId, title, descriptionPreview(설명 앞 100자), keywords:List<String>, authorName, createdAt }]`
- 실패: 개별 실패 없음

#### [GET] /api/v1/posts/{postId} — 모집글 상세 조회
- Path: `postId`(Long)
- 성공: `200 OK` / "모집글 상세 조회 성공" / result `{ postId, title, description, requiredRoles:List<String>, keywords:List<String>, author:{ memberId, name }, isAuthor:Boolean, hasApplied:Boolean, createdAt }`
- 실패: `POST404`(404) "존재하지 않는 모집글입니다." (삭제된 글 포함)

#### [POST] /api/v1/posts — 모집글 작성
- Body: `title`(String, 필수, 50자 이내), `description`(String, 필수), `requiredRoles`(List<String>, 필수, 1개 이상), `keywordIds`(List<Long>, 선택)
- 성공: `201 Created` / "모집글 작성 성공" / result `{ postId:Long }`
- 실패: `KEYWORD404`(404) "존재하지 않는 키워드가 포함되어 있습니다."

#### [POST] /api/v1/posts/{postId}/applications — 모집글 지원
- Path: `postId`(Long), Body 없음
- 성공: `201 Created` / "지원 성공" / result `{ applicationId:Long }`
- 실패: `APPLICATION409`(409) "이미 지원한 모집글입니다." / `APPLICATION400`(400) "본인의 모집글에는 지원할 수 없습니다." / `POST404`(404) "존재하지 않는 모집글입니다."

#### [PATCH] /api/v1/posts/{postId} — 모집글 수정
- Path: `postId`(Long)
- Body(모두 선택, 전달한 필드만 수정): `title`(50자 이내), `description`, `requiredRoles`(전달 시 전체 교체, 1개 이상), `keywordIds`(전달 시 전체 교체)
- 성공: `200 OK` / "모집글 수정 성공" / result 없음
- 실패: `POST404`(404) "존재하지 않는 모집글입니다." / `POST403`(403) "작성자만 수정할 수 있습니다." / `KEYWORD404`(404) "존재하지 않는 키워드가 포함되어 있습니다."

#### [DELETE] /api/v1/posts/{postId} — 모집글 삭제
- Path: `postId`(Long), 작성자만, soft delete (§6)
- 성공: `200 OK` / "모집글 삭제 성공" / result 없음
- 실패: `POST404`(404) "존재하지 않는 모집글입니다." / `POST403`(403) "작성자만 삭제할 수 있습니다."

### 7.5 요청 (`domain/request`)

#### [GET] /api/v1/requests/received — 받은 요청 목록
- Query: `status`(RequestStatus, 선택, 미입력 시 전체), `page`, `size`. 통합 조회 규칙은 §6.
- 성공: `200 OK` / "받은 요청 목록 조회 성공" / result `PageResult` items `[{ requestId:Long, requestType, sender:{ memberId, name }, content:String, reasonType:String|null, status, createdAt }]`
- 실패: 개별 실패 없음

#### [GET] /api/v1/requests/sent — 보낸 요청 목록
- Query: `status`(선택), `page`, `size`
- 성공: `200 OK` / "보낸 요청 목록 조회 성공" / result `PageResult` items `[{ requestId, requestType, receiver:{ memberId, name }, content, status, rejectReason:{ type, detail }|null, createdAt }]`
- `rejectReason`은 `REJECTED`일 때만 값이 있고 그 외 null. `detail`은 없으면 null.
- 실패: 개별 실패 없음

#### [PATCH] /api/v1/requests/{requestType}/{requestId}/accept — 요청 수락
- Path: `requestType`(RequestType), `requestId`(Long). Body 없음.
- 처리: 수신자만 가능, `PENDING`만 가능. §6의 연결/채팅방 생성 규칙 적용.
- 성공: `200 OK` / "요청 수락 성공" / result `{ connectionId:Long, chatRoomId:Long }`
- 실패: `REQUEST404`(404) "존재하지 않는 요청입니다." / `REQUEST403`(403) "요청을 처리할 권한이 없습니다." / `REQUEST409`(409) "이미 처리된 요청입니다."

#### [PATCH] /api/v1/requests/{requestType}/{requestId}/reject — 요청 거절
- Path: 위와 동일
- Body: `reasonType`(RejectReasonType, 필수), `reasonDetail`(String, 선택, 200자 이내, `reasonType=ETC`이면 필수 → 위반 시 `COMMON400`)
- 성공: `200 OK` / "요청 거절 성공" / result 없음
- 실패: `REQUEST404` / `REQUEST403` / `REQUEST409` (수락과 동일)

### 7.6 채팅 (`domain/chat`)

채팅방 생성 API는 없다(요청 수락 시 자동 생성). 참여자(2명)만 접근할 수 있다.

#### [GET] /api/v1/chatrooms — 채팅방 목록
- Query: `page`, `size`
- 처리: 내가 참여한 방, 최근 메시지 시각 내림차순(메시지가 없으면 방 생성 시각), 내가 차단한 상대와의 방 제외. `unreadCount`는 `last_read_message_id` 이후 상대가 보낸 메시지 수.
- 성공: `200 OK` / "채팅방 목록 조회 성공" / result `PageResult` items `[{ chatRoomId, member:{ memberId, name }, lastMessage:String|null, lastMessageAt:String|null, unreadCount:Integer }]`
- 실패: 개별 실패 없음

#### [GET] /api/v1/chatrooms/{chatRoomId} — 채팅방 상세 조회
- Path: `chatRoomId`(Long)
- 성공: `200 OK` / "채팅방 조회 성공" / result `{ chatRoomId, member:{ memberId, name }, contextCard:{ sourceType, title, content } }`
- `contextCard`: PROPOSAL이면 `title`=제안 사유 라벨(§4), `content`=제안 메시지 / APPLICATION이면 `title`=모집글 제목, `content`=모집글 설명 요약(앞 100자)
- 실패: `CHAT404`(404) "존재하지 않는 채팅방입니다." / `CHAT403`(403) "채팅방에 참여한 멤버만 접근할 수 있습니다."

#### [GET] /api/v1/chatrooms/{chatRoomId}/messages — 메시지 목록 조회
- Query: `cursor`(Long, 선택, 이 messageId보다 이전 메시지), `size`(기본 30)
- 처리: 최신 메시지가 먼저 오는 내림차순. `isRead`는 내가 보낸 메시지를 상대가 읽었는지(상대의 `last_read_message_id >= messageId`), 상대가 보낸 메시지는 true.
- 성공: `200 OK` / "메시지 목록 조회 성공" / result `{ items:[{ messageId, senderId, isMine:Boolean, content, isRead:Boolean, createdAt }], hasNext:Boolean, nextCursor:Long|null }`
- 실패: `CHAT404` / `CHAT403` (위와 동일)

#### [POST] /api/v1/chatrooms/{chatRoomId}/messages — 메시지 전송
- Body: `content`(String, 필수, 1~1000자)
- 처리: 저장 후 상대가 WebSocket에 연결돼 있으면 `message.new`를 푸시한다. 차단 관계의 방이면 `CHAT403`.
- 성공: `201 Created` / "메시지 전송 성공" / result `{ messageId, content, createdAt }`
- 실패: `CHAT404` / `CHAT403`

#### [PATCH] /api/v1/chatrooms/{chatRoomId}/read — 읽음 처리
- Body: `lastReadMessageId`(Long, 필수)
- 처리: 내 `last_read_message_id`를 갱신(작아지는 방향으로는 갱신하지 않음), 상대에게 `message.read` 푸시
- 성공: `200 OK` / "읽음 처리 성공" / result 없음
- 실패: `CHAT404` / `CHAT403`

### 7.7 마이 · 내 프로필 (`domain/user`)

#### [GET] /api/v1/users/me — 내 정보 조회
- 인증: PENDING 허용
- 성공: `200 OK` / "내 정보 조회 성공" / result `{ userId, name, major, department, bio, contact, role }` (프로필 미작성이면 major/department/bio/contact는 null)
- 실패: 개별 실패 없음

#### [PATCH] /api/v1/users/me/profile — 프로필 수정
- Body(모두 선택, 전달한 필드만 수정): `bio`(100자 이내), `major`, `department`, `contact`
- 성공: `200 OK` / "프로필 수정 성공" / result 없음
- 실패: `PROFILE404`(404) "작성된 프로필이 없습니다."

### 7.8 키워드 관리 · 내 연결 (`domain/keyword`, `connection`)

키워드 저장은 §7.2의 `PUT /api/v1/users/me/keywords`를 그대로 사용한다.

#### [GET] /api/v1/users/me/keywords — 내 키워드 조회
- 성공: `200 OK` / "내 키워드 조회 성공" / result `{ can:[{keywordId,name}], learning:[…], interested:[…] }`
- 실패: 개별 실패 없음

#### [GET] /api/v1/connections — 내 연결 목록
- Query: `page`, `size`
- 처리: 내가 속한 Connection, 최신순
- 성공: `200 OK` / "내 연결 목록 조회 성공" / result `PageResult` items `[{ connectionId, member:{ memberId, name, major, department }, keywords:List<String>(상대의 대표 키워드 ≤3), chatRoomId, createdAt }]`
- 실패: 개별 실패 없음

### 7.9 운영 관리 (`domain/admin`) — 전부 ADMIN 전용

#### [GET] /api/v1/admin/dashboard — 운영 관리 현황
- 성공: `200 OK` / "운영 관리 현황 조회 성공" / result `{ pendingSignupCount:Integer, pendingReportCount:Integer }`
- `pendingSignupCount`는 11.2 목록 조건과 동일한 기준으로 센다.

#### [GET] /api/v1/admin/signups — 가입 승인 대기 목록
- Query: `page`, `size`
- 처리: `status=PENDING`이면서 프로필 작성과 키워드 등록을 마친 사용자만
- 성공: `200 OK` / "가입 승인 대기 목록 조회 성공" / result `{ totalCount:Integer, items:[{ userId, name, major, department, keywords:List<String>(대표 키워드 ≤3) }], page, size, hasNext }`

#### [PATCH] /api/v1/admin/signups/{userId}/approve — 가입 승인
- Path: `userId`(Long). Body 없음. 승인 시 `status=APPROVED`.
- 성공: `200 OK` / "가입 승인 성공" / result 없음
- 실패: `USER404`(404) "존재하지 않는 사용자입니다." / `USER409`(409) "이미 승인된 사용자입니다."

#### [GET] /api/v1/admin/reports — 신고 목록
- Query: `status`(ReportStatus, 기본 `PENDING`), `page`, `size`
- 성공: `200 OK` / "신고 목록 조회 성공" / result `{ totalCount:Integer, items:[{ reportId, targetType, targetId, targetName, reporterName, reason, status, createdAt }], page, size, hasNext }`
- `targetName`: USER=이름, POST=제목, MESSAGE=보낸 사람 이름

#### [PATCH] /api/v1/admin/reports/{reportId}/resolve — 신고 처리
- Path: `reportId`(Long)
- Body: `memo`(String, 선택, 200자 이내)
- 처리: `status=RESOLVED`, `resolved_at` 기록
- 성공: `200 OK` / "신고 처리 성공" / result 없음
- 실패: `REPORT404`(404) "존재하지 않는 신고입니다." / `REPORT409`(409) "이미 처리된 신고입니다."

### 7.10 API 한눈에 보기

| 장 | Method | Path |
| --- | --- | --- |
| 7.1 | POST | /api/v1/auth/signup |
| 7.1 | POST | /api/v1/auth/login |
| 7.1 | POST | /api/v1/auth/refresh |
| 7.2 | POST | /api/v1/users/me/profile |
| 7.2 | GET | /api/v1/keywords |
| 7.2 | PUT | /api/v1/users/me/keywords |
| 7.2 | GET | /api/v1/auth/approval-status |
| 7.3 | GET | /api/v1/members |
| 7.3 | GET | /api/v1/members/{memberId} |
| 7.3 | POST | /api/v1/proposals |
| 7.3 | POST | /api/v1/reports |
| 7.3 | POST | /api/v1/blocks |
| 7.3 | DELETE | /api/v1/blocks/{targetMemberId} |
| 7.4 | GET | /api/v1/posts |
| 7.4 | GET | /api/v1/posts/{postId} |
| 7.4 | POST | /api/v1/posts |
| 7.4 | POST | /api/v1/posts/{postId}/applications |
| 7.4 | PATCH | /api/v1/posts/{postId} |
| 7.4 | DELETE | /api/v1/posts/{postId} |
| 7.5 | GET | /api/v1/requests/received |
| 7.5 | GET | /api/v1/requests/sent |
| 7.5 | PATCH | /api/v1/requests/{requestType}/{requestId}/accept |
| 7.5 | PATCH | /api/v1/requests/{requestType}/{requestId}/reject |
| 7.6 | GET | /api/v1/chatrooms |
| 7.6 | GET | /api/v1/chatrooms/{chatRoomId} |
| 7.6 | GET | /api/v1/chatrooms/{chatRoomId}/messages |
| 7.6 | POST | /api/v1/chatrooms/{chatRoomId}/messages |
| 7.6 | PATCH | /api/v1/chatrooms/{chatRoomId}/read |
| 7.7 | GET | /api/v1/users/me |
| 7.7 | PATCH | /api/v1/users/me/profile |
| 7.8 | GET | /api/v1/users/me/keywords |
| 7.8 | GET | /api/v1/connections |
| 7.9 | GET | /api/v1/admin/dashboard |
| 7.9 | GET | /api/v1/admin/signups |
| 7.9 | PATCH | /api/v1/admin/signups/{userId}/approve |
| 7.9 | GET | /api/v1/admin/reports |
| 7.9 | PATCH | /api/v1/admin/reports/{reportId}/resolve |

---

## 8. WebSocket (채팅 수신)

- 엔드포인트: `/ws/chatrooms/{chatRoomId}`
- 연결 시 Access Token을 검증하고(`Authorization` 헤더 또는 쿼리 `token`), 해당 방의 참여자만 연결을 허용한다. 실패하면 연결을 거부한다.
- 서버 → 클라이언트 이벤트(JSON, `{ "event": "...", "data": { } }`)
  - `message.new`: 새 메시지. `data`는 메시지 목록 조회의 item과 동일 구조이며 수신자 기준으로 `isMine=false`.
  - `message.read`: 상대가 읽음 처리했을 때. `data`는 `{ "lastReadMessageId": Long }`.
- 메시지 **전송은 REST**(`POST .../messages`)로만 한다. WebSocket은 수신 전용이다.
- 구현: `WebSocketConfig` + `TextWebSocketHandler`, `chatRoomId → 세션 목록`을 메모리 맵(ConcurrentHashMap)으로 관리한다. 단일 서버 기준이며 스케일 아웃은 고려하지 않는다.

---

## 9. 시드 데이터

로컬 프로필(`local`)에서 앱 기동 시 `CommandLineRunner`(또는 `data.sql`)로 아래를 넣는다. 이미 있으면 건너뛴다.

- **운영진 계정 1개**: `role=ADMIN`, `status=APPROVED`, `keyword_registered=true`, loginId/비밀번호/이름은 설정값(`application-local.yml`)에서 읽는다.
- **초대 코드 5개**: 임의 코드와 이름을 가진 미사용 초대 코드 (초대 코드 발급 API는 명세에 없으므로 DB/시드로만 발급한다)
- **기본 키워드(`APPROVED`)**: Backend, Frontend, Design, AI, LLM, 머신러닝, 데이터 분석, Python, Java, Spring, FastAPI, React, Flutter, UX/UI, Figma, 영화, 커피, 철학, 창업, 독서 등 30개 안팎

---

## 10. 구현 순서와 완료 조건

| 단계 | 내용 | 완료 조건 |
| --- | --- | --- |
| 1 | 프로젝트 초기 세팅(§2.1) + `global`(CustomResponse, PageResult, ErrorCode, 예외 처리, OpenApiConfig, BaseEntity) | 빌드 통과, 앱 기동, Swagger UI 접속 |
| 2 | Security + JWT + 승인 필터(§3.4, §3.5), auth 도메인(signup/login/refresh/approval-status), InviteCode | 가입→로그인→재발급 동작, 401/403이 JSON 포맷 |
| 3 | User/Profile, Keyword/UserKeyword(§7.2, §7.7, §7.8의 키워드 조회) | 프로필 작성/수정, 키워드 검색·저장·조회 |
| 4 | Block, Member 검색·프로필(§7.3 앞부분) | 대표 키워드·차단 제외 규칙 반영 |
| 5 | Post·Application(§7.4) | 작성자/지원자 분기, soft delete |
| 6 | Proposal, 요청 목록 통합 조회, 수락/거절, Connection + ChatRoom 자동 생성(§7.5, §7.8 연결 목록) | 수락 시 연결/채팅방 생성, 중복 연결 재사용 |
| 7 | 채팅 REST + WebSocket(§7.6, §8) | 전송/조회/읽음, 실시간 푸시 |
| 8 | Report, Admin(§7.9), 시드 데이터(§9) | 가입 승인·신고 처리 동작 |
| 9 | Swagger 점검: 모든 API가 문서에 나오고 description이 한글인지, 인증 버튼·예시 응답 확인 | `/v3/api-docs`에 37개 API |
| 10 | 통합 시나리오 테스트: 가입 → 프로필 → 키워드 → 관리자 승인 → 멤버 검색 → 제안 → 수락 → 메시지 전송 → 읽음 | 전체 플로우 통과 |

### Definition of Done
- §7의 37개 API가 경로, 필드, 응답 code/message까지 문서와 일치한다.
- 모든 실패 응답이 `CustomResponse` 포맷의 JSON이다(Security 예외 포함).
- Swagger UI에서 로그인 후 토큰을 넣고 모든 API를 호출해볼 수 있다.
- `./gradlew build`가 통과한다.

---

## 11. 명세에 없어서 정한 가정 (구현 후 `ASSUMPTIONS.md`에도 그대로 옮겨 적는다)

- 이름은 가입 폼에 없으므로 **초대 코드에 등록된 이름**을 사용한다.
- 초대 코드 발급 API는 없다. 시드/DB 직접 입력으로 발급한다.
- 소속 분야(`Department`)와 거절 사유(`RejectReasonType`) 값은 위 enum으로 가정한다.
- Access 30분 / Refresh 14일, Refresh는 재발급 시 폐기(Rotation).
- 요청 ID는 Proposal/Application이 따로 채번되고, 같은 두 사용자 사이에 연결이 이미 있으면 수락 시 기존 연결/채팅방을 재사용한다.
- 차단 관계의 채팅방은 목록에서 제외하고, 그 방에 메시지를 보내면 `CHAT403`을 반환한다.
- 모집글 삭제는 soft delete이며, 대기 중인 지원은 요청 목록에서 노출하지 않는다.
