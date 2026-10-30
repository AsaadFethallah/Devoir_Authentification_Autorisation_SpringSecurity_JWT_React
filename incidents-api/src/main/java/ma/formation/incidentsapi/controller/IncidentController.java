package ma.formation.incidentsapi.controller;

import jakarta.validation.Valid;
import ma.formation.incidentsapi.dto.CommentRequest;
import ma.formation.incidentsapi.dto.CommentResponse;
import ma.formation.incidentsapi.dto.HistoryResponse;
import ma.formation.incidentsapi.dto.IncidentRequest;
import ma.formation.incidentsapi.dto.IncidentResponse;
import ma.formation.incidentsapi.dto.StatusUpdateRequest;
import ma.formation.incidentsapi.service.IncidentService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/incidents")
public class IncidentController {

    private final IncidentService incidentService;

    public IncidentController(IncidentService incidentService) {
        this.incidentService = incidentService;
    }

    @PostMapping
    @PreAuthorize("hasRole('EMPLOYEE')")
    public IncidentResponse create(@Valid @RequestBody IncidentRequest request) {
        return incidentService.create(request);
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public List<IncidentResponse> myIncidents() {
        return incidentService.myIncidents();
    }

    @GetMapping("/assigned")
    @PreAuthorize("hasRole('TECHNICIAN')")
    public List<IncidentResponse> assignedIncidents() {
        return incidentService.assignedIncidents();
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<IncidentResponse> allIncidents() {
        return incidentService.allIncidents();
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public IncidentResponse getById(@PathVariable Long id) {
        return incidentService.getById(id);
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('TECHNICIAN', 'ADMIN')")
    public IncidentResponse updateStatus(@PathVariable Long id, @Valid @RequestBody StatusUpdateRequest request) {
        return incidentService.updateStatus(id, request);
    }

    @PutMapping("/{id}/assign/{technicianId}")
    @PreAuthorize("hasRole('ADMIN')")
    public IncidentResponse assign(@PathVariable Long id, @PathVariable Long technicianId) {
        return incidentService.assign(id, technicianId);
    }

    @PostMapping("/{id}/comments")
    @PreAuthorize("hasAnyRole('TECHNICIAN', 'ADMIN')")
    public CommentResponse addComment(@PathVariable Long id, @Valid @RequestBody CommentRequest request) {
        return incidentService.addComment(id, request);
    }

    @GetMapping("/{id}/comments")
    @PreAuthorize("isAuthenticated()")
    public List<CommentResponse> comments(@PathVariable Long id) {
        return incidentService.comments(id);
    }

    @GetMapping("/{id}/history")
    @PreAuthorize("isAuthenticated()")
    public List<HistoryResponse> history(@PathVariable Long id) {
        return incidentService.history(id);
    }
}
