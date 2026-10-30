package ma.formation.incidentsapi.service;

import ma.formation.incidentsapi.dto.CommentRequest;
import ma.formation.incidentsapi.dto.CommentResponse;
import ma.formation.incidentsapi.dto.HistoryResponse;
import ma.formation.incidentsapi.dto.IncidentRequest;
import ma.formation.incidentsapi.dto.IncidentResponse;
import ma.formation.incidentsapi.dto.StatusUpdateRequest;
import ma.formation.incidentsapi.exception.BadRequestException;
import ma.formation.incidentsapi.exception.ForbiddenException;
import ma.formation.incidentsapi.exception.NotFoundException;
import ma.formation.incidentsapi.model.Incident;
import ma.formation.incidentsapi.model.IncidentCategory;
import ma.formation.incidentsapi.model.IncidentComment;
import ma.formation.incidentsapi.model.IncidentHistory;
import ma.formation.incidentsapi.model.IncidentStatus;
import ma.formation.incidentsapi.model.User;
import ma.formation.incidentsapi.model.UserRole;
import ma.formation.incidentsapi.repository.IncidentCommentRepository;
import ma.formation.incidentsapi.repository.IncidentHistoryRepository;
import ma.formation.incidentsapi.repository.IncidentRepository;
import ma.formation.incidentsapi.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class IncidentService {

    private final IncidentRepository incidentRepository;
    private final IncidentHistoryRepository historyRepository;
    private final IncidentCommentRepository commentRepository;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;
    private final DtoMapper mapper;

    public IncidentService(IncidentRepository incidentRepository,
                            IncidentHistoryRepository historyRepository,
                            IncidentCommentRepository commentRepository,
                            UserRepository userRepository,
                            CurrentUserService currentUserService,
                            DtoMapper mapper) {
        this.incidentRepository = incidentRepository;
        this.historyRepository = historyRepository;
        this.commentRepository = commentRepository;
        this.userRepository = userRepository;
        this.currentUserService = currentUserService;
        this.mapper = mapper;
    }

    @Transactional
    public IncidentResponse create(IncidentRequest request) {
        User author = currentUserService.getRequiredUser();
        Incident incident = new Incident();
        incident.setTitle(request.title().trim());
        incident.setDescription(request.description().trim());
        incident.setCategory(request.category());
        incident.setPriority(request.priority());
        incident.setStatus(IncidentStatus.OPEN);
        incident.setCreatedBy(author);
        Incident saved = incidentRepository.save(incident);
        addHistory(saved, author, "Incident créé");
        return mapper.toIncident(saved);
    }

    @Transactional(readOnly = true)
    public List<IncidentResponse> myIncidents() {
        User user = currentUserService.getRequiredUser();
        return incidentRepository.findByCreatedByIdOrderByCreatedAtDesc(user.getId()).stream().map(mapper::toIncident).toList();
    }

    @Transactional(readOnly = true)
    public List<IncidentResponse> assignedIncidents() {
        User user = currentUserService.getRequiredUser();
        return incidentRepository.findByAssignedToIdOrderByUpdatedAtDesc(user.getId()).stream().map(mapper::toIncident).toList();
    }

    @Transactional(readOnly = true)
    public List<IncidentResponse> allIncidents() {
        return incidentRepository.findAllByOrderByCreatedAtDesc().stream().map(mapper::toIncident).toList();
    }

    @Transactional(readOnly = true)
    public IncidentResponse getById(Long id) {
        Incident incident = getIncident(id);
        assertCanRead(incident, currentUserService.getRequiredUser());
        return mapper.toIncident(incident);
    }

    @Transactional
    public IncidentResponse updateStatus(Long id, StatusUpdateRequest request) {
        User actor = currentUserService.getRequiredUser();
        Incident incident = getIncident(id);
        if (incident.getStatus() == IncidentStatus.CLOSED) {
            throw new BadRequestException("Un incident CLOSED ne peut plus être modifié");
        }
        if (actor.getRole() == UserRole.TECHNICIAN
                && (incident.getAssignedTo() == null || !incident.getAssignedTo().getId().equals(actor.getId()))) {
            throw new ForbiddenException("Le technicien n'est pas affecté à cet incident");
        }
        IncidentStatus oldStatus = incident.getStatus();
        IncidentStatus newStatus = request.status();
        if (!isAllowedTransition(oldStatus, newStatus, actor.getRole() == UserRole.ADMIN)) {
            throw new BadRequestException("Transition de statut non autorisée : " + oldStatus + " -> " + newStatus);
        }
        incident.setStatus(newStatus);
        incidentRepository.save(incident);
        addHistory(incident, actor, oldStatus + " -> " + newStatus);
        return mapper.toIncident(incident);
    }

    @Transactional
    public IncidentResponse assign(Long id, Long technicianId) {
        User actor = currentUserService.getRequiredUser();
        if (actor.getRole() != UserRole.ADMIN) {
            throw new ForbiddenException("Seul un administrateur peut affecter un incident");
        }
        Incident incident = getIncident(id);
        if (incident.getStatus() == IncidentStatus.CLOSED) {
            throw new BadRequestException("Un incident CLOSED ne peut plus être affecté");
        }
        if (incident.getStatus() != IncidentStatus.OPEN && incident.getStatus() != IncidentStatus.ASSIGNED) {
            throw new BadRequestException("Seul un incident OPEN ou ASSIGNED peut être affecté");
        }
        User technician = userRepository.findById(technicianId)
                .orElseThrow(() -> new NotFoundException("Technicien introuvable"));
        if (technician.getRole() != UserRole.TECHNICIAN || !technician.isEnabled()) {
            throw new BadRequestException("La cible doit être un technicien actif");
        }
        incident.setAssignedTo(technician);
        incident.setStatus(IncidentStatus.ASSIGNED);
        incidentRepository.save(incident);
        addHistory(incident, actor, "Affecté à " + technician.getFullName());
        return mapper.toIncident(incident);
    }

    @Transactional
    public CommentResponse addComment(Long id, CommentRequest request) {
        User actor = currentUserService.getRequiredUser();
        Incident incident = getIncident(id);
        assertCanComment(incident, actor);
        IncidentComment comment = new IncidentComment();
        comment.setIncident(incident);
        comment.setAuthor(actor);
        comment.setContent(request.content().trim());
        return mapper.toComment(commentRepository.save(comment));
    }

    @Transactional(readOnly = true)
    public List<CommentResponse> comments(Long id) {
        Incident incident = getIncident(id);
        assertCanRead(incident, currentUserService.getRequiredUser());
        return commentRepository.findByIncidentIdOrderByCreatedAtAsc(id).stream().map(mapper::toComment).toList();
    }

    @Transactional(readOnly = true)
    public List<HistoryResponse> history(Long id) {
        Incident incident = getIncident(id);
        assertCanRead(incident, currentUserService.getRequiredUser());
        return historyRepository.findByIncidentIdOrderByEventDateAsc(id).stream().map(mapper::toHistory).toList();
    }

    private Incident getIncident(Long id) {
        return incidentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Incident introuvable"));
    }

    private void assertCanRead(Incident incident, User actor) {
        boolean isAdmin = actor.getRole() == UserRole.ADMIN;
        boolean isOwner = incident.getCreatedBy().getId().equals(actor.getId());
        boolean isAssignedTechnician = incident.getAssignedTo() != null
                && incident.getAssignedTo().getId().equals(actor.getId())
                && actor.getRole() == UserRole.TECHNICIAN;
        if (!isAdmin && !isOwner && !isAssignedTechnician) {
            throw new ForbiddenException("Vous n'avez pas accès à cet incident");
        }
    }

    private void assertCanComment(Incident incident, User actor) {
        if (actor.getRole() == UserRole.ADMIN) return;
        if (actor.getRole() != UserRole.TECHNICIAN || incident.getAssignedTo() == null
                || !incident.getAssignedTo().getId().equals(actor.getId())) {
            throw new ForbiddenException("Seul le technicien affecté ou un administrateur peut ajouter une note");
        }
    }

    private boolean isAllowedTransition(IncidentStatus from, IncidentStatus to, boolean admin) {
        if (from == to) return false;
        return switch (from) {
            case OPEN -> admin && to == IncidentStatus.IN_PROGRESS;
            case ASSIGNED -> to == IncidentStatus.IN_PROGRESS;
            case IN_PROGRESS -> to == IncidentStatus.RESOLVED;
            case RESOLVED -> to == IncidentStatus.CLOSED || to == IncidentStatus.IN_PROGRESS;
            case CLOSED -> false;
        };
    }

    private void addHistory(Incident incident, User author, String action) {
        IncidentHistory history = new IncidentHistory();
        history.setIncident(incident);
        history.setAuthor(author);
        history.setActionText(action);
        historyRepository.save(history);
    }
}
