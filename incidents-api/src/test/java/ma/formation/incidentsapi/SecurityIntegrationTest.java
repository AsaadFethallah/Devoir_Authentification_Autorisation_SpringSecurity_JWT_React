package ma.formation.incidentsapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import ma.formation.incidentsapi.model.Incident;
import ma.formation.incidentsapi.model.IncidentCategory;
import ma.formation.incidentsapi.model.IncidentComment;
import ma.formation.incidentsapi.model.IncidentHistory;
import ma.formation.incidentsapi.model.IncidentPriority;
import ma.formation.incidentsapi.model.IncidentStatus;
import ma.formation.incidentsapi.model.User;
import ma.formation.incidentsapi.model.UserRole;
import ma.formation.incidentsapi.repository.IncidentCommentRepository;
import ma.formation.incidentsapi.repository.IncidentHistoryRepository;
import ma.formation.incidentsapi.repository.IncidentRepository;
import ma.formation.incidentsapi.repository.RefreshTokenRepository;
import ma.formation.incidentsapi.repository.UserRepository;
import ma.formation.incidentsapi.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityIntegrationTest {

    private static final String TEST_SECRET = "test-secret-for-junit-only-32-bytes-minimum-2026";

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired JwtService jwtService;
    @Autowired UserRepository userRepository;
    @Autowired IncidentRepository incidentRepository;
    @Autowired IncidentHistoryRepository historyRepository;
    @Autowired IncidentCommentRepository commentRepository;
    @Autowired RefreshTokenRepository refreshTokenRepository;

    private User admin;
    private User employee1;
    private User employee2;
    private User technician1;
    private User technician2;

    @BeforeEach
    void setUp() {
        commentRepository.deleteAll();
        historyRepository.deleteAll();
        incidentRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();

        admin = user("Admin Test", "admin@test.local", "Admin123!", UserRole.ADMIN);
        employee1 = user("Employee One", "employee1@test.local", "Employee123!", UserRole.EMPLOYEE);
        employee2 = user("Employee Two", "employee2@test.local", "Employee123!", UserRole.EMPLOYEE);
        technician1 = user("Technician One", "tech1@test.local", "Tech123!", UserRole.TECHNICIAN);
        technician2 = user("Technician Two", "tech2@test.local", "Tech123!", UserRole.TECHNICIAN);
    }

    @Test
    void protectedRouteWithoutJwtReturns401() throws Exception {
        mockMvc.perform(get("/api/profile"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void invalidJwtReturns401() throws Exception {
        mockMvc.perform(get("/api/profile").header("Authorization", "Bearer invalid.jwt.value"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void expiredJwtReturns401() throws Exception {
        String expired = Jwts.builder()
                .subject(employee1.getEmail())
                .issuedAt(new Date(System.currentTimeMillis() - 20_000))
                .expiration(new Date(System.currentTimeMillis() - 10_000))
                .claim("role", "EMPLOYEE")
                .signWith(Keys.hmacShaKeyFor(TEST_SECRET.getBytes(StandardCharsets.UTF_8)), Jwts.SIG.HS256)
                .compact();

        mockMvc.perform(get("/api/profile").header("Authorization", "Bearer " + expired))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void employeeCannotAccessAdminRoute() throws Exception {
        mockMvc.perform(get("/api/admin/users").header("Authorization", bearer(employee1)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void employeeCannotReadAnotherEmployeesIncident() throws Exception {
        Incident incident = incident(employee1, null, IncidentStatus.OPEN);

        mockMvc.perform(get("/api/incidents/{id}", incident.getId())
                        .header("Authorization", bearer(employee2)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void technicianCannotModifyUnassignedIncident() throws Exception {
        Incident incident = incident(employee1, technician2, IncidentStatus.ASSIGNED);

        mockMvc.perform(put("/api/incidents/{id}/status", incident.getId())
                        .header("Authorization", bearer(technician1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("status", "IN_PROGRESS"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void disabledUserCannotLoginOrUseExistingToken() throws Exception {
        String tokenBeforeDisable = bearer(employee1);
        employee1.setEnabled(false);
        userRepository.save(employee1);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", employee1.getEmail(), "password", "Employee123!"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Email ou mot de passe incorrect"));

        mockMvc.perform(get("/api/profile").header("Authorization", tokenBeforeDisable))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void duplicateEmailReturns409() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "fullName", "Duplicate",
                                "email", employee1.getEmail(),
                                "password", "NewPassword123!"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void invalidIncidentReturns400WithFieldErrors() throws Exception {
        mockMvc.perform(post("/api/incidents")
                        .header("Authorization", bearer(employee1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors[*].field", hasItem("title")))
                .andExpect(jsonPath("$.errors[*].field", hasItem("description")))
                .andExpect(jsonPath("$.errors[*].field", hasItem("category")))
                .andExpect(jsonPath("$.errors[*].field", hasItem("priority")));
    }

    @Test
    void closedIncidentCannotChangeStatus() throws Exception {
        Incident incident = incident(employee1, technician1, IncidentStatus.CLOSED);

        mockMvc.perform(put("/api/incidents/{id}/status", incident.getId())
                        .header("Authorization", bearer(technician1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("status", "RESOLVED"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Un incident CLOSED ne peut plus être modifié"));
    }

    @Test
    void validStatusTransitionsAndReopeningAreAccepted() throws Exception {
        Incident incident = incident(employee1, technician1, IncidentStatus.ASSIGNED);
        String token = bearer(technician1);

        mockMvc.perform(put("/api/incidents/{id}/status", incident.getId())
                        .header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("status", "IN_PROGRESS"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("IN_PROGRESS"));

        mockMvc.perform(put("/api/incidents/{id}/status", incident.getId())
                        .header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("status", "RESOLVED"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("RESOLVED"));

        mockMvc.perform(put("/api/incidents/{id}/status", incident.getId())
                        .header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("status", "IN_PROGRESS"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    @Test
    void registrationAlwaysCreatesEmployeeEvenWhenAdminIsSent() throws Exception {
        String email = "forced-employee@test.local";
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "fullName", "Forced Employee",
                                "email", email,
                                "password", "NewPassword123!",
                                "role", "ADMIN"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("EMPLOYEE"));

        org.assertj.core.api.Assertions.assertThat(userRepository.findByEmailIgnoreCase(email).orElseThrow().getRole())
                .isEqualTo(UserRole.EMPLOYEE);
    }

    @Test
    void refreshRotatesTheRefreshTokenAndReturnsANewAccessToken() throws Exception {
        JsonNode login = loginResponse(employee1);
        String firstRefreshToken = login.get("refreshToken").asText();

        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("refreshToken", firstRefreshToken))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.role").value("EMPLOYEE"));

        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("refreshToken", firstRefreshToken))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logoutRevokesTheRefreshToken() throws Exception {
        String refreshToken = loginResponse(employee1).get("refreshToken").asText();

        mockMvc.perform(post("/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("refreshToken", refreshToken))))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("refreshToken", refreshToken))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void disabledUserCannotRefreshAnExistingRefreshToken() throws Exception {
        String refreshToken = loginResponse(employee1).get("refreshToken").asText();
        employee1.setEnabled(false);
        userRepository.save(employee1);

        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("refreshToken", refreshToken))))
                .andExpect(status().isUnauthorized());
    }

    private User user(String fullName, String email, String password, UserRole role) {
        User user = new User();
        user.setFullName(fullName);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(role);
        user.setEnabled(true);
        return userRepository.save(user);
    }

    private Incident incident(User creator, User assignee, IncidentStatus status) {
        Incident incident = new Incident();
        incident.setTitle("Incident de test");
        incident.setDescription("Description de test");
        incident.setCategory(IncidentCategory.SOFTWARE);
        incident.setPriority(IncidentPriority.MEDIUM);
        incident.setStatus(status);
        incident.setCreatedBy(creator);
        incident.setAssignedTo(assignee);
        return incidentRepository.save(incident);
    }

    private String bearer(User user) {
        return "Bearer " + jwtService.generateToken(user);
    }

    private JsonNode loginResponse(User user) throws Exception {
        return objectMapper.readTree(mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", user.getEmail(), "password", passwordFor(user)))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
    }

    private String passwordFor(User user) {
        return switch (user.getRole()) {
            case ADMIN -> "Admin123!";
            case TECHNICIAN -> "Tech123!";
            case EMPLOYEE -> "Employee123!";
        };
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }
}
