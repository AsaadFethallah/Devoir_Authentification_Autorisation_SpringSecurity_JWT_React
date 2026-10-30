package ma.formation.incidentsapi.service;

import ma.formation.incidentsapi.dto.AdminUserResponse;
import ma.formation.incidentsapi.dto.StatisticsResponse;
import ma.formation.incidentsapi.exception.BadRequestException;
import ma.formation.incidentsapi.exception.NotFoundException;
import ma.formation.incidentsapi.model.IncidentStatus;
import ma.formation.incidentsapi.model.User;
import ma.formation.incidentsapi.model.UserRole;
import ma.formation.incidentsapi.repository.IncidentRepository;
import ma.formation.incidentsapi.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AdminService {

    private final UserRepository userRepository;
    private final IncidentRepository incidentRepository;
    private final CurrentUserService currentUserService;
    private final DtoMapper mapper;

    public AdminService(UserRepository userRepository, IncidentRepository incidentRepository,
                        CurrentUserService currentUserService, DtoMapper mapper) {
        this.userRepository = userRepository;
        this.incidentRepository = incidentRepository;
        this.currentUserService = currentUserService;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<AdminUserResponse> listUsers() {
        return userRepository.findAllByOrderByCreatedAtDesc().stream().map(mapper::toAdminUser).toList();
    }

    @Transactional
    public AdminUserResponse setEnabled(Long id, boolean enabled) {
        User current = currentUserService.getRequiredUser();
        User target = userRepository.findById(id).orElseThrow(() -> new NotFoundException("Utilisateur introuvable"));
        if (!enabled && current.getId().equals(target.getId())) {
            throw new BadRequestException("Un administrateur ne peut pas se désactiver lui-même");
        }
        target.setEnabled(enabled);
        return mapper.toAdminUser(userRepository.save(target));
    }

    @Transactional(readOnly = true)
    public StatisticsResponse statistics() {
        return new StatisticsResponse(
                userRepository.count(),
                userRepository.countByRole(UserRole.EMPLOYEE),
                userRepository.countByRole(UserRole.TECHNICIAN),
                incidentRepository.count(),
                incidentRepository.countByStatus(IncidentStatus.OPEN),
                incidentRepository.countByStatus(IncidentStatus.IN_PROGRESS),
                incidentRepository.countByStatus(IncidentStatus.CLOSED));
    }
}
