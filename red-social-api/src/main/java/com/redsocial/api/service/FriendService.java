package com.redsocial.api.service;

import com.redsocial.api.domain.Friendship;
import com.redsocial.api.domain.User;
import com.redsocial.api.exception.BadRequestException;
import com.redsocial.api.exception.ConflictException;
import com.redsocial.api.exception.ResourceNotFoundException;
import com.redsocial.api.mapper.DtoMapper;
import com.redsocial.api.repository.FollowRepository;
import com.redsocial.api.repository.FriendshipRepository;
import com.redsocial.api.repository.PostRepository;
import com.redsocial.api.repository.UserRepository;
import com.redsocial.common.dto.FriendRequestDto;
import com.redsocial.common.dto.FriendshipStatus;
import com.redsocial.common.dto.NotificationType;
import com.redsocial.common.dto.UserDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class FriendService {

    private final UserRepository userRepository;
    private final FriendshipRepository friendshipRepository;
    private final FollowRepository followRepository;
    private final PostRepository postRepository;
    private final DtoMapper mapper;
    private final NotificationService notificationService;

    public FriendService(UserRepository userRepository,
                         FriendshipRepository friendshipRepository,
                         FollowRepository followRepository,
                         PostRepository postRepository,
                         DtoMapper mapper,
                         NotificationService notificationService) {
        this.userRepository = userRepository;
        this.friendshipRepository = friendshipRepository;
        this.followRepository = followRepository;
        this.postRepository = postRepository;
        this.mapper = mapper;
        this.notificationService = notificationService;
    }

    @Transactional(readOnly = true)
    public List<UserDto> friends(Long currentUserId) {
        return friendshipRepository.findAllFor(currentUserId, FriendshipStatus.ACCEPTED).stream()
                .map(friendship -> toDto(other(friendship, currentUserId), currentUserId))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<FriendRequestDto> incoming(Long currentUserId) {
        return friendshipRepository
                .findByAddressee_IdAndStatusOrderByCreatedAtDesc(currentUserId, FriendshipStatus.PENDING).stream()
                .map(f -> new FriendRequestDto(f.getId(), toDto(f.getRequester(), currentUserId),
                        "PENDING", true, f.getCreatedAt()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<FriendRequestDto> outgoing(Long currentUserId) {
        return friendshipRepository
                .findByRequester_IdAndStatusOrderByCreatedAtDesc(currentUserId, FriendshipStatus.PENDING).stream()
                .map(f -> new FriendRequestDto(f.getId(), toDto(f.getAddressee(), currentUserId),
                        "PENDING", false, f.getCreatedAt()))
                .toList();
    }

    @Transactional(readOnly = true)
    public long incomingCount(Long currentUserId) {
        return friendshipRepository.countByAddressee_IdAndStatus(currentUserId, FriendshipStatus.PENDING);
    }

    /** Estado de la relación entre el usuario actual y otro: SELF/NONE/PENDING_OUT/PENDING_IN/ACCEPTED. */
    @Transactional(readOnly = true)
    public String status(Long currentUserId, String username) {
        User target = findByUsername(username);
        if (target.getId().equals(currentUserId)) {
            return "SELF";
        }
        Optional<Friendship> existing = friendshipRepository.findBetween(currentUserId, target.getId());
        if (existing.isEmpty()) {
            return "NONE";
        }
        Friendship friendship = existing.get();
        return switch (friendship.getStatus()) {
            case ACCEPTED -> "ACCEPTED";
            case REJECTED -> "NONE";
            case PENDING -> friendship.getRequester().getId().equals(currentUserId) ? "PENDING_OUT" : "PENDING_IN";
        };
    }

    @Transactional
    public String request(Long currentUserId, String username) {
        User target = findByUsername(username);
        if (target.getId().equals(currentUserId)) {
            throw new BadRequestException("No podés enviarte una solicitud a vos mismo");
        }
        User actor = getEntity(currentUserId);
        Optional<Friendship> existing = friendshipRepository.findBetween(currentUserId, target.getId());
        if (existing.isPresent()) {
            Friendship friendship = existing.get();
            if (friendship.getStatus() == FriendshipStatus.ACCEPTED) {
                throw new ConflictException("Ya son amigos");
            }
            if (friendship.getStatus() == FriendshipStatus.PENDING) {
                throw new ConflictException("Ya hay una solicitud pendiente");
            }
            friendship.setRequester(actor);
            friendship.setAddressee(target);
            friendship.setStatus(FriendshipStatus.PENDING);
            friendshipRepository.save(friendship);
        } else {
            friendshipRepository.save(Friendship.builder()
                    .requester(actor)
                    .addressee(target)
                    .status(FriendshipStatus.PENDING)
                    .build());
        }
        notificationService.notify(target, actor, NotificationType.FRIEND_REQUEST,
                actor.getId(), "@" + actor.getUsername() + " te envió una solicitud de amistad");
        return "PENDING_OUT";
    }

    @Transactional
    public UserDto accept(Long currentUserId, Long requestId) {
        Friendship friendship = friendshipRepository.findByIdAndAddressee_Id(requestId, currentUserId)
                .orElseThrow(() -> ResourceNotFoundException.of("Solicitud", requestId));
        if (friendship.getStatus() != FriendshipStatus.PENDING) {
            throw new ConflictException("La solicitud ya no está pendiente");
        }
        friendship.setStatus(FriendshipStatus.ACCEPTED);
        friendshipRepository.save(friendship);
        User requester = friendship.getRequester();
        User current = getEntity(currentUserId);
        notificationService.notify(requester, current, NotificationType.FRIEND_ACCEPT,
                current.getId(), "@" + current.getUsername() + " aceptó tu solicitud de amistad");
        return toDto(requester, currentUserId);
    }

    @Transactional
    public void reject(Long currentUserId, Long requestId) {
        Friendship friendship = friendshipRepository.findByIdAndAddressee_Id(requestId, currentUserId)
                .orElseThrow(() -> ResourceNotFoundException.of("Solicitud", requestId));
        if (friendship.getStatus() != FriendshipStatus.PENDING) {
            throw new ConflictException("La solicitud ya no está pendiente");
        }
        friendship.setStatus(FriendshipStatus.REJECTED);
        friendshipRepository.save(friendship);
    }

    @Transactional
    public void cancel(Long currentUserId, Long requestId) {
        Friendship friendship = friendshipRepository.findByIdAndRequester_Id(requestId, currentUserId)
                .orElseThrow(() -> ResourceNotFoundException.of("Solicitud", requestId));
        if (friendship.getStatus() != FriendshipStatus.PENDING) {
            throw new ConflictException("La solicitud ya no está pendiente");
        }
        friendshipRepository.delete(friendship);
    }

    @Transactional
    public void remove(Long currentUserId, String username) {
        User target = findByUsername(username);
        Friendship friendship = friendshipRepository.findBetween(currentUserId, target.getId())
                .orElseThrow(() -> new BadRequestException("No son amigos"));
        if (friendship.getStatus() != FriendshipStatus.ACCEPTED) {
            throw new BadRequestException("No son amigos");
        }
        friendshipRepository.delete(friendship);
    }

    @Transactional(readOnly = true)
    public long mutualCount(Long currentUserId, String username) {
        User target = findByUsername(username);
        if (target.getId().equals(currentUserId)) {
            return 0;
        }
        Set<Long> mine = acceptedFriendIds(currentUserId);
        Set<Long> theirs = acceptedFriendIds(target.getId());
        mine.retainAll(theirs);
        return mine.size();
    }

    private Set<Long> acceptedFriendIds(Long userId) {
        Set<Long> ids = new HashSet<>();
        for (Friendship friendship : friendshipRepository.findAllFor(userId, FriendshipStatus.ACCEPTED)) {
            ids.add(other(friendship, userId).getId());
        }
        return ids;
    }

    private User other(Friendship friendship, Long userId) {
        return friendship.getRequester().getId().equals(userId)
                ? friendship.getAddressee()
                : friendship.getRequester();
    }

    private User findByUsername(String username) {
        return userRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + username));
    }

    private User getEntity(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Usuario", id));
    }

    private UserDto toDto(User user, Long currentUserId) {
        boolean following = currentUserId != null
                && followRepository.existsByFollower_IdAndFollowing_Id(currentUserId, user.getId());
        boolean self = currentUserId != null && currentUserId.equals(user.getId());
        return mapper.toUserDto(
                user,
                following,
                followRepository.countByFollowing_Id(user.getId()),
                followRepository.countByFollower_Id(user.getId()),
                postRepository.countByAuthor_Id(user.getId()),
                self);
    }
}
