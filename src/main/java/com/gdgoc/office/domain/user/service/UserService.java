package com.gdgoc.office.domain.user.service;

import com.gdgoc.office.domain.user.dto.MyInfoResponse;
import com.gdgoc.office.domain.user.dto.ProfileRequest;
import com.gdgoc.office.domain.user.entity.Profile;
import com.gdgoc.office.domain.user.entity.User;
import com.gdgoc.office.domain.user.entity.UserStatus;
import com.gdgoc.office.domain.user.repository.ProfileRepository;
import com.gdgoc.office.domain.user.repository.UserRepository;
import com.gdgoc.office.global.exception.BusinessException;
import com.gdgoc.office.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;

    @Transactional
    public void createProfile(Long userId, ProfileRequest.Create request) {
        User user = getUser(userId);
        if (profileRepository.existsByUserId(userId)) {
            throw new BusinessException(ErrorCode.PROFILE_ALREADY_EXISTS);
        }
        profileRepository.save(new Profile(user, request.bio().trim(), request.major().trim(),
                request.department(), request.contact().trim()));
    }

    @Transactional
    public void updateProfile(Long userId, ProfileRequest.Update request) {
        Profile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROFILE_NOT_FOUND));
        profile.update(trim(request.bio()), trim(request.major()), request.department(), trim(request.contact()));
    }

    @Transactional(readOnly = true)
    public MyInfoResponse getMyInfo(Long userId) {
        User user = getUser(userId);
        Profile profile = profileRepository.findByUserId(userId).orElse(null);
        return new MyInfoResponse(user.getId(), user.getName(),
                profile == null ? null : profile.getMajor(),
                profile == null ? null : profile.getDepartment(),
                profile == null ? null : profile.getBio(),
                profile == null ? null : profile.getContact(),
                user.getRole());
    }

    /** 승인된 멤버를 조회한다. 없거나 미승인이면 MEMBER404. */
    @Transactional(readOnly = true)
    public User getApprovedMember(Long memberId) {
        return userRepository.findByIdAndStatus(memberId, UserStatus.APPROVED)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_TOKEN));
    }

    private static String trim(String value) {
        return value == null ? null : value.trim();
    }
}
