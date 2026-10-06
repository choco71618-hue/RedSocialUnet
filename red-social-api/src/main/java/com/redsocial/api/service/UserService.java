package com.redsocial.api.service;

import com.redsocial.api.domain.Follow;
import com.redsocial.api.domain.User;
import com.redsocial.api.exception.BadRequestException;
import com.redsocial.api.exception.ConflictException;
import com.redsocial.api.exception.ResourceNotFoundException;
import com.redsocial.api.mapper.DtoMapper;
import com.redsocial.api.repository.FollowRepository;
import com.redsocial.api.repository.PostRepository;
import com.redsocial.api.repository.UserRepository;
import com.redsocial.common.dto.UpdateProfileRequest;
import com.redsocial.common.dto.UserDto;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final FollowRepository followRepository;
    private final PostRepository postRepository;
    private final DtoMapper mapper;

    public UserService(UserRepository userRepository,
                       FollowRepository followRepository,
                       PostRepository postRepository,
                       DtoMapper mapper) {
        this.userRepository = userRepository;
        this.followRepository = followRepository;
        this.postRepository = postRepository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public User getEntity(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Usuario", id));
    }

    @Transactional(readOnly = true)
    public UserDto getProfile(String username, Long currentUserId) {
        User user = findByUsername(username);
        return toDto(user, currentUserId);
    }

    @Transactional
    public UserDto updateProfile(Long currentUserId, UpdateProfileRequest request) {
        User user = getEntity(currentUserId);
        if (request.displayName() != null) {
            String displayName = request.displayName().trim();
            if (displayName.isEmpty() || displayName.length() > 100) {
                throw new BadRequestException("El nombre para mostrar debe tener entre 1 y 100 caracteres");
            }
            user.setDisplayName(displayName);
        }
        if (request.bio() != null) {
            String bio = request.bio().trim();
            if (bio.length() > 500) {
                throw new BadRequestException("La biografía no puede superar los 500 caracteres");
            }
            user.setBio(bio.isEmpty() ? null : bio);
        }
        if (request.avatarUrl() != null) {
            String avatarUrl = request.avatarUrl().trim();
            if (avatarUrl.length() > 500) {
                throw new BadRequestException("La URL del avatar no puede superar los 500 caracteres");
            }
            user.setAvatarUrl(avatarUrl.isEmpty() ? null : avatarUrl);
        }
        userRepository.save(user);
        return toDto(user, currentUserId);
    }

    @Transactional(readOnly = true)
    public List<UserDto> search(String query, Long currentUserId) {
        if (query == null || query.trim().length() < 2) {
            throw new BadRequestException("La búsqueda debe tener al menos 2 caracteres");
        }
        return userRepository.search(query.trim(), PageRequest.of(0, 20)).stream()
                .map(user -> toDto(user, currentUserId))
                .toList();
    }

    @Transactional
    public UserDto follow(Long currentUserId, String username) {
        User target = findByUsername(username);
        if (target.getId().equals(currentUserId)) {
            throw new BadRequestException("No podés seguirte a vos mismo");
        }
        if (followRepository.existsByFollower_IdAndFollowing_Id(currentUserId, target.getId())) {
            throw new ConflictException("Ya seguís a este usuario");
        }
        followRepository.save(Follow.builder()
                .follower(getEntity(currentUserId))
                .following(target)
                .build());
        return toDto(target, currentUserId);
    }

    @Transactional
    public UserDto unfollow(Long currentUserId, String username) {
        User target = findByUsername(username);
        Follow follow = followRepository.findByFollower_IdAndFollowing_Id(currentUserId, target.getId())
                .orElseThrow(() -> new BadRequestException("No seguís a este usuario"));
        followRepository.delete(follow);
        return toDto(target, currentUserId);
    }

    @Transactional(readOnly = true)
    public List<UserDto> followers(String username, Long currentUserId) {
        User user = findByUsername(username);
        return followRepository.findByFollowing_Id(user.getId()).stream()
                .map(follow -> toDto(follow.getFollower(), currentUserId))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<UserDto> following(String username, Long currentUserId) {
        User user = findByUsername(username);
        return followRepository.findByFollower_Id(user.getId()).stream()
                .map(follow -> toDto(follow.getFollowing(), currentUserId))
                .toList();
    }

    private User findByUsername(String username) {
        return userRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + username));
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
