package com.redsocial.api.service;

import com.redsocial.api.domain.User;
import com.redsocial.api.exception.BadRequestException;
import com.redsocial.api.exception.ConflictException;
import com.redsocial.api.mapper.DtoMapper;
import com.redsocial.api.repository.FollowRepository;
import com.redsocial.api.repository.PostRepository;
import com.redsocial.api.repository.UserRepository;
import com.redsocial.api.security.JwtService;
import com.redsocial.api.security.UserPrincipal;
import com.redsocial.common.dto.AuthResponse;
import com.redsocial.common.dto.LoginRequest;
import com.redsocial.common.dto.RegisterRequest;
import com.redsocial.common.dto.TutorType;
import com.redsocial.common.dto.UserDto;
import com.redsocial.common.dto.UserRole;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.regex.Pattern;

@Service
public class AuthService {

    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]{3,30}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final Pattern UNET_EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9._%+-]+@unet\\.edu\\.ve$");
    private static final Pattern CARNET_PATTERN = Pattern.compile("^\\d{6,10}$");

    private final UserRepository userRepository;
    private final FollowRepository followRepository;
    private final PostRepository postRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final DtoMapper mapper;

    public AuthService(UserRepository userRepository,
                       FollowRepository followRepository,
                       PostRepository postRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       AuthenticationManager authenticationManager,
                       DtoMapper mapper) {
        this.userRepository = userRepository;
        this.followRepository = followRepository;
        this.postRepository = postRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        this.mapper = mapper;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String username = trim(request.username());
        String email = trim(request.email());
        String password = request.password();

        if (username == null || !USERNAME_PATTERN.matcher(username).matches()) {
            throw new BadRequestException("El usuario debe tener entre 3 y 30 caracteres (letras, números o guion bajo)");
        }
        if (email == null || !EMAIL_PATTERN.matcher(email).matches()) {
            throw new BadRequestException("El email no es válido");
        }
        if (!UNET_EMAIL_PATTERN.matcher(email).matches()) {
            throw new BadRequestException("Usá tu correo institucional @unet.edu.ve");
        }
        String carnet = trim(request.carnet());
        if (carnet != null && !CARNET_PATTERN.matcher(carnet).matches()) {
            throw new BadRequestException("El carnet debe tener entre 6 y 10 dígitos");
        }
        if (password == null || password.length() < 8) {
            throw new BadRequestException("La contraseña debe tener al menos 8 caracteres");
        }
        if (userRepository.existsByUsernameIgnoreCase(username)) {
            throw new ConflictException("El nombre de usuario ya está en uso");
        }
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("El email ya está registrado");
        }

        String displayName = trim(request.displayName());
        UserRole role = request.role();
        String career = trim(request.career());
        String subjectOfInterest = trim(request.subjectOfInterest());
        TutorType tutorType = request.tutorType();

        if (role == UserRole.STUDENT) {
            if (career == null) {
                throw new BadRequestException("La carrera es obligatoria para estudiantes");
            }
            if (subjectOfInterest == null) {
                throw new BadRequestException("La materia de interés es obligatoria para estudiantes");
            }
        } else if (role == UserRole.TUTOR) {
            if (tutorType == null) {
                tutorType = TutorType.UNET;
            }
        }

        User user = User.builder()
                .username(username)
                .email(email)
                .carnet(carnet)
                .password(passwordEncoder.encode(password))
                .displayName(displayName == null ? username : displayName)
                .role(role == null ? "USER" : role.name())
                .career(career)
                .subjectOfInterest(subjectOfInterest)
                .tutorType(tutorType)
                .enabled(true)
                .build();
        userRepository.save(user);
        return buildAuthResponse(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String identifier = trim(request.usernameOrEmail());
        if (identifier == null || request.password() == null) {
            throw new BadRequestException("Usuario y contraseña son obligatorios");
        }
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(identifier, request.password()));
        User user = userRepository
                .findByUsernameIgnoreCaseOrEmailIgnoreCase(identifier, identifier)
                .orElseThrow(() -> new BadRequestException("Credenciales inválidas"));
        return buildAuthResponse(user);
    }

    private AuthResponse buildAuthResponse(User user) {
        UserPrincipal principal = UserPrincipal.from(user);
        String token = jwtService.generateToken(principal);
        UserDto dto = mapper.toUserDto(
                user,
                false,
                followRepository.countByFollowing_Id(user.getId()),
                followRepository.countByFollower_Id(user.getId()),
                postRepository.countByAuthor_Id(user.getId()),
                true);
        return new AuthResponse(token, "Bearer", jwtService.expirationSeconds(), dto);
    }

    private String trim(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
