package co.uniquindio.crackscolombia.cracks_colombia_api.notification.controller;

import co.uniquindio.crackscolombia.cracks_colombia_api.notification.dto.response.NotificationResponse;
import co.uniquindio.crackscolombia.cracks_colombia_api.notification.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping("/notifications")
    public List<NotificationResponse> list(@RequestParam(defaultValue = "false") boolean unreadOnly,
                                           @AuthenticationPrincipal String authenticatedEmail) {
        return notificationService.listOwn(authenticatedEmail, unreadOnly);
    }

    @PatchMapping("/notifications/{notificationId}/read")
    public ResponseEntity<Void> markRead(@PathVariable UUID notificationId,
                                         @AuthenticationPrincipal String authenticatedEmail) {
        notificationService.markRead(notificationId, authenticatedEmail);
        return ResponseEntity.ok().build();
    }
}
