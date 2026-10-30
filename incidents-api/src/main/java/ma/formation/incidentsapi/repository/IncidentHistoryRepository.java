package ma.formation.incidentsapi.repository;

import ma.formation.incidentsapi.model.IncidentHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IncidentHistoryRepository extends JpaRepository<IncidentHistory, Long> {
    List<IncidentHistory> findByIncidentIdOrderByEventDateAsc(Long incidentId);
}
