# GDGoC 인력사무소 백엔드

GDGoC 동아리원이 키워드(할 수 있어요 / 배우고 있어요 / 관심 있어요)로 서로를 찾고, 같이 해요 제안·모집글 지원을 수락하면 연결과 1:1 채팅방이 생기는 폐쇄형 서비스의 백엔드입니다. 명세는 [BACKEND_SPEC.md](BACKEND_SPEC.md), 명세에 없어서 정한 내용은 [ASSUMPTIONS.md](ASSUMPTIONS.md)에 있습니다.

### ⚙️ 기술 스택
- Java 17
- Spring Boot 4.1.0 (WebMVC, Data JPA, Security, Validation, WebSocket), Jackson 3
- MySQL 8, JWT(jjwt 0.12), springdoc-openapi 3.1, Lombok
- Gradle 8.14 (Wrapper 포함)

### ▶️ 실행 방법
1. DB 실행
   ```bash
   docker compose up -d
   ```
   Docker가 없으면 로컬 MySQL에서 직접 DB를 만들고, 계정이 `root/password`가 아니면 환경변수 `DB_USERNAME`, `DB_PASSWORD`를 설정합니다.
   ```sql
   CREATE DATABASE gdgoc_office DEFAULT CHARACTER SET utf8mb4;
   ```
2. 서버 실행 (기본 프로필 `local`, 테이블은 `ddl-auto: update`로 자동 생성)
   ```bash
   ./gradlew bootRun
   ```
3. Swagger UI: http://localhost:8080/swagger-ui/index.html
   - 로그인 API로 받은 `accessToken`을 우측 상단 **Authorize**에 넣으면 인증이 필요한 API를 호출할 수 있습니다.

로컬 환경변수 예시는 [.env.example](.env.example)에 있습니다 (`DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, `ADMIN_PASSWORD`). `local` 외 프로필에서는 `JWT_SECRET`(32바이트 이상)이 필수이고 `ddl-auto`는 `validate`입니다. 배포 환경변수는 아래 "배포 (Railway)"를 보세요.

### 🌱 로컬 시드 데이터
`local` 프로필로 기동하면 아래 데이터가 없을 때만 자동으로 들어갑니다. 다른 프로필(배포)에서는 `APP_SEED_ENABLED=true`일 때만 들어갑니다.
- 운영진 계정: `admin` / `admin1234!` (`ADMIN_PASSWORD`로 변경 가능)
- 초대 코드: `GDGOC-INVITE-01` ~ `GDGOC-INVITE-05`
- 기본 키워드 31개 (Backend, Spring, AI, 커피 등)

### 💬 채팅 실시간 수신 (WebSocket)
- `ws://localhost:8080/ws/chatrooms/{chatRoomId}?token={accessToken}` (또는 `Authorization: Bearer` 헤더)
- 해당 채팅방 참여자만 연결할 수 있고, 서버 → 클라이언트 수신 전용입니다. 메시지 전송은 REST(`POST /api/v1/chatrooms/{chatRoomId}/messages`)로 합니다.
- 이벤트: `{"event":"message.new","data":{...메시지}}`, `{"event":"message.read","data":{"lastReadMessageId":10}}`

### 🚀 배포 (Railway)
GitHub 저장소를 Railway 서비스에 연결하면 `main`에 푸시될 때마다 루트의 `Dockerfile`로 빌드해 자동 배포합니다.

1. Railway 프로젝트에 **MySQL** 서비스를 추가하고, **GitHub Repo**로 이 저장소를 연결합니다.
2. 백엔드 서비스의 **Variables**에 아래 환경변수를 넣습니다.
3. **Settings → Networking → Generate Domain**으로 `https://…up.railway.app` 주소를 만듭니다. 프론트의 `NEXT_PUBLIC_API_URL`에 이 주소를 넣습니다 (WebSocket은 자동으로 `wss://`).

| 변수 | 필수 | 설명 | 예시 |
| --- | --- | --- | --- |
| `SPRING_DATASOURCE_URL` | ✅ | MySQL JDBC 주소 | `jdbc:mysql://${{MySQL.MYSQLHOST}}:${{MySQL.MYSQLPORT}}/${{MySQL.MYSQLDATABASE}}?serverTimezone=Asia/Seoul&characterEncoding=UTF-8` |
| `SPRING_DATASOURCE_USERNAME` | ✅ | DB 계정 | `${{MySQL.MYSQLUSER}}` |
| `SPRING_DATASOURCE_PASSWORD` | ✅ | DB 비밀번호 | `${{MySQL.MYSQLPASSWORD}}` |
| `SPRING_JPA_HIBERNATE_DDL_AUTO` | 첫 배포 ✅ | 테이블 생성 방식. 기본값 `validate`는 테이블이 없으면 기동 실패 | 첫 배포 `update` → 스키마가 안정되면 `validate` |
| `JWT_SECRET` | ✅ | JWT 서명 키, **32바이트 이상** 임의 문자열 | `openssl rand -base64 48` 결과 |
| `CORS_ALLOWED_ORIGINS` | ✅ | 프론트 주소, 쉼표 구분. REST와 WebSocket(`/ws/**`)에 함께 적용. `*` 패턴 가능 | `https://agency.vercel.app,https://*.vercel.app` |
| `APP_SEED_ENABLED` | | `true`면 기동할 때 운영진 계정·초대 코드·기본 키워드를 넣음 (이미 있으면 건너뜀). 기본 `false` | 첫 배포 `true` |
| `ADMIN_PASSWORD` | 시드 켤 때 ✅ | 시드로 만드는 운영진 계정 비밀번호. 비어 있으면 기동 실패 | 임의의 강한 비밀번호 |
| `ADMIN_LOGIN_ID` | | 운영진 아이디 (기본 `admin`) | `admin` |
| `PORT` | | Railway가 자동으로 넣음 (기본 8080) | |

- `${{MySQL.…}}`은 같은 프로젝트의 MySQL 서비스 값을 참조하는 Railway 문법입니다 (서비스 이름이 `MySQL`일 때).
- 이미지는 `SPRING_PROFILES_ACTIVE=prod`로 실행되므로 `local` 프로필의 개발용 DB·JWT 키·시드 설정은 쓰이지 않습니다.
- `prod` 프로필(`application-prod.yml`)은 DB 접속 정보를 `SPRING_DATASOURCE_URL`·`SPRING_DATASOURCE_USERNAME`·`SPRING_DATASOURCE_PASSWORD`에서 읽습니다. 이 셋이나 `JWT_SECRET`이 비어 있으면 기동할 때 `prod 프로필 필수 환경변수가 없습니다: …` 에러에 빠진 이름이 나옵니다.
- Variables는 **MySQL 서비스가 아니라 백엔드 서비스**에 넣고, 바꾼 뒤에는 Railway 화면의 **Deploy**를 눌러 반영합니다.
- 운영진 계정은 처음 한 번만 만들어집니다. 이후 `ADMIN_PASSWORD`를 바꿔도 기존 계정의 비밀번호는 바뀌지 않습니다.
- 초대 코드 `GDGOC-INVITE-01`~`05`는 코드에 공개돼 있으니 운영에서는 필요하면 DB에서 바꿔 주세요.

### ✅ 테스트
```bash
./gradlew build
```
H2(MySQL 모드)로 통합 시나리오(가입 → 프로필 → 키워드 → 승인 → 검색 → 제안 → 수락 → 메시지 → 읽음), WebSocket 푸시, Swagger 문서(37개 API)를 검증합니다.

# Commit 메세지 작성 시 유의 사항

### 📋 Commit Message Convention
|   Gitmoji   |    Tag     | Description |
|:-----------:|:----------:| --- |
|      ✨      |   `feat`   | 새로운 기능 추가 |
|     🐛      |   `fix`    | 버그 수정 |
|     📝      |   `docs`   | 문서 추가, 수정, 삭제 |
|      ✅      |   `test`   | 테스트 코드 추가, 수정, 삭제 |
|     💄      |  `style`   | 코드 형식 변경 |
|     ♻️      | `refactor` | 코드 리팩토링 |
|     ⚡️      |   `perf`   | 성능 개선 |
|     💚      |    `ci`    | CI 관련 설정 수정 |
|     🚀      |  `chore`   | 기타 변경사항 |
|      🔥️      |  `remove`   | 코드 및 파일 제거 |
