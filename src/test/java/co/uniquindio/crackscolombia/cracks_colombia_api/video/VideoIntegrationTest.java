package co.uniquindio.crackscolombia.cracks_colombia_api.video;

import co.uniquindio.crackscolombia.cracks_colombia_api.auth.dto.request.GuardianConsentRequest;
import co.uniquindio.crackscolombia.cracks_colombia_api.auth.dto.request.RegisterRequest;
import co.uniquindio.crackscolombia.cracks_colombia_api.shared.exception.ApiException;
import co.uniquindio.crackscolombia.cracks_colombia_api.user.entity.UserAccount;
import co.uniquindio.crackscolombia.cracks_colombia_api.user.entity.UserRole;
import co.uniquindio.crackscolombia.cracks_colombia_api.user.repository.UserAccountRepository;
import co.uniquindio.crackscolombia.cracks_colombia_api.video.entity.Video;
import co.uniquindio.crackscolombia.cracks_colombia_api.video.repository.VideoRepository;
import co.uniquindio.crackscolombia.cracks_colombia_api.video.service.VideoService;
import co.uniquindio.crackscolombia.cracks_colombia_api.video.service.VideoStorageService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "app.video.storage-dir=target/video-integration-test-storage")
class VideoIntegrationTest {

    private static final Path STORAGE_DIRECTORY = Path.of("target/video-integration-test-storage");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private VideoRepository videoRepository;

    @Autowired
    private VideoStorageService videoStorageService;

    @Autowired
    private VideoService videoService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void clearPreviousTestData() throws IOException {
        clearTestData();
    }

    @AfterEach
    void cleanTestData() throws IOException {
        clearTestData();
    }

    @Test
    void athleteCanUploadPendingVideoAndAccessItFromTheirGallery() throws Exception {
        String token = registerAndGetToken("athlete@example.com", UserRole.DEPORTISTA,
                LocalDate.now().minusYears(20), null);
        UUID athleteId = userAccountRepository.findByEmail("athlete@example.com").orElseThrow().getId();
        byte[] mp4 = validMp4(12);

        MvcResult upload = mockMvc.perform(multipart("/athletes/{athleteId}/videos", athleteId)
                        .file(mp4File(mp4))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDIENTE"))
                .andExpect(jsonPath("$.durationSeconds").value(12))
                .andExpect(jsonPath("$.sizeBytes").value(mp4.length))
                .andReturn();
        JsonNode video = readJson(upload);
        String videoId = video.get("id").asText();

        mockMvc.perform(get("/users/me/videos")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(videoId))
                .andExpect(jsonPath("$[0].status").value("PENDIENTE"));

        mockMvc.perform(get("/athletes/{athleteId}/videos", athleteId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());

        mockMvc.perform(get("/videos/{videoId}/content", videoId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(result -> assertArrayEquals(mp4, result.getResponse().getContentAsByteArray()));

        mockMvc.perform(get("/videos/{videoId}/content", videoId))
                .andExpect(status().isNotFound());
    }

    @Test
    void approvedVideoAppearsInPublicGalleryAndCanBeDeletedByItsOwner() throws Exception {
        String token = registerAndGetToken("approved@example.com", UserRole.DEPORTISTA,
                LocalDate.now().minusYears(22), null);
        UUID athleteId = userAccountRepository.findByEmail("approved@example.com").orElseThrow().getId();
        MvcResult upload = mockMvc.perform(multipart("/athletes/{athleteId}/videos", athleteId)
                        .file(mp4File(validMp4(8)))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andReturn();
        String videoId = readJson(upload).get("id").asText();
        Video savedVideo = videoRepository.findById(UUID.fromString(videoId)).orElseThrow();
        Path storedFile = STORAGE_DIRECTORY.resolve(savedVideo.getStorageKey() + ".mp4");
        assertTrue(Files.isRegularFile(storedFile));

        jdbcTemplate.update("UPDATE skill_videos SET status = 'APROBADO' WHERE id = ?", UUID.fromString(videoId));

        mockMvc.perform(get("/athletes/{athleteId}/videos", athleteId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(videoId))
                .andExpect(jsonPath("$[0].status").value("APROBADO"));

        mockMvc.perform(get("/videos/{videoId}/content", videoId))
                .andExpect(status().isOk())
                .andExpect(result -> assertEquals("video/mp4", result.getResponse().getContentType()));

        mockMvc.perform(delete("/videos/{videoId}", videoId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        assertFalse(Files.exists(storedFile));
        assertFalse(videoRepository.existsById(UUID.fromString(videoId)));
    }

    @Test
    void minorAthleteCanUploadOnlyAfterGuardianAndDataConsentsWereAccepted() throws Exception {
        GuardianConsentRequest guardian = new GuardianConsentRequest(
                "Acudiente Responsable", "guardian@example.com", true);
        String token = registerAndGetToken("minor@example.com", UserRole.DEPORTISTA,
                LocalDate.now().minusYears(16), guardian);
        UUID athleteId = userAccountRepository.findByEmail("minor@example.com").orElseThrow().getId();

        mockMvc.perform(multipart("/athletes/{athleteId}/videos", athleteId)
                        .file(mp4File(validMp4(10)))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDIENTE"));

        UserAccount minorWithoutGuardianConsent = userAccountRepository.saveAndFlush(new UserAccount(
                "unsafe-minor@example.com", "unused-hash", "Menor sin autorización", UserRole.DEPORTISTA,
                LocalDate.now().minusYears(16), null, null, false, null, java.time.Instant.now()));
        ApiException exception = assertThrows(ApiException.class,
                () -> videoService.create(minorWithoutGuardianConsent.getId(),
                        minorWithoutGuardianConsent.getEmail(), mp4File(validMp4(5))));
        assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
        assertTrue(exception.getMessage().contains("acudiente"));
    }

    @Test
    void rejectsWrongFormatsMalformedMp4AndVideosLongerThanLimit() throws Exception {
        String token = registerAndGetToken("validation@example.com", UserRole.DEPORTISTA,
                LocalDate.now().minusYears(19), null);
        UUID athleteId = userAccountRepository.findByEmail("validation@example.com").orElseThrow().getId();

        mockMvc.perform(multipart("/athletes/{athleteId}/videos", athleteId)
                        .file(new MockMultipartFile("file", "clip.avi", "video/x-msvideo", validMp4(5)))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_VIDEO"));

        mockMvc.perform(multipart("/athletes/{athleteId}/videos", athleteId)
                        .file(new MockMultipartFile("file", "fake.mp4", "video/mp4",
                                "no es un mp4".getBytes(StandardCharsets.UTF_8)))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_VIDEO"));

        mockMvc.perform(multipart("/athletes/{athleteId}/videos", athleteId)
                        .file(mp4File(validMp4(61)))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_VIDEO"));

        mockMvc.perform(get("/users/me/videos").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void rejectsOversizedFilesBeforeReadingTheirContents() {
        ApiException exception = assertThrows(ApiException.class,
                () -> videoStorageService.store(new OversizedMultipartFile()));
        assertEquals(HttpStatus.PAYLOAD_TOO_LARGE, exception.getStatus());
    }

    @Test
    void roleAndOwnershipRestrictionsProtectUploadGalleryAndDeletion() throws Exception {
        String ownerToken = registerAndGetToken("owner@example.com", UserRole.DEPORTISTA,
                LocalDate.now().minusYears(20), null);
        String otherAthleteToken = registerAndGetToken("other@example.com", UserRole.DEPORTISTA,
                LocalDate.now().minusYears(20), null);
        String coachToken = registerAndGetToken("coach@example.com", UserRole.OJEADOR_ENTRENADOR,
                null, null);
        UUID ownerId = userAccountRepository.findByEmail("owner@example.com").orElseThrow().getId();
        UUID otherId = userAccountRepository.findByEmail("other@example.com").orElseThrow().getId();

        MvcResult upload = mockMvc.perform(multipart("/athletes/{athleteId}/videos", ownerId)
                        .file(mp4File(validMp4(6)))
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isCreated())
                .andReturn();
        String videoId = readJson(upload).get("id").asText();

        mockMvc.perform(multipart("/athletes/{athleteId}/videos", ownerId)
                        .file(mp4File(validMp4(6)))
                        .header("Authorization", "Bearer " + coachToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/users/me/videos").header("Authorization", "Bearer " + coachToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(multipart("/athletes/{athleteId}/videos", ownerId)
                        .file(mp4File(validMp4(6)))
                        .header("Authorization", "Bearer " + otherAthleteToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/videos/{videoId}", videoId)
                        .header("Authorization", "Bearer " + otherAthleteToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/videos/{videoId}/content", videoId)
                        .header("Authorization", "Bearer " + otherAthleteToken))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/videos/{videoId}", videoId))
                .andExpect(status().isUnauthorized());
    }

    private String registerAndGetToken(String email, UserRole role, LocalDate birthDate,
                                      GuardianConsentRequest guardianConsent) throws Exception {
        RegisterRequest request = new RegisterRequest(email, "CracksPass123", role,
                "Usuario de Prueba", birthDate, guardianConsent, true);
        MvcResult result = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isCreated())
                .andReturn();
        return readJson(result).get("accessToken").asText();
    }

    private JsonNode readJson(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private MockMultipartFile mp4File(byte[] contents) {
        return new MockMultipartFile("file", "skills.mp4", "video/mp4", contents);
    }

    private void clearTestData() throws IOException {
        videoRepository.deleteAll();
        userAccountRepository.deleteAll();
        if (Files.isDirectory(STORAGE_DIRECTORY)) {
            try (var paths = Files.list(STORAGE_DIRECTORY)) {
                for (Path path : paths.toList()) {
                    Files.deleteIfExists(path);
                }
            }
        }
    }

    private byte[] validMp4(int durationSeconds) throws IOException {
        byte[] fileType = box("ftyp", concat(ascii("isom"), intBytes(0), ascii("isom"), ascii("mp4v")));
        byte[] movieHeader = box("mvhd", concat(intBytes(0), intBytes(0), intBytes(0),
                intBytes(1_000), intBytes(durationSeconds * 1_000)));
        byte[] handler = box("hdlr", concat(intBytes(0), intBytes(0), ascii("vide")));
        byte[] sampleEntry = box("mp4v", new byte[0]);
        byte[] sampleDescription = box("stsd", concat(intBytes(0), intBytes(1), sampleEntry));
        byte[] sampleSizes = box("stsz", concat(intBytes(0), intBytes(0), intBytes(1)));
        byte[] sampleTable = box("stbl", concat(sampleDescription, sampleSizes));
        byte[] mediaInformation = box("minf", sampleTable);
        byte[] media = box("mdia", concat(handler, mediaInformation));
        byte[] track = box("trak", media);
        byte[] movie = box("moov", concat(movieHeader, track));
        byte[] mediaData = box("mdat", new byte[]{1, 2, 3, 4});
        return concat(fileType, movie, mediaData);
    }

    private byte[] box(String type, byte[] content) throws IOException {
        ByteBuffer box = ByteBuffer.allocate(8 + content.length).order(ByteOrder.BIG_ENDIAN);
        box.putInt(8 + content.length);
        box.put(ascii(type));
        box.put(content);
        return box.array();
    }

    private byte[] ascii(String value) {
        return value.getBytes(StandardCharsets.ISO_8859_1);
    }

    private byte[] intBytes(int value) {
        return ByteBuffer.allocate(Integer.BYTES).order(ByteOrder.BIG_ENDIAN).putInt(value).array();
    }

    private byte[] concat(byte[]... arrays) throws IOException {
        var output = new java.io.ByteArrayOutputStream();
        for (byte[] array : arrays) {
            output.write(array);
        }
        return output.toByteArray();
    }

    private static final class OversizedMultipartFile implements MultipartFile {

        @Override
        public String getName() {
            return "file";
        }

        @Override
        public String getOriginalFilename() {
            return "too-large.mp4";
        }

        @Override
        public String getContentType() {
            return "video/mp4";
        }

        @Override
        public boolean isEmpty() {
            return false;
        }

        @Override
        public long getSize() {
            return 50_000_001L;
        }

        @Override
        public byte[] getBytes() {
            throw new AssertionError("Oversized content must be rejected before it is read.");
        }

        @Override
        public InputStream getInputStream() {
            throw new AssertionError("Oversized content must be rejected before it is read.");
        }

        @Override
        public void transferTo(File destination) {
            throw new AssertionError("Oversized content must be rejected before it is read.");
        }
    }
}
