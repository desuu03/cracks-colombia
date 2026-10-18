package co.uniquindio.crackscolombia.cracks_colombia_api.photo;

import co.uniquindio.crackscolombia.cracks_colombia_api.auth.dto.request.RegisterRequest;
import co.uniquindio.crackscolombia.cracks_colombia_api.search.entity.AthleteProfile;
import co.uniquindio.crackscolombia.cracks_colombia_api.search.entity.AthleteProfileStatus;
import co.uniquindio.crackscolombia.cracks_colombia_api.search.repository.AthleteProfileRepository;
import co.uniquindio.crackscolombia.cracks_colombia_api.user.entity.UserAccount;
import co.uniquindio.crackscolombia.cracks_colombia_api.user.entity.UserRole;
import co.uniquindio.crackscolombia.cracks_colombia_api.user.repository.UserAccountRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "app.photo.storage-dir=target/photo-integration-test-storage")
class PhotoIntegrationTest {

    private static final Path STORAGE_DIRECTORY = Path.of("target/photo-integration-test-storage");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private AthleteProfileRepository athleteProfileRepository;

    @BeforeEach
    @AfterEach
    void clearTestData() throws IOException {
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
    void storesPhotoAsFileAndServesItToVisitorsForApprovedProfiles() throws Exception {
        String token = registerAthleteAndGetToken("photo-owner@example.com");
        UserAccount athlete = userAccountRepository.findByEmail("photo-owner@example.com").orElseThrow();
        athleteProfileRepository.saveAndFlush(new AthleteProfile(athlete, "fútbol", "delantera",
                "Quindío", AthleteProfileStatus.APROBADO));
        byte[] png = pngImage();

        MvcResult uploadResult = mockMvc.perform(multipart(HttpMethod.PUT,
                                "/athletes/{athleteId}/photo", athlete.getId())
                        .file(photoFile(png))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.athleteId").value(athlete.getId().toString()))
                .andExpect(jsonPath("$.photoUrl").value("/athletes/" + athlete.getId() + "/photo"))
                .andExpect(jsonPath("$.contentType").value("image/png"))
                .andExpect(jsonPath("$.sizeBytes").value(png.length))
                .andReturn();

        JsonNode response = objectMapper.readTree(uploadResult.getResponse().getContentAsString());
        var profile = athleteProfileRepository.findByAthlete_Id(athlete.getId()).orElseThrow();
        Path storedFile = STORAGE_DIRECTORY.resolve(profile.getPhotoStorageKey().toString());
        assertTrue(Files.isRegularFile(storedFile));
        assertArrayEquals(png, Files.readAllBytes(storedFile));
        assertTrue(response.get("photoUrl").asText().endsWith("/photo"));

        mockMvc.perform(get("/athletes/{athleteId}/photo", athlete.getId()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG))
                .andExpect(result -> assertArrayEquals(png, result.getResponse().getContentAsByteArray()));
    }

    @Test
    void keepsPendingProfilePhotoPrivateAndRejectsFilesThatAreNotImages() throws Exception {
        String token = registerAthleteAndGetToken("pending-photo@example.com");
        UserAccount athlete = userAccountRepository.findByEmail("pending-photo@example.com").orElseThrow();
        byte[] png = pngImage();

        mockMvc.perform(multipart(HttpMethod.PUT, "/athletes/{athleteId}/photo", athlete.getId())
                        .file(photoFile(png))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(get("/athletes/{athleteId}/photo", athlete.getId()))
                .andExpect(status().isNotFound());

        mockMvc.perform(multipart(HttpMethod.PUT, "/athletes/{athleteId}/photo", athlete.getId())
                        .file(new MockMultipartFile("file", "fake.png", "image/png",
                                "not an image".getBytes(java.nio.charset.StandardCharsets.UTF_8)))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PHOTO"));
    }

    private MockMultipartFile photoFile(byte[] contents) {
        return new MockMultipartFile("file", "profile.png", "image/png", contents);
    }

    private byte[] pngImage() throws IOException {
        BufferedImage image = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);
        return output.toByteArray();
    }

    private String registerAthleteAndGetToken(String email) throws Exception {
        RegisterRequest request = new RegisterRequest(email, "CracksPass123", UserRole.DEPORTISTA,
                "Deportista de Prueba", LocalDate.now().minusYears(20), null, true);
        MvcResult result = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        return response.get("accessToken").asText();
    }

}
