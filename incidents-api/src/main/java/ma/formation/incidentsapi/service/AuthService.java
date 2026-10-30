package ma.formation.incidentsapi.service;

import ma.formation.incidentsapi.dto.AuthResponse;
import ma.formation.incidentsapi.dto.LoginRequest;
import ma.formation.incidentsapi.dto.RegisterRequest;
import ma.formation.incidentsapi.dto.ProfileResponse;
import ma.formation.incidentsapi.exception.ConflictException;
import ma.formation.incidentsapi.exception.UnauthorizedException;
import ma.formation.incidentsapi.model.User;
import ma.formation.incidentsapi.model.UserRole;
import ma.formation.incidentsapi.repository.UserRepository;
import ma.formation.incidentsapi.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private static final String GENERIC_LOGIN_ERROR = "Email ou mot de passe incorrect";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final DtoMapper mapper;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       JwtService jwtService, RefreshTokenService refreshTokenService, DtoMapper mapper) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.mapper = mapper;
    }

    @Transactional
    public ProfileResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("Email déjà utilisé");
        }
        User user = new User();
        user.setFullName(request.fullName().trim());
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.password()));
        // Le rôle fourni par le client est volontairement ignoré : toute inscription est EMPLOYEE.
        user.setRole(UserRole.EMPLOYEE);
        user.setEnabled(true);
        return mapper.toProfile(userRepository.save(user));
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmailIgnoreCase(normalizeEmail(request.email())).orElse(null);
        if (user == null || !user.isEnabled() || !passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new UnauthorizedException(GENERIC_LOGIN_ERROR);
        }
        RefreshTokenService.IssuedTokens tokens = refreshTokenService.issue(user);
        return new AuthResponse(tokens.accessToken(), user.getRole().name(), user.getFullName(), tokens.refreshToken());
    }

    @Transactional
    public AuthResponse refresh(String refreshToken) {
        RefreshTokenService.IssuedTokens tokens = refreshTokenService.rotate(refreshToken);
        User user = userRepository.findByEmailIgnoreCase(jwtService.extractEmail(tokens.accessToken()))
                .filter(User::isEnabled)
                .orElseThrow(() -> new UnauthorizedException("Refresh token invalide"));
        return new AuthResponse(tokens.accessToken(), user.getRole().name(), user.getFullName(), tokens.refreshToken());
    }

    @Transactional
    public void logout(String refreshToken) {
        refreshTokenService.revoke(refreshToken);
    }

    public String normalizeEmail(String email) {
        return email.trim().toLowerCase(java.util.Locale.ROOT);
    }
}
