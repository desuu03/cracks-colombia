package co.uniquindio.crackscolombia.cracks_colombia_api.user;

import co.uniquindio.crackscolombia.cracks_colombia_api.auth.dto.request.GuardianConsentRequest;
import co.uniquindio.crackscolombia.cracks_colombia_api.auth.dto.request.RegisterRequest;
import co.uniquindio.crackscolombia.cracks_colombia_api.match.repository.MatchRequestRepository;
import co.uniquindio.crackscolombia.cracks_colombia_api.match.repository.ScoutInterestRepository;
import co.uniquindio.crackscolombia.cracks_colombia_api.notification.repository.NotificationRepository;
import co.uniquindio.crackscolombia.cracks_colombia_api.notification.repository.ProfileViewRepository;
import co.uniquindio.crackscolombia.cracks_colombia_api.search.repository.AthleteProfileRepository;
import co.uniquindio.crackscolombia.cracks_colombia_api.user.entity.UserRole;
import co.uniquindio.crackscolombia.cracks_colombia_api.user.repository.AthleteProfileDetailsRepository;
import co.uniquindio.crackscolombia.cracks_colombia_api.user.repository.ScoutProfileRepository;
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

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class UserProfileIntegrationTest {

    private static final String VALID_PROFILE = """
            {"sport":"Fútbol","position":"Delantero","region":"Quindío",
             "heightCm":176,"weightKg":70,"trajectory":"  Goleador de la Liga Quindiana  "}
            """;

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private Clock clock;
    @Autowired private UserAccountRepository userAccountRepository;
    @Autowired private AthleteProfileRepository athleteProfileRepository;
    @Autowired private AthleteProfileDetailsRepository detailsRepository;
    @Autowired private ScoutProfileRepository scoutProfileRepository;
    @Autowired private ScoutInterestRepository scoutInterestRepository;
    @Autowired private MatchRequestRepository matchRequestRepository;
    @Autowired private NotificationRepository notificationRepository;
    @Autowired private ProfileViewRepository profileViewRepository;

    @BeforeEach
    @AfterEach
    void clearTestData() {
        notificationRepository.deleteAll();
        profileViewRepository.deleteAll();
        matchRequestRepository.deleteAll();
        scoutInterestRepository.deleteAll();
        detailsRepository.deleteAll();
        scoutProfileRepository.deleteAll();
        athleteProfileRepository.deleteAll();
        userAccountRepository.deleteAll();
    }

    @Test
    void athleteCreatesProfileThatAppearsInSearch() throws Exception {
        String athleteToken = register("juan@example.com", UserRole.DEPORTISTA, LocalDate.now(clock).minusYears(20), false);

        mockMvc.perform(get("/users/me").header("Authorization", "Bearer " + athleteToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("DEPORTISTA"))
                .andExpect(jsonPath("$.sport").doesNotExist())
                .andExpect(jsonPath("$.profileStatus").doesNotExist());

        mockMvc.perform(put("/users/me").header("Authorization", "Bearer " + athleteToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_PROFILE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sport").value("Fútbol"))
                .andExpect(jsonPath("$.heightCm").value(176))
                .andExpect(jsonPath("$.trajectory").value("Goleador de la Liga Quindiana"))
                .andExpect(jsonPath("$.profileStatus").value("APROBADO"));

        // A second save updates the existing athlete_profiles row instead of creating another.
        mockMvc.perform(put("/users/me").header("Authorization", "Bearer " + athleteToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sport\":\"Fútbol\",\"position\":\"Volante\",\"region\":\"Antioquia\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.position").value("Volante"))
                .andExpect(jsonPath("$.heightCm").doesNotExist());

        String scoutToken = register("scout@example.com", UserRole.OJEADOR_ENTRENADOR, null, false);
        mockMvc.perform(get("/athletes/search").param("region", "Antioquia")
                        .header("Authorization", "Bearer " + scoutToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.items[0].fullName").value("Usuario juan@example.com"))
                .andExpect(jsonPath("$.items[0].position").value("Volante"));
    }

    @Test
    void rejectsValuesOutsideTheCatalogAndRanges() throws Exception {
        String token = register("ana@example.com", UserRole.DEPORTISTA, LocalDate.now(clock).minusYears(20), false);

        mockMvc.perform(put("/users/me").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sport\":\"Fútbol\",\"position\":\"Base\",\"region\":\"Quindío\"}"))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));

        mockMvc.perform(put("/users/me").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sport\":\"Fútbol\",\"position\":\"Delantero\",\"region\":\"Quindío\",\"heightCm\":300}"))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.message").value("La estatura debe estar entre 100 y 230 cm."));

        // birthDate is not part of the update and stays as registered.
        mockMvc.perform(put("/users/me").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sport\":\"Fútbol\",\"position\":\"Delantero\",\"region\":\"Quindío\",\"birthDate\":\"2015-01-01\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.birthDate").value(LocalDate.now(clock).minusYears(20).toString()));
    }

    @Test
    void publicProfileHidesBirthDateAndEmail() throws Exception {
        String token = register("menor@example.com", UserRole.DEPORTISTA, LocalDate.now(clock).minusYears(15), true);
        mockMvc.perform(put("/users/me").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(VALID_PROFILE)).andExpect(status().isOk());
        String athleteId = userAccountRepository.findByEmail("menor@example.com").orElseThrow().getId().toString();

        mockMvc.perform(get("/athletes/{athleteId}", athleteId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.age").value(15))
                .andExpect(jsonPath("$.sport").value("Fútbol"))
                .andExpect(jsonPath("$.birthDate").doesNotExist())
                .andExpect(jsonPath("$.email").doesNotExist())
                .andExpect(jsonPath("$.interestedByMe").value(false));

        String scoutToken = register("scout@example.com", UserRole.OJEADOR_ENTRENADOR, null, false);
        mockMvc.perform(get("/athletes/{athleteId}", "00000000-0000-0000-0000-000000000000")
                        .header("Authorization", "Bearer " + scoutToken))
                .andExpect(status().isNotFound());
        // Two visits on the same day produce a single PROFILE_VIEWED notification.
        mockMvc.perform(get("/athletes/{athleteId}", athleteId).header("Authorization", "Bearer " + scoutToken))
                .andExpect(status().isOk());
        mockMvc.perform(get("/athletes/{athleteId}", athleteId).header("Authorization", "Bearer " + scoutToken))
                .andExpect(status().isOk());
        mockMvc.perform(get("/notifications").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].type").value("PROFILE_VIEWED"))
                .andExpect(jsonPath("$[0].message").value("Usuario scout@example.com vio tu perfil."));
    }

    private String register(String email, UserRole role, LocalDate birthDate, boolean withGuardian) throws Exception {
        GuardianConsentRequest guardian = withGuardian
                ? new GuardianConsentRequest("Acudiente de Prueba", "acudiente@example.com", true) : null;
        RegisterRequest request = new RegisterRequest(email, "CracksPass123", role, "Usuario " + email,
                birthDate, guardian, true);
        MvcResult result = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        return response.get("accessToken").asText();
    }
}
