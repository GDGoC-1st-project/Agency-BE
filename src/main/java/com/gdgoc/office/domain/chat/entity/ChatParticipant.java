package com.gdgoc.office.domain.chat.entity;

import com.gdgoc.office.domain.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "chat_participants", uniqueConstraints = @UniqueConstraint(
        name = "uk_chat_participants_room_user", columnNames = {"chat_room_id", "user_id"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "chat_room_id", nullable = false)
    private ChatRoom chatRoom;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "last_read_message_id")
    private Long lastReadMessageId;

    public ChatParticipant(ChatRoom chatRoom, User user) {
        this.chatRoom = chatRoom;
        this.user = user;
    }

    /** 읽음 위치를 갱신한다. 작아지는 방향으로는 갱신하지 않는다. */
    public void read(Long messageId) {
        if (lastReadMessageId == null || messageId > lastReadMessageId) {
            this.lastReadMessageId = messageId;
        }
    }
}
