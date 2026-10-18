package co.uniquindio.crackscolombia.cracks_colombia_api.user.controller;

import co.uniquindio.crackscolombia.cracks_colombia_api.user.dto.request.UserProfileUpdateRequest;
import co.uniquindio.crackscolombia.cracks_colombia_api.user.dto.response.AthletePublicProfileResponse;
import co.uniquindio.crackscolombia.cracks_colombia_api.user.dto.response.MyProfileResponse;
import co.uniquindio.crackscolombia.cracks_colombia_api.user.service.AthletePublicProfileService;
import co.uniquindio.crackscolombia.cracks_colombia_api.user.service.UserProfileService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
public class UserProfileController {

    private final UserProfileService userProfileService;
    private final AthletePublicProfileService athletePublicProfileService;

    public UserProfileController(UserProfileService userProfileService,
                                 AthletePublicProfileService athletePublicProfileService) {
        this.userProfileService = userProfileService;
        this.athletePublicProfileService = athletePublicProfileService;
    }

    @GetMapping("/users/me")
    public MyProfileResponse getOwn(@AuthenticationPrincipal String authenticatedEmail) {
        return userProfileService.getOwn(authenticatedEmail);
    }

    @PutMapping("/users/me")
    public MyProfileResponse updateOwn(@Valid @RequestBody UserProfileUpdateRequest request,
                                       @AuthenticationPrincipal String authenticatedEmail) {
        return userProfileService.updateOwn(authenticatedEmail, request);
    }

    @GetMapping("/athletes/{athleteId}")
    public AthletePublicProfileResponse getAthlete(@PathVariable UUID athleteId, Authentication authentication) {
        return athletePublicProfileService.get(athleteId, authentication);
    }
}
