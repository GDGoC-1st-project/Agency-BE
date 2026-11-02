package com.gdgoc.office.domain.chat.service;

import com.gdgoc.office.domain.block.repository.BlockRepository;
import com.gdgoc.office.domain.chat.dto.ChatDtos;
import com.gdgoc.office.domain.chat.entity.ChatParticipant;
import com.gdgoc.office.domain.chat.entity.ChatRoom;
import com.gdgoc.office.domain.chat.entity.Message;
import com.gdgoc.office.domain.chat.repository.ChatParticipantRepository;
import com.gdgoc.office.domain.chat.repository.ChatRoomRepository;
import com.gdgoc.office.domain.chat.repository.MessageRepository;
import com.gdgoc.office.domain.chat.websocket.ChatEvent;
import com.gdgoc.office.domain.connection.entity.Connection;
import com.gdgoc.office.domain.post.entity.Post;
import com.gdgoc.office.domain.post.repository.ApplicationRepository;
import com.gdgoc.office.domain.post.dto.PostResponse;
import com.gdgoc.office.domain.request.entity.RequestType;
import com.gdgoc.office.domain.request.repository.ProposalRepository;
import com.gdgoc.office.domain.user.entity.User;
import com.gdgoc.office.domain.user.repository.UserRepository;
import com.gdgoc.office.global.common.PageResult;
import com.gdgoc.office.global.exception.BusinessException;
import com.gdgoc.office.global.exception.ErrorCode;
import com.gdgoc.office.global.util.TextUtils;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatParticipantRepository chatParticipantRepository;
    private final MessageRepository messageRepository;
    private final BlockRepository blockRepository;
    private final ProposalRepository proposalRepository;
    private final ApplicationRepository applicationRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    /** 방 접근 컨텍스트: 내 참여 정보와 상대 참여 정보 */
    private record RoomAccess(ChatRoom room, ChatParticipant mine, ChatParticipant partner) {
    }

    private record RoomRow(ChatRoom room, User partner, Message lastMessage) {
        LocalDateTime sortTime() {
            return lastMessage != null ? lastMessage.getCreatedAt() : room.getCreatedAt();
        }
    }

    @Transactional(readOnly = true)
    public PageResult<ChatDtos.RoomItem> getRooms(Long me, int page, int size) {
        Map<Long, ChatRoom> myRooms = chatParticipantRepository.findAllByUserId(me).stream()
                .map(ChatParticipant::getChatRoom)
                .collect(Collectors.toMap(ChatRoom::getId, Function.identity()));
        List<Long> roomIds = List.copyOf(myRooms.keySet());
        if (roomIds.isEmpty()) {
            return PageResult.of(List.of(), page, size, false);
        }
        Set<Long> blocked = new HashSet<>(blockRepository.findBlockedIdsByBlockerId(me));
        Map<Long, List<ChatParticipant>> participantsByRoom = chatParticipantRepository.findAllByChatRoomIdIn(roomIds)
                .stream().collect(Collectors.groupingBy(cp -> cp.getChatRoom().getId()));
        Map<Long, Message> lastMessages = messageRepository.findLastMessages(roomIds).stream()
                .collect(Collectors.toMap(m -> m.getChatRoom().getId(), Function.identity()));

        List<RoomRow> rows = participantsByRoom.entrySet().stream()
                .map(entry -> {
                    User partner = entry.getValue().stream()
                            .map(ChatParticipant::getUser)
                            .filter(user -> !user.getId().equals(me)).findFirst().orElse(null);
                    return partner == null ? null
                            : new RoomRow(myRooms.get(entry.getKey()), partner, lastMessages.get(entry.getKey()));
                })
                .filter(row -> row != null && !blocked.contains(row.partner().getId()))
                .sorted(Comparator.comparing(RoomRow::sortTime, Comparator.reverseOrder())
                        .thenComparing(row -> row.room().getId(), Comparator.reverseOrder()))
                .toList();

        int from = Math.min(page * size, rows.size());
        int to = Math.min(from + size, rows.size());
        List<RoomRow> pageRows = rows.subList(from, to);
        List<Long> pageRoomIds = pageRows.stream().map(row -> row.room().getId()).toList();
        Map<Long, Long> unread = pageRoomIds.isEmpty() ? Map.of()
                : messageRepository.countUnread(me, pageRoomIds).stream()
                        .collect(Collectors.toMap(r -> (Long) r[0], r -> (Long) r[1]));

        List<ChatDtos.RoomItem> items = pageRows.stream()
                .map(row -> new ChatDtos.RoomItem(row.room().getId(),
                        new ChatDtos.Member(row.partner().getId(), row.partner().getName()),
                        row.lastMessage() == null ? null : row.lastMessage().getContent(),
                        row.lastMessage() == null ? null : row.lastMessage().getCreatedAt(),
                        unread.getOrDefault(row.room().getId(), 0L).intValue()))
                .toList();
        return PageResult.of(items, page, size, rows.size() > to);
    }

    @Transactional(readOnly = true)
    public ChatDtos.RoomDetail getRoom(Long me, Long chatRoomId) {
        RoomAccess access = access(me, chatRoomId);
        User partner = access.partner().getUser();
        return new ChatDtos.RoomDetail(chatRoomId, new ChatDtos.Member(partner.getId(), partner.getName()),
                contextCard(access.room().getConnection()));
    }

    @Transactional(readOnly = true)
    public ChatDtos.MessagePage getMessages(Long me, Long chatRoomId, Long cursor, int size) {
        RoomAccess access = access(me, chatRoomId);
        List<Message> fetched = messageRepository.findPage(chatRoomId, cursor, PageRequest.of(0, size + 1));
        boolean hasNext = fetched.size() > size;
        List<Message> messages = hasNext ? fetched.subList(0, size) : fetched;
        Long partnerLastRead = access.partner().getLastReadMessageId();

        List<ChatDtos.MessageItem> items = messages.stream()
                .map(m -> toItem(m, me, partnerLastRead))
                .toList();
        Long nextCursor = hasNext ? messages.get(messages.size() - 1).getId() : null;
        return new ChatDtos.MessagePage(items, hasNext, nextCursor);
    }

    @Transactional
    public ChatDtos.Sent send(Long me, Long chatRoomId, String content) {
        RoomAccess access = access(me, chatRoomId);
        if (blockRepository.existsBetween(me, access.partner().getUser().getId())) {
            throw new BusinessException(ErrorCode.CHAT_FORBIDDEN);
        }
        Message message = messageRepository.save(new Message(access.room(), userRepository.getReferenceById(me), content));
        messageRepository.flush();

        // 수신자 기준: isMine=false, 상대(보낸 사람)의 메시지이므로 isRead=true
        ChatDtos.MessageItem pushed = new ChatDtos.MessageItem(message.getId(), me, false,
                message.getContent(), true, message.getCreatedAt());
        eventPublisher.publishEvent(new ChatEvent(chatRoomId, me, ChatEvent.MESSAGE_NEW, pushed));
        return new ChatDtos.Sent(message.getId(), message.getContent(), message.getCreatedAt());
    }

    @Transactional
    public void read(Long me, Long chatRoomId, Long lastReadMessageId) {
        RoomAccess access = access(me, chatRoomId);
        access.mine().read(lastReadMessageId);
        eventPublisher.publishEvent(new ChatEvent(chatRoomId, me, ChatEvent.MESSAGE_READ,
                Map.of("lastReadMessageId", access.mine().getLastReadMessageId())));
    }

    private RoomAccess access(Long me, Long chatRoomId) {
        ChatRoom room = chatRoomRepository.findWithConnectionById(chatRoomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_ROOM_NOT_FOUND));
        List<ChatParticipant> participants = chatParticipantRepository.findAllByChatRoomId(chatRoomId);
        ChatParticipant mine = participants.stream()
                .filter(cp -> cp.getUser().getId().equals(me)).findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_FORBIDDEN));
        ChatParticipant partner = participants.stream()
                .filter(cp -> !cp.getUser().getId().equals(me)).findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_FORBIDDEN));
        return new RoomAccess(room, mine, partner);
    }

    private ChatDtos.ContextCard contextCard(Connection connection) {
        if (connection.getSourceType() == RequestType.PROPOSAL) {
            return proposalRepository.findById(connection.getSourceId())
                    .map(p -> new ChatDtos.ContextCard(RequestType.PROPOSAL, p.getReasonType().getLabel(), p.getMessage()))
                    .orElse(new ChatDtos.ContextCard(RequestType.PROPOSAL, null, null));
        }
        return applicationRepository.findWithPostById(connection.getSourceId())
                .map(a -> {
                    Post post = a.getPost();
                    return new ChatDtos.ContextCard(RequestType.APPLICATION, post.getTitle(),
                            TextUtils.preview(post.getDescription(), PostResponse.PREVIEW_LENGTH));
                })
                .orElse(new ChatDtos.ContextCard(RequestType.APPLICATION, null, null));
    }

    private static ChatDtos.MessageItem toItem(Message message, Long me, Long partnerLastRead) {
        boolean mine = message.getSender().getId().equals(me);
        boolean read = !mine || (partnerLastRead != null && partnerLastRead >= message.getId());
        return new ChatDtos.MessageItem(message.getId(), message.getSender().getId(), mine,
                message.getContent(), read, message.getCreatedAt());
    }
}
