package com.gdgoc.office.domain.chat.repository;

import com.gdgoc.office.domain.chat.entity.ChatRoom;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    Optional<ChatRoom> findByConnectionId(Long connectionId);

    @Query("select r from ChatRoom r where r.connection.id in :connectionIds")
    List<ChatRoom> findAllByConnectionIdIn(Collection<Long> connectionIds);

    @Query("select r from ChatRoom r join fetch r.connection where r.id = :id")
    Optional<ChatRoom> findWithConnectionById(Long id);
}
