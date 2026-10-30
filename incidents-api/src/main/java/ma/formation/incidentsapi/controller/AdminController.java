package ma.formation.incidentsapi.controller;

import ma.formation.incidentsapi.dto.AdminUserResponse;
import ma.formation.incidentsapi.dto.StatisticsResponse;
import ma.formation.incidentsapi.service.AdminService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/users")
    public List<AdminUserResponse> listUsers() {
        return adminService.listUsers();
    }

    @PutMapping("/users/{id}/enable")
    public AdminUserResponse enable(@PathVariable Long id) {
        return adminService.setEnabled(id, true);
    }

    @PutMapping("/users/{id}/disable")
    public AdminUserResponse disable(@PathVariable Long id) {
        return adminService.setEnabled(id, false);
    }

    @GetMapping("/statistics")
    public StatisticsResponse statistics() {
        return adminService.statistics();
    }
}
