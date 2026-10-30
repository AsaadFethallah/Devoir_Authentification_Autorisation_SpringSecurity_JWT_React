package ma.formation.incidentsapi.service;

import ma.formation.incidentsapi.dto.PasswordChangeRequest;
import ma.formation.incidentsapi.dto.ProfileResponse;
import ma.formation.incidentsapi.dto.ProfileUpdateRequest;
import ma.formation.incidentsapi.dto.ProfileUpdateResponse;
import ma.formation.incidentsapi.exception.BadRequestException;
import ma.formation.incidentsapi.exception.ConflictException;
import ma.formation.incidentsapi.model.User;
import ma.formation.incidentsapi.repository.UserRepository;
import ma.formation.incidentsapi.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProfileService {

    private final CurrentUserService currentUserService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final DtoMapper mapper;
    private final AuthService authService;

    public ProfileService(CurrentUserService currentUserService, UserRepository userRepository,
                          PasswordEncoder passwordEncoder, JwtService jwtService,
                          DtoMapper mapper, AuthService authService) {
        this.currentUserService = currentUserService;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.mapper = mapper;
        this.authService = authService;
    }

    @Transactional(readOnly = true)
    public ProfileResponse getProfile() {
        return mapper.toProfile(currentUserService.getRequiredUser());
    }

    @Transactional
    public ProfileUpdateResponse updateProfile(ProfileUpdateRequest request) {
        User user = currentUserService.getRequiredUser();
        String newEmail = authService.normalizeEmail(request.email());
        boolean emailChanged = !user.getEmail().equalsIgnoreCase(newEmail);
        if (emailChanged && userRepository.existsByEmailIgnoreCase(newEmail)) {
            throw new ConflictException("Email déjà utilisé");
        }
        user.setFullName(request.fullName().trim());
        user.setEmail(newEmail);
        User saved = userRepository.save(user);
        // Le subject du JWT est l'email : un changement d'email exige donc un nouveau token.
        String newToken = emailChanged ? jwtService.generateToken(saved) : null;
        return new ProfileUpdateResponse(mapper.toProfile(saved), newToken);
    }

    @Transactional
    public void changePassword(PasswordChangeRequest request) {
        User user = currentUserService.getRequiredUser();
        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new BadRequestException("L'ancien mot de passe est incorrect");
        }
        if (!request.newPassword().equals(request.confirmation())) {
            throw new BadRequestException("La confirmation du mot de passe est incorrecte");
        }
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
    }
}
