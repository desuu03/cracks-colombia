package co.uniquindio.crackscolombia.cracks_colombia_api.video.controller;

import co.uniquindio.crackscolombia.cracks_colombia_api.video.dto.response.VideoResponse;
import co.uniquindio.crackscolombia.cracks_colombia_api.video.service.VideoService;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping
public class VideoController {

    private final VideoService videoService;

    public VideoController(VideoService videoService) {
        this.videoService = videoService;
    }

    @PostMapping(value = "/athletes/{athleteId}/videos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<VideoResponse> create(@PathVariable UUID athleteId,
                                                @RequestPart("file") MultipartFile file,
                                                @AuthenticationPrincipal String authenticatedEmail) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(videoService.create(athleteId, authenticatedEmail, file));
    }

    @GetMapping("/users/me/videos")
    public List<VideoResponse> listOwn(@AuthenticationPrincipal String authenticatedEmail) {
        return videoService.listOwn(authenticatedEmail);
    }

    @GetMapping("/athletes/{athleteId}/videos")
    public List<VideoResponse> listApproved(@PathVariable UUID athleteId) {
        return videoService.listApproved(athleteId);
    }

    @GetMapping(value = "/videos/{videoId}/content", produces = "video/mp4")
    public ResponseEntity<Resource> content(@PathVariable UUID videoId, Authentication authentication) {
        Resource resource = videoService.loadContent(videoId, authentication);
        return ResponseEntity.ok()
                .contentType(MediaType.valueOf("video/mp4"))
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"video.mp4\"")
                .header("X-Content-Type-Options", "nosniff")
                .body(resource);
    }

    @DeleteMapping("/videos/{videoId}")
    public ResponseEntity<Void> delete(@PathVariable UUID videoId,
                                       @AuthenticationPrincipal String authenticatedEmail) {
        videoService.delete(videoId, authenticatedEmail);
        return ResponseEntity.noContent().build();
    }
}
