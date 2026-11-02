package com.gdgoc.office.domain.chat.repository;

import com.gdgoc.office.domain.chat.entity.Message;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface MessageRepository extends JpaRepository<Message, Long> {

    /** 방별 가장 최근 메시지 */
    @Query("""
            select m from Message m
            where m.id in (select max(m2.id) from Message m2 where m2.chatRoom.id in :chatRoomIds group by m2.chatRoom.id)
            """)
    List<Message> findLastMessages(Collection<Long> chatRoomIds);

    /** 방별 안 읽은 메시지 수 (내 last_read_message_id 이후 상대가 보낸 메시지) */
    @Query("""
            select m.chatRoom.id, count(m) from Message m, ChatParticipant cp
            where cp.chatRoom = m.chatRoom and cp.user.id = :me
              and m.chatRoom.id in :chatRoomIds
              and m.sender.id <> :me
              and (cp.lastReadMessageId is null or m.id > cp.lastReadMessageId)
            group by m.chatRoom.id
            """)
    List<Object[]> countUnread(Long me, Collection<Long> chatRoomIds);

    @Query("""
            select m from Message m
            where m.chatRoom.id = :chatRoomId and (:cursor is null or m.id < :cursor)
            order by m.id desc
            """)
    List<Message> findPage(Long chatRoomId, Long cursor, Pageable pageable);

    @Query("select m from Message m join fetch m.sender where m.id in :ids")
    List<Message> findAllWithSenderByIdIn(Collection<Long> ids);
}
