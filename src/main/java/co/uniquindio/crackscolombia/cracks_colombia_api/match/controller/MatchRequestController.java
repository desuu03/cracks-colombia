package co.uniquindio.crackscolombia.cracks_colombia_api.match.controller;

import co.uniquindio.crackscolombia.cracks_colombia_api.match.dto.request.MatchRequestCreateRequest;
import co.uniquindio.crackscolombia.cracks_colombia_api.match.dto.request.MatchRequestRespondRequest;
import co.uniquindio.crackscolombia.cracks_colombia_api.match.dto.response.MatchRequestResponse;
import co.uniquindio.crackscolombia.cracks_colombia_api.match.entity.MatchRequestStatus;
import co.uniquindio.crackscolombia.cracks_colombia_api.match.service.MatchRequestService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
public class MatchRequestController {

    private final MatchRequestService matchRequestService;

    public MatchRequestController(MatchRequestService matchRequestService) {
        this.matchRequestService = matchRequestService;
    }

    @PostMapping("/athletes/{athleteId}/interest")
    public ResponseEntity<Void> markInterest(@PathVariable UUID athleteId,
                                             @AuthenticationPrincipal String authenticatedEmail) {
        matchRequestService.markInterest(athleteId, authenticatedEmail);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/requests")
    public List<MatchRequestResponse> list(@RequestParam(required = false) MatchRequestStatus status,
                                           @AuthenticationPrincipal String authenticatedEmail) {
        return matchRequestService.listOwn(authenticatedEmail, status);
    }

    @GetMapping("/prueba")
    public String prueba() {
        return "Verificando el flujo de CI CD de nuestro proyecto";
    }



    @PostMapping("/requests")
    public ResponseEntity<MatchRequestResponse> create(@Valid @RequestBody MatchRequestCreateRequest request,
                                                       @AuthenticationPrincipal String authenticatedEmail) {
        return ResponseEntity.status(HttpStatus.CREATED).body(matchRequestService.create(request, authenticatedEmail));
    }

    @PatchMapping("/requests/{requestId}/respond")
    public MatchRequestResponse respond(@PathVariable UUID requestId,
                                        @Valid @RequestBody MatchRequestRespondRequest body,
                                        @AuthenticationPrincipal String authenticatedEmail) {
        return matchRequestService.respond(requestId, body, authenticatedEmail);
    }
}
