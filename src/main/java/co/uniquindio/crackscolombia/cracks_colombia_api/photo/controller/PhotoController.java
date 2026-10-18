package co.uniquindio.crackscolombia.cracks_colombia_api.photo.controller;

import co.uniquindio.crackscolombia.cracks_colombia_api.photo.dto.response.AthletePhotoResponse;
import co.uniquindio.crackscolombia.cracks_colombia_api.photo.service.PhotoService;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
public class PhotoController {

    private final PhotoService photoService;

    public PhotoController(PhotoService photoService) {
        this.photoService = photoService;
    }

    @PutMapping(value = "/athletes/{athleteId}/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AthletePhotoResponse> upload(@PathVariable UUID athleteId,
                                                       @RequestPart("file") MultipartFile file,
                                                       @AuthenticationPrincipal String authenticatedEmail) {
        return ResponseEntity.status(HttpStatus.OK)
                .body(photoService.upload(athleteId, authenticatedEmail, file));
    }

    @GetMapping("/athletes/{athleteId}/photo")
    public ResponseEntity<Resource> content(@PathVariable UUID athleteId, Authentication authentication) {
        PhotoService.PhotoContent content = photoService.loadContent(athleteId, authentication);
        String extension = content.contentType().equals("image/png") ? "png" : "jpg";
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(content.contentType()))
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"profile-photo." + extension + "\"")
                .header("X-Content-Type-Options", "nosniff")
                .body(content.resource());
    }
}
