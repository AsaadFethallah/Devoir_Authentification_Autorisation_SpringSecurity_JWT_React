package ma.formation.incidentsapi.controller;

import jakarta.validation.Valid;
import ma.formation.incidentsapi.dto.PasswordChangeRequest;
import ma.formation.incidentsapi.dto.ProfileResponse;
import ma.formation.incidentsapi.dto.ProfileUpdateRequest;
import ma.formation.incidentsapi.dto.ProfileUpdateResponse;
import ma.formation.incidentsapi.service.ProfileService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/profile")
@PreAuthorize("isAuthenticated()")
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping
    public ProfileResponse getProfile() {
        return profileService.getProfile();
    }

    @PutMapping
    public ProfileUpdateResponse updateProfile(@Valid @RequestBody ProfileUpdateRequest request) {
        return profileService.updateProfile(request);
    }

    @PutMapping("/password")
    public void changePassword(@Valid @RequestBody PasswordChangeRequest request) {
        profileService.changePassword(request);
    }
}
