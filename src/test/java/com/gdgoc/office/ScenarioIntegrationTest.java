package com.gdgoc.office;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gdgoc.office.domain.auth.entity.InviteCode;
import com.gdgoc.office.domain.auth.repository.InviteCodeRepository;
import com.gdgoc.office.domain.keyword.entity.Keyword;
import com.gdgoc.office.domain.keyword.entity.KeywordStatus;
import com.gdgoc.office.domain.keyword.repository.KeywordRepository;
import com.gdgoc.office.domain.user.entity.User;
import com.gdgoc.office.domain.user.repository.UserRepository;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * §10 통합 시나리오: 가입 → 프로필 → 키워드 → 관리자 승인 → 멤버 검색 → 제안 → 수락 → 메시지 전송 → 읽음
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ScenarioIntegrationTest {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    ObjectMapper objectMapper;
    @Autowired
    UserRepository userRepository;
    @Autowired
    InviteCodeRepository inviteCodeRepository;
    @Autowired
    KeywordRepository keywordRepository;
    @Autowired
    PasswordEncoder passwordEncoder;

    @Test
    void fullFlow() throws Exception {
        Long adminId = userRepository.save(User.createAdmin("admin", passwordEncoder.encode("admin1234!"), "운영진")).getId();
        inviteCodeRepository.save(new InviteCode("CODE-A", "김지디"));
        inviteCodeRepository.save(new InviteCode("CODE-B", "이구글"));
        Long spring = keywordRepository.save(new Keyword("Spring", KeywordStatus.APPROVED)).getId();
        Long java = keywordRepository.save(new Keyword("Java", KeywordStatus.APPROVED)).getId();
        Long ai = keywordRepository.save(new Keyword("AI", KeywordStatus.APPROVED)).getId();
        keywordRepository.save(new Keyword("SpringBoot Hidden", KeywordStatus.PENDING));

        // 인증 없이 호출 → AUTH401 JSON
        call(get("/api/v1/users/me"), null, null)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("AUTH401"));

        // 회원가입
        JsonNode signupA = body(call(post("/api/v1/auth/signup"), null,
                Map.of("loginId", "userA1", "password", "password1", "inviteCode", "CODE-A"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("201 Created"))
                .andExpect(jsonPath("$.message").value("회원가입 성공"))
                .andExpect(jsonPath("$.result.status").value("PENDING")));
        String tokenA = signupA.at("/result/accessToken").asText();
        Long userA = signupA.at("/result/userId").asLong();

        // 사용된 초대 코드 재사용 → INVITE400
        call(post("/api/v1/auth/signup"), null, Map.of("loginId", "userX1", "password", "password1", "inviteCode", "CODE-A"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVITE400"));
        // 아이디 중복 → AUTH409
        call(post("/api/v1/auth/signup"), null, Map.of("loginId", "userA1", "password", "password1", "inviteCode", "CODE-B"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("AUTH409"));
        // Validation 실패 → COMMON400
        call(post("/api/v1/auth/signup"), null, Map.of("loginId", "a", "password", "short", "inviteCode", "CODE-B"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON400"));

        JsonNode signupB = body(call(post("/api/v1/auth/signup"), null,
                Map.of("loginId", "userB1", "password", "password1", "inviteCode", "CODE-B"))
                .andExpect(status().isCreated()));
        String tokenB = signupB.at("/result/accessToken").asText();
        Long userB = signupB.at("/result/userId").asLong();

        // 로그인 → onboardingStep PROFILE
        call(post("/api/v1/auth/login"), null, Map.of("loginId", "userA1", "password", "password1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.onboardingStep").value("PROFILE"));
        call(post("/api/v1/auth/login"), null, Map.of("loginId", "userA1", "password", "wrongpass"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("LOGIN401"));

        // 토큰 재발급 + Rotation
        String refreshA = signupA.at("/result/refreshToken").asText();
        call(post("/api/v1/auth/refresh"), null, Map.of("refreshToken", refreshA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("토큰 재발급 성공"));
        call(post("/api/v1/auth/refresh"), null, Map.of("refreshToken", refreshA))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("REFRESH401"));

        // PENDING 사용자 → 승인 필요 API는 AUTH403
        call(get("/api/v1/members"), tokenA, null)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTH403"));
        call(get("/api/v1/auth/approval-status"), tokenA, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.status").value("PENDING"));

        // 프로필 작성
        Map<String, Object> profile = Map.of("bio", "백엔드 좋아요", "major", "컴퓨터공학과", "department", "DEV", "contact", "a@gdgoc.dev");
        call(post("/api/v1/users/me/profile"), tokenA, profile)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("프로필 작성 성공"))
                .andExpect(jsonPath("$.result").doesNotExist());
        call(post("/api/v1/users/me/profile"), tokenA, profile)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PROFILE409"));
        call(post("/api/v1/users/me/profile"), tokenB,
                Map.of("bio", "AI 공부 중", "major", "소프트웨어학과", "department", "AI", "contact", "b@gdgoc.dev"))
                .andExpect(status().isCreated());

        // 키워드 검색 (APPROVED만, 앞부분 일치 우선)
        call(get("/api/v1/keywords?q=spr"), tokenA, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.items.length()").value(1))
                .andExpect(jsonPath("$.result.items[0].name").value("Spring"));

        // 내 키워드 저장
        call(put("/api/v1/users/me/keywords"), tokenA, Map.of("can", new Long[]{spring, java}, "learning", new Long[]{ai}, "interested", new Long[]{}))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("내 키워드 저장 성공"));
        call(put("/api/v1/users/me/keywords"), tokenB, Map.of("can", new Long[]{ai}, "learning", new Long[]{spring}, "interested", new Long[]{9999L}))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("KEYWORD404"));
        call(put("/api/v1/users/me/keywords"), tokenB, Map.of("can", new Long[]{ai}, "learning", new Long[]{spring}, "interested", new Long[]{}))
                .andExpect(status().isOk());

        call(post("/api/v1/auth/login"), null, Map.of("loginId", "userA1", "password", "password1"))
                .andExpect(jsonPath("$.result.onboardingStep").value("WAITING"));

        // 관리자 로그인 → 승인 대기 목록 → 승인
        String adminToken = body(call(post("/api/v1/auth/login"), null, Map.of("loginId", "admin", "password", "admin1234!")))
                .at("/result/accessToken").asText();
        call(get("/api/v1/admin/dashboard"), tokenA, null)
                .andExpect(status().isForbidden());
        call(get("/api/v1/admin/dashboard"), adminToken, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.pendingSignupCount").value(2));
        call(get("/api/v1/admin/signups"), adminToken, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.totalCount").value(2))
                .andExpect(jsonPath("$.result.items[0].keywords[0]").value("Spring"));
        call(patch("/api/v1/admin/signups/" + userA + "/approve"), adminToken, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("가입 승인 성공"));
        call(patch("/api/v1/admin/signups/" + userA + "/approve"), adminToken, null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("USER409"));
        call(patch("/api/v1/admin/signups/" + userB + "/approve"), adminToken, null)
                .andExpect(status().isOk());

        // 승인된 멤버(일반 MEMBER)가 관리자 API 호출 → ADMIN403
        call(get("/api/v1/admin/dashboard"), tokenA, null)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ADMIN403"));

        // 멤버 검색 (키워드 일치 키워드 우선)
        call(get("/api/v1/members?keyword=ai"), tokenA, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.items.length()").value(1))
                .andExpect(jsonPath("$.result.items[0].memberId").value(userB))
                .andExpect(jsonPath("$.result.items[0].keywords[0]").value("AI"))
                .andExpect(jsonPath("$.result.hasNext").value(false));
        call(get("/api/v1/members?keyword=ai&relation=LEARNING"), tokenA, null)
                .andExpect(jsonPath("$.result.items.length()").value(0));
        call(get("/api/v1/members/" + userB), tokenA, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.keywords.can[0].name").value("AI"));
        call(get("/api/v1/members?relation=WRONG"), tokenA, null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON400"));

        // 같이 해요 제안
        call(post("/api/v1/proposals"), tokenA, Map.of("receiverId", userA, "reasonType", "PROJECT", "message", "hi"))
                .andExpect(jsonPath("$.code").value("PROPOSAL400"));
        Long proposalId = body(call(post("/api/v1/proposals"), tokenA,
                Map.of("receiverId", userB, "reasonType", "PROJECT", "message", "같이 해커톤 나가실래요?"))
                .andExpect(status().isCreated()))
                .at("/result/proposalId").asLong();
        call(post("/api/v1/proposals"), tokenA, Map.of("receiverId", userB, "reasonType", "STUDY", "message", "또"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PROPOSAL409"));

        // 모집글 작성 + 지원
        Long postId = body(call(post("/api/v1/posts"), tokenA,
                Map.of("title", "해커톤 팀원 모집", "description", "백엔드 구해요", "requiredRoles", new String[]{"백엔드"}, "keywordIds", new Long[]{spring}))
                .andExpect(status().isCreated()))
                .at("/result/postId").asLong();
        call(post("/api/v1/posts/" + postId + "/applications"), tokenA, null)
                .andExpect(jsonPath("$.code").value("APPLICATION400"));
        call(post("/api/v1/posts/" + postId + "/applications"), tokenB, null)
                .andExpect(status().isCreated());
        call(get("/api/v1/posts/" + postId), tokenB, null)
                .andExpect(jsonPath("$.result.isAuthor").value(false))
                .andExpect(jsonPath("$.result.hasApplied").value(true))
                .andExpect(jsonPath("$.result.keywords[0]").value("Spring"));
        call(patch("/api/v1/posts/" + postId), tokenB, Map.of("title", "남의 글"))
                .andExpect(jsonPath("$.code").value("POST403"))
                .andExpect(jsonPath("$.message").value("작성자만 수정할 수 있습니다."));
        call(get("/api/v1/posts"), tokenB, null)
                .andExpect(jsonPath("$.result.items[0].authorName").value("김지디"));

        // 받은 요청 (B: 제안 1건), 보낸 요청 (B: 지원 1건)
        call(get("/api/v1/requests/received"), tokenB, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.items.length()").value(1))
                .andExpect(jsonPath("$.result.items[0].requestType").value("PROPOSAL"))
                .andExpect(jsonPath("$.result.items[0].reasonType").value("PROJECT"));
        call(get("/api/v1/requests/received"), tokenA, null)
                .andExpect(jsonPath("$.result.items[0].requestType").value("APPLICATION"))
                .andExpect(jsonPath("$.result.items[0].content").value("해커톤 팀원 모집"))
                .andExpect(jsonPath("$.result.items[0].reasonType").isEmpty());

        // 수락 → 연결 + 채팅방
        call(patch("/api/v1/requests/PROPOSAL/" + proposalId + "/accept"), tokenA, null)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("REQUEST403"));
        JsonNode accepted = body(call(patch("/api/v1/requests/PROPOSAL/" + proposalId + "/accept"), tokenB, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("요청 수락 성공")));
        Long connectionId = accepted.at("/result/connectionId").asLong();
        Long chatRoomId = accepted.at("/result/chatRoomId").asLong();
        call(patch("/api/v1/requests/PROPOSAL/" + proposalId + "/accept"), tokenB, null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("REQUEST409"));

        // 지원 수락 시 기존 연결/채팅방 재사용
        Long applicationId = body(call(get("/api/v1/requests/received?status=PENDING"), tokenA, null))
                .at("/result/items/0/requestId").asLong();
        call(patch("/api/v1/requests/APPLICATION/" + applicationId + "/accept"), tokenA, null)
                .andExpect(jsonPath("$.result.connectionId").value(connectionId))
                .andExpect(jsonPath("$.result.chatRoomId").value(chatRoomId));

        call(get("/api/v1/connections"), tokenA, null)
                .andExpect(jsonPath("$.result.items.length()").value(1))
                .andExpect(jsonPath("$.result.items[0].member.memberId").value(userB))
                .andExpect(jsonPath("$.result.items[0].chatRoomId").value(chatRoomId));

        // 채팅방 상세 (PROPOSAL 컨텍스트 카드)
        call(get("/api/v1/chatrooms/" + chatRoomId), tokenA, null)
                .andExpect(jsonPath("$.result.contextCard.sourceType").value("PROPOSAL"))
                .andExpect(jsonPath("$.result.contextCard.title").value("프로젝트"));

        // 메시지 전송 → 목록 → 읽음
        Long messageId = body(call(post("/api/v1/chatrooms/" + chatRoomId + "/messages"), tokenA, Map.of("content", "안녕하세요!"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("메시지 전송 성공")))
                .at("/result/messageId").asLong();
        call(post("/api/v1/chatrooms/" + chatRoomId + "/messages"), tokenA, Map.of("content", "두 번째"))
                .andExpect(status().isCreated());

        call(get("/api/v1/chatrooms"), tokenB, null)
                .andExpect(jsonPath("$.result.items[0].unreadCount").value(2))
                .andExpect(jsonPath("$.result.items[0].lastMessage").value("두 번째"));
        call(get("/api/v1/chatrooms/" + chatRoomId + "/messages?size=1"), tokenA, null)
                .andExpect(jsonPath("$.result.items[0].isMine").value(true))
                .andExpect(jsonPath("$.result.items[0].isRead").value(false))
                .andExpect(jsonPath("$.result.hasNext").value(true))
                .andExpect(jsonPath("$.result.nextCursor").isNumber());

        JsonNode page = body(call(get("/api/v1/chatrooms/" + chatRoomId + "/messages"), tokenB, null));
        Long latest = page.at("/result/items/0/messageId").asLong();
        call(patch("/api/v1/chatrooms/" + chatRoomId + "/read"), tokenB, Map.of("lastReadMessageId", latest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("읽음 처리 성공"));
        call(patch("/api/v1/chatrooms/" + chatRoomId + "/read"), tokenB, Map.of("lastReadMessageId", messageId))
                .andExpect(status().isOk());
        call(get("/api/v1/chatrooms"), tokenB, null)
                .andExpect(jsonPath("$.result.items[0].unreadCount").value(0));
        call(get("/api/v1/chatrooms/" + chatRoomId + "/messages"), tokenA, null)
                .andExpect(jsonPath("$.result.items[0].isRead").value(true))
                .andExpect(jsonPath("$.result.items[1].isRead").value(true));

        // 비참여자 접근 → CHAT403
        call(get("/api/v1/chatrooms/" + chatRoomId), adminToken, null)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("CHAT403"));

        // 신고 (중복 불가) → 관리자 처리
        Long reportId = body(call(post("/api/v1/reports"), tokenA, Map.of("targetType", "MESSAGE", "targetId", messageId, "reason", "테스트 신고"))
                .andExpect(status().isCreated()))
                .at("/result/reportId").asLong();
        call(post("/api/v1/reports"), tokenA, Map.of("targetType", "MESSAGE", "targetId", messageId, "reason", "또"))
                .andExpect(jsonPath("$.code").value("REPORT409"));
        call(post("/api/v1/reports"), tokenA, Map.of("targetType", "POST", "targetId", 9999, "reason", "없는 글"))
                .andExpect(jsonPath("$.code").value("REPORT404"))
                .andExpect(jsonPath("$.message").value("신고 대상을 찾을 수 없습니다."));
        call(get("/api/v1/admin/reports"), adminToken, null)
                .andExpect(jsonPath("$.result.totalCount").value(1))
                .andExpect(jsonPath("$.result.items[0].targetName").value("김지디"));
        call(patch("/api/v1/admin/reports/" + reportId + "/resolve"), adminToken, Map.of("memo", "확인"))
                .andExpect(status().isOk());
        call(patch("/api/v1/admin/reports/" + reportId + "/resolve"), adminToken, Map.of("memo", "확인"))
                .andExpect(jsonPath("$.code").value("REPORT409"))
                .andExpect(jsonPath("$.message").value("이미 처리된 신고입니다."));

        // 차단 → 채팅방 목록/멤버 검색 제외, 메시지 전송 CHAT403 → 해제
        call(post("/api/v1/blocks"), tokenA, Map.of("targetMemberId", userB))
                .andExpect(status().isCreated());
        call(post("/api/v1/blocks"), tokenA, Map.of("targetMemberId", userB))
                .andExpect(jsonPath("$.code").value("BLOCK409"));
        call(get("/api/v1/chatrooms"), tokenA, null)
                .andExpect(jsonPath("$.result.items.length()").value(0));
        call(get("/api/v1/members"), tokenA, null)
                .andExpect(jsonPath("$.result.items[?(@.memberId == " + userB + ")]").isEmpty());
        call(post("/api/v1/chatrooms/" + chatRoomId + "/messages"), tokenB, Map.of("content", "차단됨?"))
                .andExpect(jsonPath("$.code").value("CHAT403"));
        call(delete("/api/v1/blocks/" + userB), tokenA, null)
                .andExpect(status().isOk());
        call(delete("/api/v1/blocks/" + userB), tokenA, null)
                .andExpect(jsonPath("$.code").value("BLOCK404"));

        // 거절: ETC 인데 상세 사유 없음 → COMMON400
        Long proposal2 = body(call(post("/api/v1/proposals"), tokenA,
                Map.of("receiverId", adminId, "reasonType", "COFFEE_CHAT", "message", "운영진님 커피챗")))
                .at("/result/proposalId").asLong();
        String rejectPath = "/api/v1/requests/PROPOSAL/" + proposal2 + "/reject";
        call(patch(rejectPath), adminToken, Map.of("reasonType", "ETC"))
                .andExpect(jsonPath("$.code").value("COMMON400"));
        call(patch(rejectPath), adminToken, Map.of("reasonType", "SCHEDULE"))
                .andExpect(status().isOk());
        call(get("/api/v1/requests/sent?status=REJECTED"), tokenA, null)
                .andExpect(jsonPath("$.result.items[0].rejectReason.type").value("SCHEDULE"))
                .andExpect(jsonPath("$.result.items[0].rejectReason.detail").isEmpty());

        // 모집글 삭제 (soft delete)
        call(delete("/api/v1/posts/" + postId), tokenA, null)
                .andExpect(status().isOk());
        call(get("/api/v1/posts/" + postId), tokenA, null)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("POST404"));
    }

    @Test
    void openApiDocumentsAll37Operations() throws Exception {
        JsonNode docs = objectMapper.readTree(mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        int count = 0;
        Iterator<JsonNode> paths = docs.get("paths").elements();
        while (paths.hasNext()) {
            JsonNode pathItem = paths.next();
            for (String method : new String[]{"get", "post", "put", "patch", "delete"}) {
                if (pathItem.has(method)) {
                    count++;
                }
            }
        }
        assertThat(count).isEqualTo(37);
        assertThat(docs.at("/components/securitySchemes/bearerAuth/scheme").asText()).isEqualTo("bearer");
        assertThat(docs.at("/paths/~1api~1v1~1auth~1signup/post/responses/400/content/application~1json/examples").toString())
                .contains("INVITE400").contains("COMMON400");
    }

    private ResultActions call(MockHttpServletRequestBuilder builder, String token, Object body) throws Exception {
        if (token != null) {
            builder.header("Authorization", "Bearer " + token);
        }
        if (body != null) {
            builder.contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(body));
        }
        return mockMvc.perform(builder);
    }

    private JsonNode body(ResultActions actions) throws Exception {
        return objectMapper.readTree(actions.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8));
    }
}
