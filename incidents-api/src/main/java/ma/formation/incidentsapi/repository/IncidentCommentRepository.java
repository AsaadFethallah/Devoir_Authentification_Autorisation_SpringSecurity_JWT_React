package ma.formation.incidentsapi.repository;

import ma.formation.incidentsapi.model.IncidentComment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IncidentCommentRepository extends JpaRepository<IncidentComment, Long> {
    List<IncidentComment> findByIncidentIdOrderByCreatedAtAsc(Long incidentId);
}
