package co.uniquindio.crackscolombia.cracks_colombia_api.admin;

import co.uniquindio.crackscolombia.cracks_colombia_api.admin.dto.request.CreateVerifierRequest;
import co.uniquindio.crackscolombia.cracks_colombia_api.admin.service.AdminBootstrap;
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
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "app.admin.bootstrap.email=Admin@Example.com",
        "app.admin.bootstrap.password=AdminPass123",
        "app.admin.bootstrap.full-name=Ana Administradora"
})
class AdminVerifierIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AdminBootstrap adminBootstrap;

    private String adminToken;

    @BeforeEach
    void bootstrapAdmin() throws Exception {
        userAccountRepository.deleteAll();
        adminBootstrap.run(null);
        adminToken = login("admin@example.com", "AdminPass123")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.role").value("ADMIN"))
                .andReturn().getResponse().getContentAsString().transform(this::accessToken);
    }

    @Test
    void bootstrapCreatesTheFirstAdminOnlyOnce() {
        adminBootstrap.run(null);

        var admins = userAccountRepository.findAllByRoleOrderByCreatedAtAsc(UserRole.ADMIN);
        assertEquals(1, admins.size());
        assertEquals("Ana Administradora", admins.getFirst().getFullName());
    }

    @Test
    void adminCreatesAndListsVerifiers() throws Exception {
        createVerifier("Verificador@Example.com", "Laura Verificadora")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("verificador@example.com"))
                .andExpect(jsonPath("$.fullName").value("Laura Verificadora"))
                .andExpect(jsonPath("$.role").value("VERIFICADOR"))
                .andExpect(jsonPath("$.active").value(true));

        UserAccount saved = userAccountRepository.findByEmail("verificador@example.com").orElseThrow();
        assertEquals(UserRole.VERIFICADOR, saved.getRole());
        assertTrue(passwordEncoder.matches("TempPass123", saved.getPasswordHash()));

        mockMvc.perform(get("/admin/verifiers").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].email").value("verificador@example.com"));

        String verifierToken = accessToken(login("verificador@example.com", "TempPass123")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.role").value("VERIFICADOR"))
                .andReturn().getResponse().getContentAsString());
        mockMvc.perform(get("/moderation/queue").header("Authorization", "Bearer " + verifierToken))
                .andExpect(status().isOk());
    }

    @Test
    void creatingAVerifierRejectsDuplicateEmailsAndInvalidData() throws Exception {
        createVerifier("verificador@example.com", "Laura Verificadora").andExpect(status().isCreated());

        createVerifier("verificador@example.com", "Otra Persona")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_ALREADY_REGISTERED"));

        mockMvc.perform(post("/admin/verifiers")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(
                                new CreateVerifierRequest("Laura", "nuevo@example.com", "corta"))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @Test
    void deactivatedVerifierCannotLogInOrModerate() throws Exception {
        String verifierId = createVerifier("verificador@example.com", "Laura Verificadora")
                .andReturn().getResponse().getContentAsString().transform(this::id);
        String verifierToken = accessToken(login("verificador@example.com", "TempPass123")
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());

        mockMvc.perform(patch("/admin/verifiers/{id}/deactivate", verifierId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        assertFalse(userAccountRepository.findByEmail("verificador@example.com").orElseThrow().isActive());
        login("verificador@example.com", "TempPass123")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
        mockMvc.perform(get("/moderation/queue").header("Authorization", "Bearer " + verifierToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/admin/verifiers").header("Authorization", "Bearer " + adminToken))
                .andExpect(jsonPath("$[0].active").value(false));
    }

    @Test
    void onlyVerifierAccountsCanBeDeactivatedThroughThisEndpoint() throws Exception {
        UUID adminId = userAccountRepository.findByEmail("admin@example.com").orElseThrow().getId();

        mockMvc.perform(patch("/admin/verifiers/{id}/deactivate", adminId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
        mockMvc.perform(patch("/admin/verifiers/{id}/deactivate", UUID.randomUUID())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void nonAdminRolesReceiveForbiddenOnAdminEndpoints() throws Exception {
        createVerifier("verificador@example.com", "Laura Verificadora").andExpect(status().isCreated());
        String verifierToken = accessToken(login("verificador@example.com", "TempPass123")
                .andReturn().getResponse().getContentAsString());
        String athleteToken = register("deportista@example.com", UserRole.DEPORTISTA, LocalDate.now().minusYears(20));
        String scoutToken = register("ojeador@example.com", UserRole.OJEADOR_ENTRENADOR, null);

        for (String token : new String[]{verifierToken, athleteToken, scoutToken}) {
            mockMvc.perform(get("/admin/verifiers").header("Authorization", "Bearer " + token))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("FORBIDDEN"));
            mockMvc.perform(post("/admin/verifiers")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsBytes(new CreateVerifierRequest(
                                    "Intruso", "intruso@example.com", "TempPass123"))))
                    .andExpect(status().isForbidden());
            mockMvc.perform(patch("/admin/verifiers/{id}/deactivate", UUID.randomUUID())
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isForbidden());
        }
        assertFalse(userAccountRepository.existsByEmail("intruso@example.com"));

        mockMvc.perform(get("/admin/verifiers"))
                .andExpect(status().isUnauthorized());
    }

    private ResultActions createVerifier(String email, String fullName) throws Exception {
        return mockMvc.perform(post("/admin/verifiers")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(
                        new CreateVerifierRequest(fullName, email, "TempPass123"))));
    }

    private ResultActions login(String email, String password) throws Exception {
        return mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(new LoginRequest(email, password))));
    }

    private String register(String email, UserRole role, LocalDate birthDate) throws Exception {
        MvcResult result = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(new RegisterRequest(
                                email, "CracksPass123", role, "Usuario de Prueba", birthDate, null, true))))
                .andExpect(status().isCreated())
                .andReturn();
        return accessToken(result.getResponse().getContentAsString());
    }

    private String accessToken(String json) {
        return objectMapper.readTree(json).get("accessToken").asText();
    }

    private String id(String json) {
        return objectMapper.readTree(json).get("id").asText();
    }
}
