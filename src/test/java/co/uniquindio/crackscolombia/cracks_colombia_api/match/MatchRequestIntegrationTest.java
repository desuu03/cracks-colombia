package co.uniquindio.crackscolombia.cracks_colombia_api.match;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class MatchRequestIntegrationTest {

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
    void scoutContactsAthleteAndBothSeeTheOutcome() throws Exception {
        String athleteToken = register("juan@example.com", UserRole.DEPORTISTA, LocalDate.now(clock).minusYears(20), false);
        String scoutToken = register("scout@example.com", UserRole.OJEADOR_ENTRENADOR, null, false);
        String athleteId = idOf("juan@example.com");
        mockMvc.perform(put("/users/me").header("Authorization", "Bearer " + scoutToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"club\":\"Academia Cafetera FC\",\"contactPhone\":\"+57 300 555 0142\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.club").value("Academia Cafetera FC"));

        String body = "{\"athleteId\":\"" + athleteId + "\",\"message\":\"Te invitamos a una prueba\"}";
        mockMvc.perform(post("/requests").header("Authorization", "Bearer " + scoutToken)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.code").value("INTEREST_REQUIRED"));

        mockMvc.perform(post("/athletes/{athleteId}/interest", athleteId).header("Authorization", "Bearer " + scoutToken))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/athletes/{athleteId}/interest", athleteId).header("Authorization", "Bearer " + scoutToken))
                .andExpect(status().isCreated());
        mockMvc.perform(get("/athletes/{athleteId}", athleteId).header("Authorization", "Bearer " + scoutToken))
                .andExpect(jsonPath("$.interestedByMe").value(true))
                .andExpect(jsonPath("$.pendingRequestByMe").value(false));

        mockMvc.perform(post("/requests").header("Authorization", "Bearer " + scoutToken)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDIENTE"))
                .andExpect(jsonPath("$.athlete.fullName").value("Usuario juan@example.com"))
                .andExpect(jsonPath("$.contact").doesNotExist());
        mockMvc.perform(post("/requests").header("Authorization", "Bearer " + scoutToken)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("REQUEST_ALREADY_PENDING"));
        mockMvc.perform(get("/athletes/{athleteId}", athleteId).header("Authorization", "Bearer " + scoutToken))
                .andExpect(jsonPath("$.pendingRequestByMe").value(true));

        MvcResult inbox = mockMvc.perform(get("/requests").header("Authorization", "Bearer " + athleteToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].scout.club").value("Academia Cafetera FC"))
                .andReturn();
        String requestId = objectMapper.readTree(inbox.getResponse().getContentAsString()).get(0).get("id").asText();

        mockMvc.perform(patch("/requests/{requestId}/respond", requestId).header("Authorization", "Bearer " + scoutToken)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"decision\":\"ACCEPTED\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(patch("/requests/{requestId}/respond", requestId).header("Authorization", "Bearer " + athleteToken)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"decision\":\"ACCEPTED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACEPTADA"))
                .andExpect(jsonPath("$.contactSharedAt").exists())
                .andExpect(jsonPath("$.contact.phone").value("+57 300 555 0142"))
                .andExpect(jsonPath("$.contact.email").value("scout@example.com"));
        mockMvc.perform(patch("/requests/{requestId}/respond", requestId).header("Authorization", "Bearer " + athleteToken)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"decision\":\"REJECTED\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("REQUEST_ALREADY_ANSWERED"));

        mockMvc.perform(get("/requests").param("status", "ACEPTADA").header("Authorization", "Bearer " + scoutToken))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].contact.email").value("juan@example.com"));

        MvcResult scoutNotifications = mockMvc.perform(get("/notifications").header("Authorization", "Bearer " + scoutToken))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].type").value("REQUEST_ACCEPTED"))
                .andExpect(jsonPath("$[0].read").value(false))
                .andReturn();
        String notificationId = objectMapper.readTree(scoutNotifications.getResponse().getContentAsString())
                .get(0).get("id").asText();
        mockMvc.perform(patch("/notifications/{id}/read", notificationId).header("Authorization", "Bearer " + athleteToken))
                .andExpect(status().isNotFound());
        mockMvc.perform(patch("/notifications/{id}/read", notificationId).header("Authorization", "Bearer " + scoutToken))
                .andExpect(status().isOk());
        mockMvc.perform(get("/notifications").param("unreadOnly", "true").header("Authorization", "Bearer " + scoutToken))
                .andExpect(jsonPath("$", hasSize(0)));

        mockMvc.perform(get("/notifications").header("Authorization", "Bearer " + athleteToken))
                .andExpect(jsonPath("$[0].type").value("REQUEST_CREATED"))
                .andExpect(jsonPath("$[0].message").value("Academia Cafetera FC te envió una solicitud de contacto."));
    }

    @Test
    void minorsAreContactedThroughTheirGuardian() throws Exception {
        String athleteToken = register("menor@example.com", UserRole.DEPORTISTA, LocalDate.now(clock).minusYears(15), true);
        String scoutToken = register("scout@example.com", UserRole.OJEADOR_ENTRENADOR, null, false);
        String athleteId = idOf("menor@example.com");

        mockMvc.perform(post("/athletes/{athleteId}/interest", athleteId).header("Authorization", "Bearer " + scoutToken))
                .andExpect(status().isCreated());
        MvcResult created = mockMvc.perform(post("/requests").header("Authorization", "Bearer " + scoutToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"athleteId\":\"" + athleteId + "\",\"message\":\"Hola\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        String requestId = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asText();
        mockMvc.perform(patch("/requests/{requestId}/respond", requestId).header("Authorization", "Bearer " + athleteToken)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"decision\":\"ACCEPTED\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/requests").header("Authorization", "Bearer " + scoutToken))
                .andExpect(jsonPath("$[0].contact.email").value("acudiente@example.com"));
    }

    @Test
    void rejectsInvalidRequests() throws Exception {
        register("juan@example.com", UserRole.DEPORTISTA, LocalDate.now(clock).minusYears(20), false);
        String scoutToken = register("scout@example.com", UserRole.OJEADOR_ENTRENADOR, null, false);
        String athleteId = idOf("juan@example.com");
        mockMvc.perform(post("/athletes/{athleteId}/interest", athleteId).header("Authorization", "Bearer " + scoutToken))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/requests").header("Authorization", "Bearer " + scoutToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"athleteId\":\"" + athleteId + "\",\"message\":\"   \"}"))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.message").value("Escribe un mensaje para el deportista."));
        mockMvc.perform(post("/requests").header("Authorization", "Bearer " + scoutToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"athleteId\":\"" + athleteId + "\",\"message\":\"Hola\",\"proposedDate\":\"2020-01-01T10:00:00Z\"}"))
                .andExpect(status().is(422));
        mockMvc.perform(post("/athletes/{athleteId}/interest", "00000000-0000-0000-0000-000000000000")
                        .header("Authorization", "Bearer " + scoutToken))
                .andExpect(status().isNotFound());
    }

    private String idOf(String email) {
        return userAccountRepository.findByEmail(email).orElseThrow().getId().toString();
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
