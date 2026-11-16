package co.uniquindio.crackscolombia.cracks_colombia_api.moderation.controller;

import co.uniquindio.crackscolombia.cracks_colombia_api.moderation.dto.request.RejectVideoRequest;
import co.uniquindio.crackscolombia.cracks_colombia_api.moderation.dto.response.ModerationQueuePage;
import co.uniquindio.crackscolombia.cracks_colombia_api.moderation.dto.response.ModerationVideoResponse;
import co.uniquindio.crackscolombia.cracks_colombia_api.moderation.service.ModerationService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/moderation")
public class ModerationController {

    private final ModerationService moderationService;

    public ModerationController(ModerationService moderationService) {
        this.moderationService = moderationService;
    }

    @GetMapping("/queue")
    public ModerationQueuePage queue(@RequestParam(defaultValue = "0") int page,
                                     @RequestParam(defaultValue = "20") int size,
                                     @AuthenticationPrincipal String authenticatedEmail) {
        return moderationService.queue(authenticatedEmail, page, size);
    }

    @PostMapping("/{videoId}/approve")
    public ModerationVideoResponse approve(@PathVariable UUID videoId,
                                           @AuthenticationPrincipal String authenticatedEmail) {
        return moderationService.approve(videoId, authenticatedEmail);
    }

    @PostMapping("/{videoId}/reject")
    public ModerationVideoResponse reject(@PathVariable UUID videoId,
                                          @Valid @RequestBody RejectVideoRequest request,
                                          @AuthenticationPrincipal String authenticatedEmail) {
        return moderationService.reject(videoId, request.reason(), authenticatedEmail);
    }
}
