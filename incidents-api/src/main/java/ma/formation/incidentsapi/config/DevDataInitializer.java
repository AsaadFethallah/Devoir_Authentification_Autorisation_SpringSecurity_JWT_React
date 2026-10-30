package ma.formation.incidentsapi.config;

import ma.formation.incidentsapi.model.Incident;
import ma.formation.incidentsapi.model.IncidentCategory;
import ma.formation.incidentsapi.model.IncidentHistory;
import ma.formation.incidentsapi.model.IncidentPriority;
import ma.formation.incidentsapi.model.IncidentStatus;
import ma.formation.incidentsapi.model.User;
import ma.formation.incidentsapi.model.UserRole;
import ma.formation.incidentsapi.repository.IncidentHistoryRepository;
import ma.formation.incidentsapi.repository.IncidentRepository;
import ma.formation.incidentsapi.repository.UserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

@Configuration
@Profile("dev")
public class DevDataInitializer {

    @Bean
    CommandLineRunner seedDevelopmentData(UserRepository userRepository,
                                           IncidentRepository incidentRepository,
                                           IncidentHistoryRepository historyRepository,
                                           PasswordEncoder passwordEncoder) {
        return args -> initialize(userRepository, incidentRepository, historyRepository, passwordEncoder);
    }

    @Transactional
    void initialize(UserRepository userRepository, IncidentRepository incidentRepository,
                    IncidentHistoryRepository historyRepository, PasswordEncoder passwordEncoder) {
        User admin = ensureUser(userRepository, passwordEncoder, "Admin Démo", "admin@example.com", "Admin123!", UserRole.ADMIN);
        User tech1 = ensureUser(userRepository, passwordEncoder, "Technicien Alpha", "tech1@example.com", "Tech123!", UserRole.TECHNICIAN);
        ensureUser(userRepository, passwordEncoder, "Technicien Beta", "tech2@example.com", "Tech123!", UserRole.TECHNICIAN);
        User employee1 = ensureUser(userRepository, passwordEncoder, "Employé Un", "employee1@example.com", "Employee123!", UserRole.EMPLOYEE);
        User employee2 = ensureUser(userRepository, passwordEncoder, "Employé Deux", "employee2@example.com", "Employee123!", UserRole.EMPLOYEE);

        if (incidentRepository.count() == 0) {
            Incident first = createIncident(incidentRepository, employee1, "Poste suspect après phishing",
                    "Un lien reçu par email a été ouvert par erreur.", IncidentCategory.PHISHING,
                    IncidentPriority.HIGH, IncidentStatus.ASSIGNED, tech1);
            addHistory(historyRepository, first, employee1, "Incident créé");
            addHistory(historyRepository, first, admin, "Affecté à " + tech1.getFullName());

            Incident second = createIncident(incidentRepository, employee2, "Panne réseau du bureau",
                    "Le réseau est indisponible depuis ce matin.", IncidentCategory.NETWORK,
                    IncidentPriority.MEDIUM, IncidentStatus.OPEN, null);
            addHistory(historyRepository, second, employee2, "Incident créé");
        }
    }

    private User ensureUser(UserRepository repository, PasswordEncoder encoder, String fullName,
                            String email, String password, UserRole role) {
        return repository.findByEmailIgnoreCase(email).orElseGet(() -> {
            User user = new User();
            user.setFullName(fullName);
            user.setEmail(email);
            user.setPassword(encoder.encode(password));
            user.setRole(role);
            user.setEnabled(true);
            return repository.save(user);
        });
    }

    private Incident createIncident(IncidentRepository repository, User creator, String title,
                                    String description, IncidentCategory category,
                                    IncidentPriority priority, IncidentStatus status, User assignee) {
        Incident incident = new Incident();
        incident.setCreatedBy(creator);
        incident.setTitle(title);
        incident.setDescription(description);
        incident.setCategory(category);
        incident.setPriority(priority);
        incident.setStatus(status);
        incident.setAssignedTo(assignee);
        return repository.save(incident);
    }

    private void addHistory(IncidentHistoryRepository repository, Incident incident, User author, String action) {
        IncidentHistory history = new IncidentHistory();
        history.setIncident(incident);
        history.setAuthor(author);
        history.setActionText(action);
        repository.save(history);
    }
}
