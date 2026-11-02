package com.gdgoc.office.domain.block.service;

import com.gdgoc.office.domain.block.entity.Block;
import com.gdgoc.office.domain.block.repository.BlockRepository;
import com.gdgoc.office.domain.user.entity.User;
import com.gdgoc.office.domain.user.repository.UserRepository;
import com.gdgoc.office.domain.user.service.UserService;
import com.gdgoc.office.global.exception.BusinessException;
import com.gdgoc.office.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BlockService {

    private final BlockRepository blockRepository;
    private final UserRepository userRepository;
    private final UserService userService;

    @Transactional
    public void block(Long userId, Long targetMemberId) {
        if (userId.equals(targetMemberId)) {
            throw new BusinessException(ErrorCode.BLOCK_SELF);
        }
        User target = userService.getApprovedMember(targetMemberId);
        if (blockRepository.existsByBlockerIdAndBlockedId(userId, targetMemberId)) {
            throw new BusinessException(ErrorCode.BLOCK_DUPLICATED);
        }
        blockRepository.save(new Block(userRepository.getReferenceById(userId), target));
    }

    @Transactional
    public void unblock(Long userId, Long targetMemberId) {
        Block block = blockRepository.findByBlockerIdAndBlockedId(userId, targetMemberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.BLOCK_NOT_FOUND));
        blockRepository.delete(block);
    }
}
