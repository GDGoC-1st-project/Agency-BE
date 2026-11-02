package com.gdgoc.office.domain.auth.service;

import com.gdgoc.office.domain.auth.dto.AuthRequest;
import com.gdgoc.office.domain.auth.dto.AuthResponse;
import com.gdgoc.office.domain.auth.dto.AuthResponse.OnboardingStep;
import com.gdgoc.office.domain.auth.entity.InviteCode;
import com.gdgoc.office.domain.auth.entity.RefreshToken;
import com.gdgoc.office.domain.auth.repository.InviteCodeRepository;
import com.gdgoc.office.domain.auth.repository.RefreshTokenRepository;
import com.gdgoc.office.domain.user.entity.User;
import com.gdgoc.office.domain.user.entity.UserStatus;
import com.gdgoc.office.domain.user.repository.ProfileRepository;
import com.gdgoc.office.domain.user.repository.UserRepository;
import com.gdgoc.office.global.exception.BusinessException;
import com.gdgoc.office.global.exception.ErrorCode;
import com.gdgoc.office.global.security.JwtProvider;
import java.time.LocalDateTime;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final InviteCodeRepository inviteCodeRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    @Transactional
    public AuthResponse.Signup signup(AuthRequest.Signup request) {
        InviteCode inviteCode = inviteCodeRepository.findByCodeForUpdate(request.inviteCode().trim())
                .filter(code -> !code.isUsed())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVITE_CODE_INVALID));
        if (userRepository.existsByLoginId(request.loginId())) {
            throw new BusinessException(ErrorCode.LOGIN_ID_DUPLICATED);
        }

        User user = userRepository.save(User.createMember(
                request.loginId(), passwordEncoder.encode(request.password()), inviteCode.getName()));
        inviteCode.use(user.getId());

        String accessToken = jwtProvider.createAccessToken(user.getId(), user.getRole());
        String refreshToken = issueRefreshToken(user.getId());
        return new AuthResponse.Signup(user.getId(), accessToken, refreshToken, user.getStatus());
    }

    @Transactional
    public AuthResponse.Login login(AuthRequest.Login request) {
        User user = userRepository.findByLoginId(request.loginId())
                .filter(found -> passwordEncoder.matches(request.password(), found.getPassword()))
                .orElseThrow(() -> new BusinessException(ErrorCode.LOGIN_FAILED));

        String accessToken = jwtProvider.createAccessToken(user.getId(), user.getRole());
        String refreshToken = issueRefreshToken(user.getId());
        return new AuthResponse.Login(accessToken, refreshToken, user.getStatus(), user.getRole(),
                resolveOnboardingStep(user));
    }

    @Transactional(noRollbackFor = BusinessException.class)
    public AuthResponse.Token refresh(AuthRequest.Refresh request) {
        RefreshToken stored = refreshTokenRepository.findByToken(request.refreshToken())
                .orElseThrow(() -> new BusinessException(ErrorCode.REFRESH_TOKEN_INVALID));
        if (stored.isExpired(now()) || jwtProvider.parseRefreshToken(stored.getToken()).isEmpty()) {
            refreshTokenRepository.delete(stored);
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_INVALID);
        }
        User user = userRepository.findById(stored.getUserId())
                .orElseThrow(() -> new BusinessException(ErrorCode.REFRESH_TOKEN_INVALID));

        refreshTokenRepository.delete(stored);
        String accessToken = jwtProvider.createAccessToken(user.getId(), user.getRole());
        String refreshToken = issueRefreshToken(user.getId());
        return new AuthResponse.Token(accessToken, refreshToken);
    }

    @Transactional(readOnly = true)
    public AuthResponse.ApprovalStatus getApprovalStatus(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_TOKEN));
        return new AuthResponse.ApprovalStatus(user.getStatus());
    }

    private String issueRefreshToken(Long userId) {
        String token = jwtProvider.createRefreshToken(userId);
        refreshTokenRepository.save(new RefreshToken(userId, token, jwtProvider.refreshExpiresAt()));
        return token;
    }

    private OnboardingStep resolveOnboardingStep(User user) {
        if (!profileRepository.existsByUserId(user.getId())) {
            return OnboardingStep.PROFILE;
        }
        if (!user.isKeywordRegistered()) {
            return OnboardingStep.KEYWORD;
        }
        if (user.getStatus() == UserStatus.PENDING) {
            return OnboardingStep.WAITING;
        }
        return OnboardingStep.DONE;
    }

    private LocalDateTime now() {
        return LocalDateTime.now(ZoneId.of("Asia/Seoul"));
    }
}
