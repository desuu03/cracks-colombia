package co.uniquindio.crackscolombia.cracks_colombia_api.notification.repository;

import co.uniquindio.crackscolombia.cracks_colombia_api.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    List<Notification> findTop50ByRecipient_IdOrderByCreatedAtDesc(UUID recipientId);

    List<Notification> findTop50ByRecipient_IdAndReadFalseOrderByCreatedAtDesc(UUID recipientId);

    Optional<Notification> findByIdAndRecipient_Id(UUID id, UUID recipientId);
}
