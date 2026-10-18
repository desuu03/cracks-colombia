package co.uniquindio.crackscolombia.cracks_colombia_api.auth.service;

import co.uniquindio.crackscolombia.cracks_colombia_api.auth.dto.request.GuardianConsentRequest;
import co.uniquindio.crackscolombia.cracks_colombia_api.auth.dto.request.LoginRequest;
import co.uniquindio.crackscolombia.cracks_colombia_api.auth.dto.request.RegisterRequest;
import co.uniquindio.crackscolombia.cracks_colombia_api.auth.dto.response.AuthResponse;
import co.uniquindio.crackscolombia.cracks_colombia_api.auth.dto.response.UserProfileResponse;
import co.uniquindio.crackscolombia.cracks_colombia_api.shared.exception.ApiException;
import co.uniquindio.crackscolombia.cracks_colombia_api.shared.security.JwtService;
import co.uniquindio.crackscolombia.cracks_colombia_api.user.entity.UserAccount;
import co.uniquindio.crackscolombia.cracks_colombia_api.user.entity.UserRole;
import co.uniquindio.crackscolombia.cracks_colombia_api.user.repository.UserAccountRepository;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.Period;
import java.util.Locale;
import java.util.Set;

@Service
public class AuthService {

    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final Validator validator;
    private final Clock clock;

    public AuthService(UserAccountRepository userAccountRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       JwtService jwtService,
                       Validator validator,
                       Clock clock) {
        this.userAccountRepository = userAccountRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.validator = validator;
        this.clock = clock;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (request.role() != UserRole.DEPORTISTA
                && request.role() != UserRole.OJEADOR_ENTRENADOR) {
            throw ApiException.invalid("Solo puedes registrarte como deportista u ojeador/entrenador.");
        }

        String email = normalizeEmail(request.email());
        if (userAccountRepository.existsByEmail(email)) {
            throw ApiException.emailAlreadyRegistered();
        }

        boolean minor = false;
        GuardianConsentRequest guardianConsent = null;
        if (request.role() == UserRole.DEPORTISTA) {
            if (request.birthDate() == null) {
                throw ApiException.invalid("La fecha de nacimiento es obligatoria para un deportista.");
            }
            LocalDate today = LocalDate.now(clock);
            if (request.birthDate().isAfter(today)) {
                throw ApiException.invalid("La fecha de nacimiento no puede estar en el futuro.");
            }
            minor = Period.between(request.birthDate(), today).getYears() < 18;
            if (minor) {
                guardianConsent = validateGuardianConsent(request.guardianConsent());
            }
        }

        var now = clock.instant();
        String guardianName = guardianConsent == null ? null : guardianConsent.guardianFullName().trim();
        String guardianEmail = guardianConsent == null ? null : normalizeEmail(guardianConsent.guardianEmail());
        var user = new UserAccount(
                email,
                passwordEncoder.encode(request.password()),
                request.fullName().trim(),
                request.role(),
                request.birthDate(),
                guardianName,
                guardianEmail,
                guardianConsent != null,
                guardianConsent == null ? null : now,
                now
        );

        try {
            userAccountRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException exception) {
            throw ApiException.emailAlreadyRegistered();
        }

        return createAuthResponse(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String email = normalizeEmail(request.email());
        try {
            authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(email, request.password()));
        } catch (AuthenticationException exception) {
            throw ApiException.invalidCredentials();
        }
        UserAccount user = userAccountRepository.findByEmail(email)
                .orElseThrow(ApiException::invalidCredentials);
        return createAuthResponse(user);
    }

    private GuardianConsentRequest validateGuardianConsent(GuardianConsentRequest consent) {
        if (consent == null) {
            throw ApiException.invalid(
                    "El registro de un deportista menor requiere la autorización de un acudiente.");
        }
        Set<ConstraintViolation<GuardianConsentRequest>> violations = validator.validate(consent);
        if (!violations.isEmpty()) {
            String message = violations.stream()
                    .sorted((left, right) -> left.getPropertyPath().toString()
                            .compareTo(right.getPropertyPath().toString()))
                    .findFirst()
                    .orElseThrow()
                    .getMessage();
            throw ApiException.invalid(message);
        }
        if (!Boolean.TRUE.equals(consent.accepted())) {
            throw ApiException.invalid("El acudiente debe aceptar la autorización para registrar al menor.");
        }
        return consent;
    }

    private AuthResponse createAuthResponse(UserAccount user) {
        var profile = new UserProfileResponse(
                user.getId(), user.getEmail(), user.getFullName(), user.getRole(), user.getBirthDate());
        return new AuthResponse(jwtService.createToken(user), "Bearer", profile);
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
