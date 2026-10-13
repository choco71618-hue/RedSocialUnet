package com.redsocial.api.config;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import com.redsocial.api.domain.Comment;
import com.redsocial.api.domain.Conversation;
import com.redsocial.api.domain.ConversationParticipant;
import com.redsocial.api.domain.Donation;
import com.redsocial.api.domain.Follow;
import com.redsocial.api.domain.Message;
import com.redsocial.api.domain.Post;
import com.redsocial.api.domain.PostLike;
import com.redsocial.api.domain.Project;
import com.redsocial.api.domain.User;
import com.redsocial.api.repository.CommentRepository;
import com.redsocial.api.repository.ConversationParticipantRepository;
import com.redsocial.api.repository.ConversationRepository;
import com.redsocial.api.repository.DonationRepository;
import com.redsocial.api.repository.FollowRepository;
import com.redsocial.api.repository.MessageRepository;
import com.redsocial.api.repository.PostLikeRepository;
import com.redsocial.api.repository.PostRepository;
import com.redsocial.api.repository.ProjectRepository;
import com.redsocial.api.repository.UserRepository;
import com.redsocial.common.dto.ProjectKind;
import com.redsocial.common.dto.ProjectStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true")
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final PostLikeRepository postLikeRepository;
    private final FollowRepository followRepository;
    private final ConversationRepository conversationRepository;
    private final ConversationParticipantRepository participantRepository;
    private final ProjectRepository projectRepository;
    private final DonationRepository donationRepository;
    private final MessageRepository messageRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(UserRepository userRepository,
                      PostRepository postRepository,
                      CommentRepository commentRepository,
                      PostLikeRepository postLikeRepository,
                      FollowRepository followRepository,
                      ConversationRepository conversationRepository,
                      ConversationParticipantRepository participantRepository,
                      ProjectRepository projectRepository,
                      DonationRepository donationRepository,
                      MessageRepository messageRepository,
                      PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.postRepository = postRepository;
        this.commentRepository = commentRepository;
        this.postLikeRepository = postLikeRepository;
        this.followRepository = followRepository;
        this.conversationRepository = conversationRepository;
        this.participantRepository = participantRepository;
        this.projectRepository = projectRepository;
        this.donationRepository = donationRepository;
        this.messageRepository = messageRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) {
            log.info("Seed omitido: ya existen usuarios en la base de datos");
            return;
        }

        String encoded = passwordEncoder.encode("password123");
        User alice = userRepository.save(User.builder()
                .username("alice").email("alice@redsocial.local").password(encoded)
                .displayName("Alice").bio("Amante del código y el café.").role("USER").build());
        User bob = userRepository.save(User.builder()
                .username("bob").email("bob@redsocial.local").password(encoded)
                .displayName("Bob").bio("Backend developer.").role("USER").build());
        User carol = userRepository.save(User.builder()
                .username("carol").email("carol@redsocial.local").password(encoded)
                .displayName("Carol").bio("Diseñadora UX.").role("USER").build());

        followRepository.save(Follow.builder().follower(alice).following(bob).build());
        followRepository.save(Follow.builder().follower(alice).following(carol).build());
        followRepository.save(Follow.builder().follower(bob).following(alice).build());

        Post first = postRepository.save(Post.builder()
                .author(alice).content("¡Bienvenidos a RED-SOCIAL! Esta es la primera publicación.").build());
        Post second = postRepository.save(Post.builder()
                .author(bob).content("Construyendo el backend con Spring Boot y Vaadin en el frontend.").build());
        postRepository.save(Post.builder()
                .author(carol).content("Probando el sistema de publicaciones y el feed.").build());

        postLikeRepository.save(PostLike.builder().post(first).user(bob).build());
        postLikeRepository.save(PostLike.builder().post(first).user(carol).build());
        commentRepository.save(Comment.builder().post(first).author(bob).content("¡Gran inicio!").build());
        commentRepository.save(Comment.builder().post(first).author(carol).content("Se ve muy bien.").build());

        Conversation conversation = conversationRepository.save(Conversation.builder().build());
        participantRepository.save(ConversationParticipant.builder().conversation(conversation).user(alice).build());
        participantRepository.save(ConversationParticipant.builder().conversation(conversation).user(bob).build());
        Message hello = messageRepository.save(Message.builder()
                .conversation(conversation).sender(alice).content("Hola Bob, ¿cómo va el backend?").build());
        messageRepository.save(Message.builder()
                .conversation(conversation).sender(bob).content("¡Todo listo con Spring Boot!").build());
        conversation.setLastMessageAt(hello.getCreatedAt());
        conversationRepository.save(conversation);

        Project hackaton = projectRepository.save(Project.builder()
                .creator(alice).kind(ProjectKind.EVENT)
                .title("Hackatón de la Facultad")
                .description("48 horas de código e ideas para mejorar la vida universitaria. Equipos, mentores y premios.")
                .eventDate(LocalDateTime.now().plusDays(12).withHour(9).withMinute(0))
                .location("Aula Magna, Edificio Central")
                .build());
        Project impresion3d = projectRepository.save(Project.builder()
                .creator(bob).kind(ProjectKind.DONATION)
                .title("Espacio de Impresión 3D")
                .description("Acondicionar un rincón del campus con impresoras 3D para que estudiantes puedan prototipar.")
                .targetAmount(BigDecimal.valueOf(350000))
                .raisedAmount(BigDecimal.valueOf(120000))
                .build());
        projectRepository.save(Project.builder()
                .creator(carol).kind(ProjectKind.DONATION)
                .title("Coworking estudiantil")
                .description("Mesas, sillones y conexiones para armar un espacio de trabajo colaborativo abierto a todos.")
                .targetAmount(BigDecimal.valueOf(500000))
                .raisedAmount(BigDecimal.valueOf(200000))
                .build());
        projectRepository.save(Project.builder()
                .creator(bob).kind(ProjectKind.PROJECT)
                .title("Refacción de aulas")
                .description("Pintura, bancos y nueva iluminación en las aulas del segundo piso.")
                .status(ProjectStatus.IN_PROGRESS)
                .build());
        projectRepository.save(Project.builder()
                .creator(alice).kind(ProjectKind.PROJECT)
                .title("Wi-Fi académico gratuito")
                .description("Nueva infraestructura de red abierta en biblioteca y patios.")
                .status(ProjectStatus.PROPOSED)
                .build());

        donationRepository.save(Donation.builder().project(impresion3d).donor(alice).amount(BigDecimal.valueOf(50000)).build());
        donationRepository.save(Donation.builder().project(impresion3d).donor(carol).amount(BigDecimal.valueOf(35000)).build());

        log.info("Seed completado: usuarios de prueba alice/bob/carol con contraseña 'password123'");
    }
}
