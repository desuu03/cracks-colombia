package co.uniquindio.crackscolombia.cracks_colombia_api.shared.config;

import co.uniquindio.crackscolombia.cracks_colombia_api.user.entity.UserRole;
import jakarta.persistence.EntityManagerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.Connection;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Keeps the CHECK constraint on user_accounts.role in line with {@link UserRole}.
 *
 * <p>Hibernate writes that constraint when it creates the table, and ddl-auto=update never refreshes it.
 * After MODERADOR was renamed to VERIFICADOR, databases created before the rename rejected every new
 * verifier. This runs once at startup, before the web server accepts requests, and does nothing when the
 * constraint already lists every role. Only PostgreSQL needs it; the H2 test database is created from
 * scratch on each run.
 */
@Component
public class RoleConstraintMigration implements InitializingBean {

    private static final Logger log = LoggerFactory.getLogger(RoleConstraintMigration.class);
    private static final String CONSTRAINT = "user_accounts_role_check";

    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;

    // EntityManagerFactory is injected only so this runs after Hibernate has updated the schema.
    public RoleConstraintMigration(JdbcTemplate jdbcTemplate,
                                   TransactionTemplate transactionTemplate,
                                   EntityManagerFactory entityManagerFactory) {
        this.jdbcTemplate = jdbcTemplate;
        this.transactionTemplate = transactionTemplate;
    }

    @Override
    public void afterPropertiesSet() {
        if (!isPostgres()) {
            return;
        }
        List<String> definitions = jdbcTemplate.queryForList(
                "SELECT pg_get_constraintdef(oid) FROM pg_constraint WHERE conname = ?", String.class, CONSTRAINT);
        if (definitions.isEmpty() || listsEveryRole(definitions.getFirst())) {
            return;
        }
        String allowed = Arrays.stream(UserRole.values())
                .map(role -> "'" + role.name() + "'")
                .collect(Collectors.joining(", "));
        Integer renamed = transactionTemplate.execute(status -> {
            jdbcTemplate.execute("ALTER TABLE user_accounts DROP CONSTRAINT " + CONSTRAINT);
            int rows = jdbcTemplate.update("UPDATE user_accounts SET role = 'VERIFICADOR' WHERE role = 'MODERADOR'");
            jdbcTemplate.execute("ALTER TABLE user_accounts ADD CONSTRAINT " + CONSTRAINT
                    + " CHECK (role IN (" + allowed + "))");
            return rows;
        });
        log.info("Restricción {} actualizada a ({}). Cuentas MODERADOR pasadas a VERIFICADOR: {}.",
                CONSTRAINT, allowed, renamed);
    }

    private boolean isPostgres() {
        String product = jdbcTemplate.execute(
                (Connection connection) -> connection.getMetaData().getDatabaseProductName());
        return "PostgreSQL".equalsIgnoreCase(product);
    }

    private static boolean listsEveryRole(String definition) {
        return Arrays.stream(UserRole.values()).allMatch(role -> definition.contains("'" + role.name() + "'"));
    }
}
