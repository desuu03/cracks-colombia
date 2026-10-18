package co.uniquindio.crackscolombia.cracks_colombia_api.user.repository;

import co.uniquindio.crackscolombia.cracks_colombia_api.user.entity.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserAccountRepository extends JpaRepository<UserAccount, UUID> {

    boolean existsByEmail(String email);

    Optional<UserAccount> findByEmail(String email);
}
