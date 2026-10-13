package com.redsocial.api.mapper;

import com.redsocial.api.domain.Comment;
import com.redsocial.api.domain.Conversation;
import com.redsocial.api.domain.Message;
import com.redsocial.api.domain.Notification;
import com.redsocial.api.domain.Post;
import com.redsocial.api.domain.Project;
import com.redsocial.api.domain.User;
import com.redsocial.common.dto.CommentDto;
import com.redsocial.common.dto.ConversationDto;
import com.redsocial.common.dto.MessageDto;
import com.redsocial.common.dto.NotificationDto;
import com.redsocial.common.dto.PostDto;
import com.redsocial.common.dto.ProjectDto;
import com.redsocial.common.dto.UserDto;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class DtoMapper {

    public UserDto toUserDto(User user, boolean following, long followersCount,
                             long followingCount, long postsCount, boolean includeEmail) {
        return new UserDto(
                user.getId(),
                user.getUsername(),
                includeEmail ? user.getEmail() : null,
                user.getDisplayName(),
                user.getBio(),
                user.getAvatarUrl(),
                user.getCoverUrl(),
                following,
                followersCount,
                followingCount,
                postsCount,
                user.getCreatedAt()
        );
    }

    public PostDto toPostDto(Post post, long likeCount, long commentCount, boolean likedByMe) {
        User author = post.getAuthor();
        return new PostDto(
                post.getId(),
                author.getId(),
                author.getUsername(),
                author.getDisplayName(),
                author.getAvatarUrl(),
                post.getContent(),
                post.getMediaUrl(),
                post.getCreatedAt(),
                post.getUpdatedAt(),
                likeCount,
                commentCount,
                likedByMe
        );
    }

    public CommentDto toCommentDto(Comment comment) {
        User author = comment.getAuthor();
        return new CommentDto(
                comment.getId(),
                comment.getPost().getId(),
                author.getId(),
                author.getUsername(),
                author.getDisplayName(),
                author.getAvatarUrl(),
                comment.getContent(),
                comment.getCreatedAt()
        );
    }

    public MessageDto toMessageDto(Message message) {
        User sender = message.getSender();
        return new MessageDto(
                message.getId(),
                message.getConversation().getId(),
                sender.getId(),
                sender.getUsername(),
                sender.getDisplayName(),
                message.getContent(),
                message.getCreatedAt()
        );
    }

    public ConversationDto toConversationDto(Conversation conversation, User otherUser,
                                             String lastMessage, Instant lastMessageAt, long unreadCount) {
        UserDto other = toUserDto(otherUser, false, 0, 0, 0, false);
        return new ConversationDto(
                conversation.getId(),
                other,
                lastMessage,
                lastMessageAt,
                unreadCount
        );
    }

    public ProjectDto toProjectDto(Project project, long donationCount) {
        User creator = project.getCreator();
        return new ProjectDto(
                project.getId(),
                project.getKind(),
                project.getTitle(),
                project.getDescription(),
                creator.getId(),
                creator.getUsername(),
                creator.getDisplayName(),
                creator.getAvatarUrl(),
                project.getEventDate(),
                project.getLocation(),
                project.getTargetAmount(),
                project.getRaisedAmount(),
                project.getStatus(),
                donationCount,
                project.getCreatedAt()
        );
    }

    public NotificationDto toNotificationDto(Notification notification) {
        User actor = notification.getActor();
        return new NotificationDto(
                notification.getId(),
                notification.getType(),
                actor.getId(),
                actor.getUsername(),
                actor.getDisplayName(),
                actor.getAvatarUrl(),
                notification.getTargetId(),
                notification.getText(),
                notification.isRead(),
                notification.getCreatedAt()
        );
    }
}
