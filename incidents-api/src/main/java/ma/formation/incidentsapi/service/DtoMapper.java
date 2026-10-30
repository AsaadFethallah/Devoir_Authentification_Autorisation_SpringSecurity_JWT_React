package ma.formation.incidentsapi.service;

import ma.formation.incidentsapi.dto.AdminUserResponse;
import ma.formation.incidentsapi.dto.CommentResponse;
import ma.formation.incidentsapi.dto.HistoryResponse;
import ma.formation.incidentsapi.dto.IncidentResponse;
import ma.formation.incidentsapi.dto.ProfileResponse;
import ma.formation.incidentsapi.dto.UserSummary;
import ma.formation.incidentsapi.model.Incident;
import ma.formation.incidentsapi.model.IncidentComment;
import ma.formation.incidentsapi.model.IncidentHistory;
import ma.formation.incidentsapi.model.User;
import org.springframework.stereotype.Component;

@Component
public class DtoMapper {

    public UserSummary toUserSummary(User user) {
        if (user == null) return null;
        return new UserSummary(user.getId(), user.getFullName(), user.getEmail(), user.getRole().name());
    }

    public ProfileResponse toProfile(User user) {
        return new ProfileResponse(user.getId(), user.getFullName(), user.getEmail(), user.getRole().name(), user.isEnabled());
    }

    public AdminUserResponse toAdminUser(User user) {
        return new AdminUserResponse(user.getId(), user.getFullName(), user.getEmail(), user.getRole().name(), user.isEnabled(), user.getCreatedAt());
    }

    public IncidentResponse toIncident(Incident incident) {
        return new IncidentResponse(
                incident.getId(), incident.getTitle(), incident.getDescription(), incident.getCategory(),
                incident.getPriority(), incident.getStatus(), incident.getCreatedAt(), incident.getUpdatedAt(),
                toUserSummary(incident.getCreatedBy()), toUserSummary(incident.getAssignedTo()));
    }

    public HistoryResponse toHistory(IncidentHistory history) {
        return new HistoryResponse(history.getId(), history.getEventDate(), history.getActionText(), toUserSummary(history.getAuthor()));
    }

    public CommentResponse toComment(IncidentComment comment) {
        return new CommentResponse(comment.getId(), comment.getContent(), comment.getCreatedAt(), toUserSummary(comment.getAuthor()));
    }
}
