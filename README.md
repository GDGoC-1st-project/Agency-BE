# GDGoC 인력사무소 백엔드

GDGoC 동아리원이 키워드(할 수 있어요 / 배우고 있어요 / 관심 있어요)로 서로를 찾고, 같이 해요 제안·모집글 지원을 수락하면 연결과 1:1 채팅방이 생기는 폐쇄형 서비스의 백엔드입니다. 명세는 [BACKEND_SPEC.md](BACKEND_SPEC.md), 명세에 없어서 정한 내용은 [ASSUMPTIONS.md](ASSUMPTIONS.md)에 있습니다.

### ⚙️ 기술 스택
- Java 17
- Spring Boot 3.5.16 (Web, Data JPA, Security, Validation, WebSocket)
- MySQL 8, JWT(jjwt 0.12), springdoc-openapi 2.8, Lombok
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

환경변수 예시는 [.env.example](.env.example)에 있습니다 (`DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, `ADMIN_PASSWORD`). `local` 외 프로필에서는 `JWT_SECRET`(32바이트 이상)이 필수이고 `ddl-auto`는 `validate`입니다.

### 🌱 로컬 시드 데이터
`local` 프로필로 기동하면 아래 데이터가 없을 때만 자동으로 들어갑니다.
- 운영진 계정: `admin` / `admin1234!` (`ADMIN_PASSWORD`로 변경 가능)
- 초대 코드: `GDGOC-INVITE-01` ~ `GDGOC-INVITE-05`
- 기본 키워드 31개 (Backend, Spring, AI, 커피 등)

### 💬 채팅 실시간 수신 (WebSocket)
- `ws://localhost:8080/ws/chatrooms/{chatRoomId}?token={accessToken}` (또는 `Authorization: Bearer` 헤더)
- 해당 채팅방 참여자만 연결할 수 있고, 서버 → 클라이언트 수신 전용입니다. 메시지 전송은 REST(`POST /api/v1/chatrooms/{chatRoomId}/messages`)로 합니다.
- 이벤트: `{"event":"message.new","data":{...메시지}}`, `{"event":"message.read","data":{"lastReadMessageId":10}}`

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
