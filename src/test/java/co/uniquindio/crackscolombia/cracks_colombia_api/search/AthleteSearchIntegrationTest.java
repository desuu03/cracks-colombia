package co.uniquindio.crackscolombia.cracks_colombia_api.search;

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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AthleteSearchIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private AthleteProfileRepository athleteProfileRepository;

    @Autowired
    private Clock clock;

    @BeforeEach
    @AfterEach
    void clearTestData() {
        athleteProfileRepository.deleteAll();
        userAccountRepository.deleteAll();
    }

    @Test
    void combinesFiltersReturnsOnlyApprovedProfilesAndCalculatesAge() throws Exception {
        LocalDate today = LocalDate.now(clock);
        UUID photoStorageKey = UUID.randomUUID();
        seedAthlete("match@example.com", "Ana Delantera", today.minusYears(19), "fútbol",
                "delantera", "Quindío", photoStorageKey, AthleteProfileStatus.APROBADO);
        seedAthlete("age-mismatch@example.com", "Beatriz Delantera", today.minusYears(17), "fútbol",
                "delantera", "Quindío", null, AthleteProfileStatus.APROBADO);
        seedAthlete("position-mismatch@example.com", "Carolina Portera", today.minusYears(19), "fútbol",
                "portera", "Quindío", null, AthleteProfileStatus.APROBADO);
        seedAthlete("pending@example.com", "Daniela Pendiente", today.minusYears(19), "fútbol",
                "delantera", "Quindío", null, AthleteProfileStatus.PENDIENTE);

        String token = registerScoutAndGetToken();
        UUID athleteId = userAccountRepository.findByEmail("match@example.com").orElseThrow().getId();

        mockMvc.perform(get("/athletes/search")
                        .param("sport", "FÚTBOL")
                        .param("position", "delantera")
                        .param("minAge", "18")
                        .param("maxAge", "20")
                        .param("region", "QUINDÍO")
                        .param("page", "0")
                        .param("size", "10")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].fullName").value("Ana Delantera"))
                .andExpect(jsonPath("$.items[0].sport").value("fútbol"))
                .andExpect(jsonPath("$.items[0].position").value("delantera"))
                .andExpect(jsonPath("$.items[0].age").value(19))
                .andExpect(jsonPath("$.items[0].region").value("Quindío"))
                .andExpect(jsonPath("$.items[0].photoUrl").value("/athletes/" + athleteId + "/photo"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.message").doesNotExist());
    }

    @Test
    void returnsInformativeMessageWhenThereAreNoMatches() throws Exception {
        seedAthlete("approved@example.com", "Deportista Aprobado", LocalDate.now(clock).minusYears(22),
                "baloncesto", "base", "Antioquia", null, AthleteProfileStatus.APROBADO);

        mockMvc.perform(get("/athletes/search")
                        .param("sport", "natación")
                        .header("Authorization", "Bearer " + registerScoutAndGetToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.totalPages").value(0))
                .andExpect(jsonPath("$.message").value(
                        "No se encontraron deportistas con los filtros aplicados."));
    }

    @Test
    void paginatesSearchResults() throws Exception {
        LocalDate birthDate = LocalDate.now(clock).minusYears(21);
        seedAthlete("page-a@example.com", "Andrea", birthDate, "voleibol", "central", "Caldas",
                null, AthleteProfileStatus.APROBADO);
        seedAthlete("page-b@example.com", "Berta", birthDate, "voleibol", "central", "Caldas",
                null, AthleteProfileStatus.APROBADO);
        seedAthlete("page-c@example.com", "Camila", birthDate, "voleibol", "central", "Caldas",
                null, AthleteProfileStatus.APROBADO);

        mockMvc.perform(get("/athletes/search")
                        .param("sport", "voleibol")
                        .param("page", "1")
                        .param("size", "2")
                        .header("Authorization", "Bearer " + registerScoutAndGetToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.first").value(false))
                .andExpect(jsonPath("$.last").value(true));
    }

    private void seedAthlete(String email, String fullName, LocalDate birthDate, String sport,
                             String position, String region, UUID photoStorageKey, AthleteProfileStatus status) {
        UserAccount athlete = userAccountRepository.saveAndFlush(new UserAccount(
                email, "unused-hash", fullName, UserRole.DEPORTISTA, birthDate,
                null, null, false, null, java.time.Instant.now(clock)));
        AthleteProfile profile = new AthleteProfile(athlete, sport, position, region, status);
        if (photoStorageKey != null) {
            profile.setPhoto(photoStorageKey, "image/jpeg", 100L);
        }
        athleteProfileRepository.saveAndFlush(profile);
    }

    private String registerScoutAndGetToken() throws Exception {
        String email = "scout-" + UUID.randomUUID() + "@example.com";
        RegisterRequest request = new RegisterRequest(email, "CracksPass123", UserRole.OJEADOR_ENTRENADOR,
                "Ojeador de Prueba", null, null, true);
        MvcResult result = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        return response.get("accessToken").asText();
    }
}
