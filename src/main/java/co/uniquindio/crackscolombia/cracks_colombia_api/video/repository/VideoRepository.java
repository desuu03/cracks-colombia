package co.uniquindio.crackscolombia.cracks_colombia_api.video.repository;

import co.uniquindio.crackscolombia.cracks_colombia_api.video.entity.Video;
import co.uniquindio.crackscolombia.cracks_colombia_api.video.entity.VideoStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface VideoRepository extends JpaRepository<Video, UUID> {

    List<Video> findAllByAthlete_IdOrderByUploadedAtDesc(UUID athleteId);

    List<Video> findAllByAthlete_IdAndStatusOrderByUploadedAtDesc(UUID athleteId, VideoStatus status);
}
