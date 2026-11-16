package co.uniquindio.crackscolombia.cracks_colombia_api.admin.service;

import co.uniquindio.crackscolombia.cracks_colombia_api.user.entity.UserAccount;
import co.uniquindio.crackscolombia.cracks_colombia_api.user.entity.UserRole;
import co.uniquindio.crackscolombia.cracks_colombia_api.user.repository.UserAccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Locale;

/**
 * Creates the first administrator from environment variables. The public API never creates admins,
 * so this runs only while no ADMIN account exists yet.
 */
@Component
public class AdminBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;
    private final String email;
    private final String password;
    private final String fullName;

    public AdminBootstrap(UserAccountRepository userAccountRepository,
                          PasswordEncoder passwordEncoder,
                          Clock clock,
                          @Value("${app.admin.bootstrap.email:}") String email,
                          @Value("${app.admin.bootstrap.password:}") String password,
                          @Value("${app.admin.bootstrap.full-name:Administrador}") String fullName) {
        this.userAccountRepository = userAccountRepository;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
        this.email = email;
        this.password = password;
        this.fullName = fullName;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userAccountRepository.existsByRole(UserRole.ADMIN)) {
            return;
        }
        if (email.isBlank() || password.isBlank()) {
            log.warn("No existe un administrador. Define ADMIN_BOOTSTRAP_EMAIL y ADMIN_BOOTSTRAP_PASSWORD para crearlo.");
            return;
        }
        if (password.length() < 8) {
            throw new IllegalStateException("ADMIN_BOOTSTRAP_PASSWORD debe tener al menos 8 caracteres.");
        }
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        if (userAccountRepository.existsByEmail(normalizedEmail)) {
            throw new IllegalStateException(
                    "ADMIN_BOOTSTRAP_EMAIL ya pertenece a una cuenta que no es de administrador.");
        }
        userAccountRepository.save(new UserAccount(
                normalizedEmail,
                passwordEncoder.encode(password),
                fullName.trim(),
                UserRole.ADMIN,
                null,
                null,
                null,
                false,
                null,
                clock.instant()
        ));
        log.info("Administrador inicial creado: {}", normalizedEmail);
    }
}
