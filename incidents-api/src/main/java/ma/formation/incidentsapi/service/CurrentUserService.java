package ma.formation.incidentsapi.service;

import ma.formation.incidentsapi.exception.NotFoundException;
import ma.formation.incidentsapi.exception.UnauthorizedException;
import ma.formation.incidentsapi.model.User;
import ma.formation.incidentsapi.repository.UserRepository;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class CurrentUserService {

    private final UserRepository userRepository;

    public CurrentUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User getRequiredUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            throw new UnauthorizedException("Authentification requise");
        }
        return userRepository.findByEmailIgnoreCase(authentication.getName())
                .filter(User::isEnabled)
                .orElseThrow(() -> new NotFoundException("Utilisateur introuvable"));
    }
}
