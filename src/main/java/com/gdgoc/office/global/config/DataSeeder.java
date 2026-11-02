package com.gdgoc.office.global.config;

import com.gdgoc.office.domain.auth.entity.InviteCode;
import com.gdgoc.office.domain.auth.repository.InviteCodeRepository;
import com.gdgoc.office.domain.keyword.entity.Keyword;
import com.gdgoc.office.domain.keyword.entity.KeywordStatus;
import com.gdgoc.office.domain.keyword.repository.KeywordRepository;
import com.gdgoc.office.domain.user.entity.User;
import com.gdgoc.office.domain.user.repository.UserRepository;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** 로컬 프로필 시드 데이터 (§9). 이미 있는 데이터는 건너뛴다. */
@Slf4j
@Component
@Profile("local")
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    static final Map<String, String> INVITE_CODES = Map.of(
            "GDGOC-INVITE-01", "김지디",
            "GDGOC-INVITE-02", "이구글",
            "GDGOC-INVITE-03", "박개발",
            "GDGOC-INVITE-04", "최디자인",
            "GDGOC-INVITE-05", "정인공");

    static final List<String> KEYWORDS = List.of(
            "Backend", "Frontend", "Design", "AI", "LLM", "머신러닝", "딥러닝", "데이터 분석",
            "Python", "Java", "Kotlin", "JavaScript", "TypeScript", "Spring", "FastAPI", "Django",
            "React", "Next.js", "Flutter", "Android", "iOS", "UX/UI", "Figma", "DevOps", "Cloud",
            "영화", "커피", "철학", "창업", "독서", "운동");

    private final UserRepository userRepository;
    private final InviteCodeRepository inviteCodeRepository;
    private final KeywordRepository keywordRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.seed.admin-login-id}")
    private String adminLoginId;

    @Value("${app.seed.admin-password}")
    private String adminPassword;

    @Value("${app.seed.admin-name}")
    private String adminName;

    @Override
    @Transactional
    public void run(String... args) {
        if (!userRepository.existsByLoginId(adminLoginId)) {
            userRepository.save(User.createAdmin(adminLoginId, passwordEncoder.encode(adminPassword), adminName));
            log.info("[seed] 운영진 계정 생성: {}", adminLoginId);
        }
        INVITE_CODES.forEach((code, name) -> {
            if (!inviteCodeRepository.existsByCode(code)) {
                inviteCodeRepository.save(new InviteCode(code, name));
                log.info("[seed] 초대 코드 생성: {} ({})", code, name);
            }
        });
        for (String name : KEYWORDS) {
            if (keywordRepository.findByName(name).isEmpty()) {
                keywordRepository.save(new Keyword(name, KeywordStatus.APPROVED));
            }
        }
    }
}
