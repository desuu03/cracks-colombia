package co.uniquindio.crackscolombia.cracks_colombia_api.moderation;

import co.uniquindio.crackscolombia.cracks_colombia_api.auth.dto.request.GuardianConsentRequest;
import co.uniquindio.crackscolombia.cracks_colombia_api.auth.dto.request.RegisterRequest;
import co.uniquindio.crackscolombia.cracks_colombia_api.search.repository.AthleteProfileRepository;
import co.uniquindio.crackscolombia.cracks_colombia_api.shared.security.JwtService;
import co.uniquindio.crackscolombia.cracks_colombia_api.user.entity.UserAccount;
import co.uniquindio.crackscolombia.cracks_colombia_api.user.entity.UserRole;
import co.uniquindio.crackscolombia.cracks_colombia_api.user.repository.AthleteProfileDetailsRepository;
import co.uniquindio.crackscolombia.cracks_colombia_api.user.repository.UserAccountRepository;
import co.uniquindio.crackscolombia.cracks_colombia_api.video.repository.VideoRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "app.video.storage-dir=target/moderation-integration-test-storage")
class ModerationIntegrationTest {

    private static final Path STORAGE_DIRECTORY = Path.of("target/moderation-integration-test-storage");
    private static final String PROFILE = """
            {"sport":"Fútbol","position":"Delantero","region":"Quindío",
             "heightCm":170,"weightKg":60,"trajectory":"Escuela de formación"}
            """;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private Clock clock;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private AthleteProfileRepository athleteProfileRepository;

    @Autowired
    private AthleteProfileDetailsRepository detailsRepository;

    @Autowired
    private VideoRepository videoRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    @AfterEach
    void clearTestData() throws IOException {
        videoRepository.deleteAll();
        detailsRepository.deleteAll();
        athleteProfileRepository.deleteAll();
        userAccountRepository.deleteAll();
        if (Files.isDirectory(STORAGE_DIRECTORY)) {
            try (var paths = Files.list(STORAGE_DIRECTORY)) {
                for (Path path : paths.toList()) {
                    Files.deleteIfExists(path);
                }
            }
        }
    }

    @Test
    void approvingAPendingVideoPublishesItAndRecordsTheModerator() throws Exception {
        String athleteToken = registerAthlete("menor@example.com", "Sofía Gómez", 15);
        UUID athleteId = userAccountRepository.findByEmail("menor@example.com").orElseThrow().getId();
        completeProfile(athleteToken);
        String videoId = uploadVideo(athleteToken, athleteId);
        UserAccount moderator = saveModerator();
        String moderatorToken = jwtService.createToken(moderator);
        String scoutToken = registerScout();

        mockMvc.perform(get("/moderation/queue").header("Authorization", "Bearer " + moderatorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.items[0].id").value(videoId))
                .andExpect(jsonPath("$.items[0].status").value("PENDIENTE"))
                .andExpect(jsonPath("$.items[0].athleteFullName").value("Sofía Gómez"))
                .andExpect(jsonPath("$.items[0].athleteAge").value(15))
                .andExpect(jsonPath("$.items[0].minor").value(true))
                .andExpect(jsonPath("$.items[0].sport").value("Fútbol"))
                .andExpect(jsonPath("$.items[0].url").value("/videos/" + videoId + "/content"))
                .andExpect(jsonPath("$.items[0].moderatedById").value(nullValue()))
                .andExpect(jsonPath("$.items[0].moderatedAt").value(nullValue()));

        mockMvc.perform(get("/athletes/{athleteId}/videos", athleteId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
        assertAthleteRemainsSearchable(scoutToken, athleteId);

        mockMvc.perform(post("/moderation/{videoId}/approve", videoId)
                        .header("Authorization", "Bearer " + moderatorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APROBADO"))
                .andExpect(jsonPath("$.rejectionReason").value(nullValue()))
                .andExpect(jsonPath("$.moderatedById").value(moderator.getId().toString()))
                .andExpect(jsonPath("$.moderatedByName").value("Laura Moderadora"))
                .andExpect(jsonPath("$.moderatedAt").isNotEmpty());

        mockMvc.perform(get("/athletes/{athleteId}/videos", athleteId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(videoId))
                .andExpect(jsonPath("$[0].status").value("APROBADO"));
        mockMvc.perform(get("/videos/{videoId}/content", videoId))
                .andExpect(status().isOk());
        mockMvc.perform(get("/moderation/queue").header("Authorization", "Bearer " + moderatorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
        assertAthleteRemainsSearchable(scoutToken, athleteId);

        mockMvc.perform(post("/moderation/{videoId}/approve", videoId)
                        .header("Authorization", "Bearer " + moderatorToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("VIDEO_ALREADY_MODERATED"));
        mockMvc.perform(post("/moderation/{videoId}/reject", videoId)
                        .header("Authorization", "Bearer " + moderatorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Intento tardío\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("VIDEO_ALREADY_MODERATED"));
    }

    @Test
    void rejectingRequiresAReasonAndHidesTheVideoFromScouts() throws Exception {
        String athleteToken = registerAthlete("juan@example.com", "Juan Pérez", 20);
        UUID athleteId = userAccountRepository.findByEmail("juan@example.com").orElseThrow().getId();
        completeProfile(athleteToken);
        String videoId = uploadVideo(athleteToken, athleteId);
        String moderatorToken = jwtService.createToken(saveModerator());
        String scoutToken = registerScout();

        mockMvc.perform(post("/moderation/{videoId}/reject", videoId)
                        .header("Authorization", "Bearer " + moderatorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"   \"}"))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));

        mockMvc.perform(post("/moderation/{videoId}/reject", videoId)
                        .header("Authorization", "Bearer " + moderatorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"" + "x".repeat(501) + "\"}"))
                .andExpect(status().is(422));

        mockMvc.perform(post("/moderation/{videoId}/reject", videoId)
                        .header("Authorization", "Bearer " + moderatorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\" Contenido ajeno a la disciplina \"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RECHAZADO"))
                .andExpect(jsonPath("$.rejectionReason").value("Contenido ajeno a la disciplina"))
                .andExpect(jsonPath("$.moderatedByName").value("Laura Moderadora"))
                .andExpect(jsonPath("$.moderatedAt").isNotEmpty());

        mockMvc.perform(get("/athletes/{athleteId}/videos", athleteId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
        mockMvc.perform(get("/videos/{videoId}/content", videoId)
                        .header("Authorization", "Bearer " + scoutToken))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/users/me/videos").header("Authorization", "Bearer " + athleteToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("RECHAZADO"))
                .andExpect(jsonPath("$[0].rejectionReason").value("Contenido ajeno a la disciplina"));
        assertAthleteRemainsSearchable(scoutToken, athleteId);

        mockMvc.perform(post("/moderation/{videoId}/reject", videoId)
                        .header("Authorization", "Bearer " + moderatorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Otro motivo\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("VIDEO_ALREADY_MODERATED"));
    }

    @Test
    void queueListsTheOldestPendingVideoFirst() throws Exception {
        String athleteToken = registerAthlete("ana@example.com", "Ana López", 19);
        UUID athleteId = userAccountRepository.findByEmail("ana@example.com").orElseThrow().getId();
        String olderId = uploadVideo(athleteToken, athleteId);
        String newerId = uploadVideo(athleteToken, athleteId);
        jdbcTemplate.update("update skill_videos set uploaded_at = ? where id = ?",
                Timestamp.from(clock.instant().minus(1, ChronoUnit.DAYS)), UUID.fromString(olderId));
        String moderatorToken = jwtService.createToken(saveModerator());

        mockMvc.perform(get("/moderation/queue").header("Authorization", "Bearer " + moderatorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(olderId))
                .andExpect(jsonPath("$.items[1].id").value(newerId));
    }

    @Test
    void otherRolesCannotModerateAndMissingVideosAreNotFound() throws Exception {
        String athleteToken = registerAthlete("dueno@example.com", "Carlos Díaz", 18);
        String scoutToken = registerScout();
        UUID missingId = UUID.randomUUID();

        mockMvc.perform(get("/moderation/queue"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/moderation/queue").header("Authorization", "Bearer " + athleteToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/moderation/{videoId}/approve", missingId)
                        .header("Authorization", "Bearer " + scoutToken))
                .andExpect(status().isForbidden());

        String moderatorToken = jwtService.createToken(saveModerator());
        mockMvc.perform(post("/moderation/{videoId}/approve", missingId)
                        .header("Authorization", "Bearer " + moderatorToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    private void assertAthleteRemainsSearchable(String scoutToken, UUID athleteId) throws Exception {
        mockMvc.perform(get("/athletes/search").param("sport", "Fútbol")
                        .header("Authorization", "Bearer " + scoutToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[*].athleteId", hasItem(athleteId.toString())));
    }

    private void completeProfile(String athleteToken) throws Exception {
        mockMvc.perform(put("/users/me").header("Authorization", "Bearer " + athleteToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PROFILE))
                .andExpect(status().isOk());
    }

    private String uploadVideo(String athleteToken, UUID athleteId) throws Exception {
        MvcResult upload = mockMvc.perform(multipart("/athletes/{athleteId}/videos", athleteId)
                        .file(new MockMultipartFile("file", "skills.mp4", "video/mp4", validMp4(8)))
                        .header("Authorization", "Bearer " + athleteToken))
                .andExpect(status().isCreated())
                .andReturn();
        return readJson(upload).get("id").asText();
    }

    private UserAccount saveModerator() {
        return userAccountRepository.saveAndFlush(new UserAccount(
                "moderador@example.com", "unused-hash", "Laura Moderadora", UserRole.MODERADOR,
                null, null, null, false, null, clock.instant()));
    }

    private String registerAthlete(String email, String fullName, int age) throws Exception {
        boolean minor = age < 18;
        GuardianConsentRequest guardian = minor
                ? new GuardianConsentRequest("Marta Gómez", "acudiente@example.com", true)
                : null;
        RegisterRequest request = new RegisterRequest(email, "CracksPass123", UserRole.DEPORTISTA,
                fullName, LocalDate.now(clock).minusYears(age), guardian, true);
        MvcResult result = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isCreated())
                .andReturn();
        return readJson(result).get("accessToken").asText();
    }

    private String registerScout() throws Exception {
        RegisterRequest request = new RegisterRequest("ojeador@example.com", "CracksPass123",
                UserRole.OJEADOR_ENTRENADOR, "Carlos Ramírez", null, null, true);
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

    private byte[] box(String type, byte[] content) {
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
}
