package com.gdgoc.office.domain.connection.service;

import com.gdgoc.office.domain.chat.entity.ChatParticipant;
import com.gdgoc.office.domain.chat.entity.ChatRoom;
import com.gdgoc.office.domain.chat.repository.ChatParticipantRepository;
import com.gdgoc.office.domain.chat.repository.ChatRoomRepository;
import com.gdgoc.office.domain.connection.dto.ConnectionResponse;
import com.gdgoc.office.domain.connection.entity.Connection;
import com.gdgoc.office.domain.connection.repository.ConnectionRepository;
import com.gdgoc.office.domain.keyword.service.RepresentativeKeywordResolver;
import com.gdgoc.office.domain.request.entity.RequestType;
import com.gdgoc.office.domain.user.entity.Profile;
import com.gdgoc.office.domain.user.entity.User;
import com.gdgoc.office.domain.user.repository.ProfileRepository;
import com.gdgoc.office.global.common.PageResult;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ConnectionService {

    private final ConnectionRepository connectionRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatParticipantRepository chatParticipantRepository;
    private final ProfileRepository profileRepository;
    private final RepresentativeKeywordResolver representativeKeywordResolver;

    /**
     * 두 사용자를 연결하고 1:1 채팅방을 만든다 (§6).
     * 이미 연결이 있으면 새로 만들지 않고 기존 연결/채팅방을 재사용한다.
     */
    @Transactional
    public ConnectionResponse.Connected connect(User first, User second, RequestType sourceType, Long sourceId) {
        Connection connection = connectionRepository.findBetween(first.getId(), second.getId()).orElse(null);
        if (connection != null) {
            ChatRoom room = chatRoomRepository.findByConnectionId(connection.getId())
                    .orElseGet(() -> createRoom(connection, first, second));
            return new ConnectionResponse.Connected(connection.getId(), room.getId());
        }
        Connection created = connectionRepository.save(Connection.of(first, second, sourceType, sourceId));
        ChatRoom room = createRoom(created, first, second);
        return new ConnectionResponse.Connected(created.getId(), room.getId());
    }

    @Transactional(readOnly = true)
    public PageResult<ConnectionResponse.ListItem> getMyConnections(Long me, int page, int size) {
        Slice<Connection> connections = connectionRepository.findMyConnections(me, PageRequest.of(page, size));
        List<Long> connectionIds = connections.getContent().stream().map(Connection::getId).toList();
        List<Long> partnerIds = connections.getContent().stream().map(c -> c.partnerOf(me).getId()).toList();

        Map<Long, Long> roomIds = connectionIds.isEmpty() ? Map.of()
                : chatRoomRepository.findAllByConnectionIdIn(connectionIds).stream()
                        .collect(Collectors.toMap(r -> r.getConnection().getId(), ChatRoom::getId));
        Map<Long, Profile> profiles = partnerIds.isEmpty() ? Map.of()
                : profileRepository.findAllByUserIdIn(partnerIds).stream()
                        .collect(Collectors.toMap(p -> p.getUser().getId(), Function.identity()));
        Map<Long, List<String>> keywords = representativeKeywordResolver.resolve(partnerIds);

        return PageResult.of(connections, connection -> {
            User partner = connection.partnerOf(me);
            Profile profile = profiles.get(partner.getId());
            return new ConnectionResponse.ListItem(connection.getId(),
                    new ConnectionResponse.Member(partner.getId(), partner.getName(),
                            profile == null ? null : profile.getMajor(),
                            profile == null ? null : profile.getDepartment()),
                    keywords.getOrDefault(partner.getId(), List.of()),
                    roomIds.get(connection.getId()),
                    connection.getCreatedAt());
        });
    }

    private ChatRoom createRoom(Connection connection, User first, User second) {
        ChatRoom room = chatRoomRepository.save(new ChatRoom(connection));
        chatParticipantRepository.save(new ChatParticipant(room, first));
        chatParticipantRepository.save(new ChatParticipant(room, second));
        return room;
    }
}
