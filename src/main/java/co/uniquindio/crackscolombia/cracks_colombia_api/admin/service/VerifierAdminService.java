package co.uniquindio.crackscolombia.cracks_colombia_api.admin.service;

import co.uniquindio.crackscolombia.cracks_colombia_api.admin.dto.request.CreateVerifierRequest;
import co.uniquindio.crackscolombia.cracks_colombia_api.admin.dto.response.VerifierResponse;
import co.uniquindio.crackscolombia.cracks_colombia_api.shared.exception.ApiException;
import co.uniquindio.crackscolombia.cracks_colombia_api.user.entity.UserAccount;
import co.uniquindio.crackscolombia.cracks_colombia_api.user.entity.UserRole;
import co.uniquindio.crackscolombia.cracks_colombia_api.user.repository.UserAccountRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class VerifierAdminService {

    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    public VerifierAdminService(UserAccountRepository userAccountRepository,
                                PasswordEncoder passwordEncoder,
                                Clock clock) {
        this.userAccountRepository = userAccountRepository;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    @Transactional
    public VerifierResponse create(CreateVerifierRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (userAccountRepository.existsByEmail(email)) {
            throw ApiException.emailAlreadyRegistered();
        }
        var verifier = new UserAccount(
                email,
                passwordEncoder.encode(request.temporaryPassword()),
                request.fullName().trim(),
                UserRole.VERIFICADOR,
                null,
                null,
                null,
                false,
                null,
                clock.instant()
        );
        try {
            userAccountRepository.saveAndFlush(verifier);
        } catch (DataIntegrityViolationException exception) {
            throw ApiException.emailAlreadyRegistered();
        }
        return toResponse(verifier);
    }

    @Transactional(readOnly = true)
    public List<VerifierResponse> list() {
        return userAccountRepository.findAllByRoleOrderByCreatedAtAsc(UserRole.VERIFICADOR).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public VerifierResponse deactivate(UUID verifierId) {
        UserAccount verifier = userAccountRepository.findById(verifierId)
                .filter(account -> account.getRole() == UserRole.VERIFICADOR)
                .orElseThrow(() -> ApiException.notFound("No se encontró el verificador."));
        verifier.deactivate();
        return toResponse(userAccountRepository.saveAndFlush(verifier));
    }

    private VerifierResponse toResponse(UserAccount account) {
        return new VerifierResponse(account.getId(), account.getEmail(), account.getFullName(),
                account.getRole(), account.isActive(), account.getCreatedAt());
    }
}
