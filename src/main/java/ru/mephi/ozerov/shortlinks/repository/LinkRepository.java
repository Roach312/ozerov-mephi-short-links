package ru.mephi.ozerov.shortlinks.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.mephi.ozerov.shortlinks.entity.Link;

public interface LinkRepository extends JpaRepository<Link, Long> {

    Optional<Link> findByShortCode(String shortCode);

    boolean existsByShortCode(String shortCode);

    List<Link> findByUserIdOrderByCreatedAtDesc(UUID userId);

    @Query("SELECT l FROM Link l WHERE l.expiresAt < :now AND l.active = true")
    List<Link> findExpiredActiveLinks(Instant now);
}
