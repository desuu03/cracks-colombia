package co.uniquindio.crackscolombia.cracks_colombia_api.video.service;

import co.uniquindio.crackscolombia.cracks_colombia_api.shared.exception.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.UUID;

@Service
public class VideoStorageService {

    private static final Logger LOGGER = LoggerFactory.getLogger(VideoStorageService.class);
    private static final String MP4_CONTENT_TYPE = "video/mp4";
    private static final String GENERIC_CONTENT_TYPE = "application/octet-stream";

    private final Path storageDirectory;
    private final long maxFileSizeBytes;
    private final int maxDurationSeconds;
    private final Mp4MetadataReader metadataReader;

    public VideoStorageService(
            @Value("${app.video.storage-dir:./data/videos}") String storageDirectory,
            @Value("${app.video.max-file-size-bytes:50000000}") long maxFileSizeBytes,
            @Value("${app.video.max-duration-seconds:60}") int maxDurationSeconds,
            Mp4MetadataReader metadataReader) {
        this.storageDirectory = Path.of(storageDirectory).toAbsolutePath().normalize();
        this.maxFileSizeBytes = maxFileSizeBytes;
        this.maxDurationSeconds = maxDurationSeconds;
        this.metadataReader = metadataReader;
    }

    public StoredVideo store(MultipartFile upload) {
        validateRequestMetadata(upload);
        UUID storageKey = UUID.randomUUID();
        Path temporaryFile = null;
        try {
            Files.createDirectories(storageDirectory);
            temporaryFile = Files.createTempFile(storageDirectory, "video-upload-", ".tmp");
            try (InputStream input = upload.getInputStream()) {
                Files.copy(input, temporaryFile, StandardCopyOption.REPLACE_EXISTING);
            }

            long sizeBytes = Files.size(temporaryFile);
            if (sizeBytes == 0) {
                throw ApiException.invalidVideo("El archivo de video esta vacio.");
            }
            if (sizeBytes > maxFileSizeBytes) {
                throw ApiException.videoTooLarge(maxFileSizeBytes);
            }

            double actualDuration = metadataReader.readDurationSeconds(temporaryFile);
            if (!Double.isFinite(actualDuration) || actualDuration <= 0
                    || actualDuration > maxDurationSeconds) {
                throw ApiException.invalidVideo(
                        "La duracion real del video debe ser mayor a 0 y no superar "
                                + maxDurationSeconds + " segundos.");
            }
            int durationSeconds = (int) Math.ceil(actualDuration);

            Path destination = pathFor(storageKey);
            try {
                Files.move(temporaryFile, destination, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporaryFile, destination);
            }
            return new StoredVideo(storageKey, sizeBytes, durationSeconds);
        } catch (ApiException exception) {
            deleteIfPresent(temporaryFile);
            throw exception;
        } catch (IOException exception) {
            deleteIfPresent(temporaryFile);
            throw new IllegalStateException("No fue posible almacenar el archivo de video.", exception);
        }
    }

    public Resource load(UUID storageKey) {
        Path file = pathFor(storageKey);
        if (!Files.isRegularFile(file)) {
            throw ApiException.notFound("El archivo del video no esta disponible.");
        }
        return new FileSystemResource(file);
    }

    public void deleteQuietly(UUID storageKey) {
        try {
            Files.deleteIfExists(pathFor(storageKey));
        } catch (IOException exception) {
            LOGGER.warn("Could not delete stored video {}", storageKey, exception);
        }
    }

    private void validateRequestMetadata(MultipartFile upload) {
        if (upload == null || upload.isEmpty()) {
            throw ApiException.invalidVideo("Debes seleccionar un archivo MP4.");
        }
        if (upload.getSize() > maxFileSizeBytes) {
            throw ApiException.videoTooLarge(maxFileSizeBytes);
        }

        String originalName = upload.getOriginalFilename();
        if (originalName == null || !originalName.toLowerCase(Locale.ROOT).endsWith(".mp4")) {
            throw ApiException.invalidVideo("El formato permitido es MP4 con video H.264 (.mp4).");
        }

        String contentType = upload.getContentType();
        if (contentType != null && !contentType.isBlank()) {
            String normalizedType = contentType.split(";", 2)[0].trim().toLowerCase(Locale.ROOT);
            if (!normalizedType.equals(MP4_CONTENT_TYPE) && !normalizedType.equals(GENERIC_CONTENT_TYPE)) {
                throw ApiException.invalidVideo("El archivo debe enviarse en formato MP4.");
            }
        }
    }

    private Path pathFor(UUID storageKey) {
        Path file = storageDirectory.resolve(storageKey + ".mp4").normalize();
        if (!file.startsWith(storageDirectory)) {
            throw new IllegalArgumentException("Invalid video storage key");
        }
        return file;
    }

    private void deleteIfPresent(Path file) {
        if (file == null) {
            return;
        }
        try {
            Files.deleteIfExists(file);
        } catch (IOException exception) {
            LOGGER.warn("Could not delete temporary video file {}", file, exception);
        }
    }

    public record StoredVideo(UUID storageKey, long sizeBytes, int durationSeconds) {
    }
}
