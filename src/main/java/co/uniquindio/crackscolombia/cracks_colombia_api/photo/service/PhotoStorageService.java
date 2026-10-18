package co.uniquindio.crackscolombia.cracks_colombia_api.photo.service;

import co.uniquindio.crackscolombia.cracks_colombia_api.shared.exception.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Iterator;
import java.util.Locale;
import java.util.UUID;

@Service
public class PhotoStorageService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PhotoStorageService.class);
    private static final long MAX_PIXELS = 16_000_000L;
    private static final String JPEG = "image/jpeg";
    private static final String PNG = "image/png";
    private static final String GENERIC = "application/octet-stream";

    private final Path storageDirectory;
    private final long maxFileSizeBytes;

    public PhotoStorageService(
            @Value("${app.photo.storage-dir:./data/photos}") String storageDirectory,
            @Value("${app.photo.max-file-size-bytes:5000000}") long maxFileSizeBytes) {
        this.storageDirectory = Path.of(storageDirectory).toAbsolutePath().normalize();
        this.maxFileSizeBytes = maxFileSizeBytes;
    }

    public StoredPhoto store(MultipartFile upload) {
        validateRequestMetadata(upload);
        UUID storageKey = UUID.randomUUID();
        Path temporaryFile = null;
        try {
            Files.createDirectories(storageDirectory);
            temporaryFile = Files.createTempFile(storageDirectory, "photo-upload-", ".tmp");
            try (InputStream input = upload.getInputStream()) {
                Files.copy(input, temporaryFile, StandardCopyOption.REPLACE_EXISTING);
            }

            long sizeBytes = Files.size(temporaryFile);
            if (sizeBytes == 0) {
                throw ApiException.invalidPhoto("El archivo de imagen está vacío.");
            }
            if (sizeBytes > maxFileSizeBytes) {
                throw ApiException.photoTooLarge(maxFileSizeBytes);
            }
            String contentType;
            try {
                contentType = detectAndValidateImage(temporaryFile, upload);
            } catch (IOException exception) {
                throw ApiException.invalidPhoto("El archivo no contiene una imagen JPEG o PNG válida.");
            }

            try {
                Files.move(temporaryFile, pathFor(storageKey), StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporaryFile, pathFor(storageKey));
            }
            return new StoredPhoto(storageKey, contentType, sizeBytes);
        } catch (ApiException exception) {
            deleteIfPresent(temporaryFile);
            throw exception;
        } catch (IOException exception) {
            deleteIfPresent(temporaryFile);
            throw new IllegalStateException("No fue posible almacenar la foto.", exception);
        }
    }

    public Resource load(UUID storageKey) {
        Path file = pathFor(storageKey);
        if (!Files.isRegularFile(file)) {
            throw ApiException.notFound("El archivo de la foto no está disponible.");
        }
        return new FileSystemResource(file);
    }

    public void deleteQuietly(UUID storageKey) {
        try {
            Files.deleteIfExists(pathFor(storageKey));
        } catch (IOException exception) {
            LOGGER.warn("Could not delete stored photo {}", storageKey, exception);
        }
    }

    private void validateRequestMetadata(MultipartFile upload) {
        if (upload == null || upload.isEmpty()) {
            throw ApiException.invalidPhoto("Debes seleccionar una imagen JPEG o PNG.");
        }
        if (upload.getSize() > maxFileSizeBytes) {
            throw ApiException.photoTooLarge(maxFileSizeBytes);
        }

        String originalName = upload.getOriginalFilename();
        if (originalName == null) {
            throw ApiException.invalidPhoto("El archivo debe tener extensión .jpg, .jpeg o .png.");
        }
        String normalizedName = originalName.toLowerCase(Locale.ROOT);
        if (!normalizedName.endsWith(".jpg") && !normalizedName.endsWith(".jpeg")
                && !normalizedName.endsWith(".png")) {
            throw ApiException.invalidPhoto("Los formatos permitidos son JPEG y PNG.");
        }

        String contentType = normalizedContentType(upload.getContentType());
        if (contentType != null && !contentType.equals(JPEG) && !contentType.equals(PNG)
                && !contentType.equals(GENERIC)) {
            throw ApiException.invalidPhoto("La foto debe enviarse en formato JPEG o PNG.");
        }
    }

    private String detectAndValidateImage(Path file, MultipartFile upload) throws IOException {
        String format;
        try (ImageInputStream input = ImageIO.createImageInputStream(file.toFile())) {
            if (input == null) {
                throw ApiException.invalidPhoto("El archivo no contiene una imagen válida.");
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) {
                throw ApiException.invalidPhoto("El archivo no contiene una imagen JPEG o PNG válida.");
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(input, true, true);
                format = reader.getFormatName().toLowerCase(Locale.ROOT);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if ((long) width * height > MAX_PIXELS) {
                    throw ApiException.invalidPhoto("La resolución de la foto es demasiado grande.");
                }
                if (reader.read(0) == null) {
                    throw ApiException.invalidPhoto("El archivo no contiene una imagen válida.");
                }
            } finally {
                reader.dispose();
            }
        }

        String detectedType = switch (format) {
            case "jpeg", "jpg" -> JPEG;
            case "png" -> PNG;
            default -> throw ApiException.invalidPhoto("Los formatos permitidos son JPEG y PNG.");
        };
        String declaredType = normalizedContentType(upload.getContentType());
        if (declaredType != null && !declaredType.equals(GENERIC) && !declaredType.equals(detectedType)) {
            throw ApiException.invalidPhoto("El contenido de la foto no coincide con su tipo de archivo.");
        }
        String filename = upload.getOriginalFilename().toLowerCase(Locale.ROOT);
        if (detectedType.equals(PNG) != filename.endsWith(".png")) {
            throw ApiException.invalidPhoto("La extensión del archivo no coincide con su contenido.");
        }
        return detectedType;
    }

    private String normalizedContentType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return null;
        }
        return contentType.split(";", 2)[0].trim().toLowerCase(Locale.ROOT);
    }

    private Path pathFor(UUID storageKey) {
        Path file = storageDirectory.resolve(storageKey.toString()).normalize();
        if (!file.startsWith(storageDirectory)) {
            throw new IllegalArgumentException("Invalid photo storage key");
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
            LOGGER.warn("Could not delete temporary photo file {}", file, exception);
        }
    }

    public record StoredPhoto(UUID storageKey, String contentType, long sizeBytes) {
    }
}
