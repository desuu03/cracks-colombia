package co.uniquindio.crackscolombia.cracks_colombia_api.auth;

import co.uniquindio.crackscolombia.cracks_colombia_api.auth.dto.request.GuardianConsentRequest;
import co.uniquindio.crackscolombia.cracks_colombia_api.auth.dto.request.LoginRequest;
import co.uniquindio.crackscolombia.cracks_colombia_api.auth.dto.request.RegisterRequest;
import co.uniquindio.crackscolombia.cracks_colombia_api.user.entity.UserAccount;
import co.uniquindio.crackscolombia.cracks_colombia_api.user.entity.UserRole;
import co.uniquindio.crackscolombia.cracks_colombia_api.user.repository.UserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void clearAccounts() {
        userAccountRepository.deleteAll();
    }

    @Test
    void registersCoachWithSelectedRoleAndStoresPasswordHash() throws Exception {
        RegisterRequest request = registration("coach@example.com", UserRole.OJEADOR_ENTRENADOR,
                null, null, true);

        MvcResult result = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.user.role").value("OJEADOR_ENTRENADOR"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andReturn();

        UserAccount saved = userAccountRepository.findByEmail("coach@example.com").orElseThrow();
        assertNotEquals("CracksPass123", saved.getPasswordHash());
        assertTrue(passwordEncoder.matches("CracksPass123", saved.getPasswordHash()));
        assertTrue(saved.isDataProcessingConsentAccepted());
        assertNotNull(saved.getDataProcessingConsentAcceptedAt());
        assertNotNull(readJson(result).get("accessToken").asText());
    }

    @Test
    void duplicateEmailReturnsClearConflict() throws Exception {
        postRegistration(registration("player@example.com", UserRole.DEPORTISTA,
                LocalDate.now().minusYears(20), null, true))
                .andExpect(status().isCreated());

        postRegistration(registration("PLAYER@example.com", UserRole.OJEADOR_ENTRENADOR,
                null, null, true))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_ALREADY_REGISTERED"))
                .andExpect(jsonPath("$.message").value("Este correo ya está registrado."));
    }

    @Test
    void minorAthleteRequiresGuardianAndDataProcessingConsent() throws Exception {
        RegisterRequest withoutGuardian = registration("minor@example.com", UserRole.DEPORTISTA,
                LocalDate.now().minusYears(16), null, true);

        postRegistration(withoutGuardian)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value(
                        "El registro de un deportista menor requiere la autorización de un acudiente."));

        RegisterRequest withoutPolicy = registration("minor@example.com", UserRole.DEPORTISTA,
                LocalDate.now().minusYears(16),
                new GuardianConsentRequest("Acudiente", "guardian@example.com", true), false);
        postRegistration(withoutPolicy)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").exists());

        RegisterRequest withoutGuardianApproval = registration("minor@example.com", UserRole.DEPORTISTA,
                LocalDate.now().minusYears(16),
                new GuardianConsentRequest("Acudiente", "guardian@example.com", false), true);
        postRegistration(withoutGuardianApproval)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value(
                        "El acudiente debe aceptar la autorización para registrar al menor."));

        assertFalse(userAccountRepository.existsByEmail("minor@example.com"));
    }

    @Test
    void minorAthleteCanRegisterAfterBothConsentsAreAccepted() throws Exception {
        GuardianConsentRequest guardian = new GuardianConsentRequest(
                "Acudiente Responsable", "guardian@example.com", true);
        postRegistration(registration("minor@example.com", UserRole.DEPORTISTA,
                LocalDate.now().minusYears(16), guardian, true))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.user.role").value("DEPORTISTA"));

        UserAccount saved = userAccountRepository.findByEmail("minor@example.com").orElseThrow();
        assertTrue(saved.isGuardianConsentAccepted());
        assertTrue(saved.isDataProcessingConsentAccepted());
        assertEquals("guardian@example.com", saved.getGuardianEmail());
        assertNotNull(saved.getGuardianConsentAcceptedAt());
    }

    @Test
    void loginReturnsTokenForValidCredentialsAndRejectsInvalidCredentials() throws Exception {
        postRegistration(registration("login@example.com", UserRole.OJEADOR_ENTRENADOR,
                null, null, true)).andExpect(status().isCreated());

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(
                                new LoginRequest("LOGIN@example.com", "CracksPass123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.user.role").value("OJEADOR_ENTRENADOR"));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(
                                new LoginRequest("login@example.com", "incorrecta"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    void athleteAndCoachTokensCannotUseTheOtherRolesRoutes() throws Exception {
        String athleteToken = registerAndGetToken(registration("athlete@example.com", UserRole.DEPORTISTA,
                LocalDate.now().minusYears(20), null, true));
        String coachToken = registerAndGetToken(registration("scout@example.com",
                UserRole.OJEADOR_ENTRENADOR, null, null, true));

        mockMvc.perform(post("/requests")
                        .header("Authorization", "Bearer " + athleteToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        mockMvc.perform(post("/athletes/{athleteId}/videos", UUID.randomUUID())
                        .header("Authorization", "Bearer " + coachToken)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void protectedRoutesRequireAValidBearerToken() throws Exception {
        mockMvc.perform(post("/requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    private RegisterRequest registration(String email, UserRole role, LocalDate birthDate,
                                         GuardianConsentRequest guardianConsent,
                                         Boolean dataProcessingConsent) {
        return new RegisterRequest(email, "CracksPass123", role, "Usuario de Prueba",
                birthDate, guardianConsent, dataProcessingConsent);
    }

    private org.springframework.test.web.servlet.ResultActions postRegistration(RegisterRequest request)
            throws Exception {
        return mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(request)));
    }

    private String registerAndGetToken(RegisterRequest request) throws Exception {
        MvcResult result = postRegistration(request).andExpect(status().isCreated()).andReturn();
        JsonNode response = readJson(result);
        return response.get("accessToken").asText();
    }

    private JsonNode readJson(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }
}
