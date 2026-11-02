package com.gdgoc.office.domain.chat.repository;

import com.gdgoc.office.domain.chat.entity.ChatParticipant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ChatParticipantRepository extends JpaRepository<ChatParticipant, Long> {

    @Query("select cp from ChatParticipant cp join fetch cp.user where cp.chatRoom.id = :chatRoomId")
    List<ChatParticipant> findAllByChatRoomId(Long chatRoomId);

    @Query("select cp from ChatParticipant cp join fetch cp.chatRoom where cp.user.id = :userId")
    List<ChatParticipant> findAllByUserId(Long userId);

    @Query("select cp from ChatParticipant cp join fetch cp.user where cp.chatRoom.id in :chatRoomIds")
    List<ChatParticipant> findAllByChatRoomIdIn(Collection<Long> chatRoomIds);

    Optional<ChatParticipant> findByChatRoomIdAndUserId(Long chatRoomId, Long userId);

    boolean existsByChatRoomIdAndUserId(Long chatRoomId, Long userId);
}
