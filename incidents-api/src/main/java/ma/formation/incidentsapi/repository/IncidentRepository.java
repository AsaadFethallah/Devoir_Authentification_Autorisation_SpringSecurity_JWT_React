package ma.formation.incidentsapi.repository;

import ma.formation.incidentsapi.model.Incident;
import ma.formation.incidentsapi.model.IncidentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IncidentRepository extends JpaRepository<Incident, Long> {
    List<Incident> findByCreatedByIdOrderByCreatedAtDesc(Long userId);
    List<Incident> findByAssignedToIdOrderByUpdatedAtDesc(Long userId);
    List<Incident> findAllByOrderByCreatedAtDesc();
    long countByStatus(IncidentStatus status);
}
