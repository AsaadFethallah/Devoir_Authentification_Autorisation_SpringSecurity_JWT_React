package ma.formation.incidentsapi.repository;

import ma.formation.incidentsapi.model.User;
import ma.formation.incidentsapi.model.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);
    List<User> findByRoleAndEnabledTrueOrderByFullNameAsc(UserRole role);
    List<User> findAllByOrderByCreatedAtDesc();
    long countByRole(UserRole role);
}
