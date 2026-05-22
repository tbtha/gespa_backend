package com.tbtha.gespa_backend.repositories;

import com.tbtha.gespa_backend.entities.ProfessionalInvitationToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.Optional;

public interface ProfessionalInvitationTokenRepository extends JpaRepository<ProfessionalInvitationToken, Long> {

    Optional<ProfessionalInvitationToken> findByTokenHash(String tokenHash);

    boolean existsByUser_IdAndUsedFalseAndExpiresAtAfter(Long userId, OffsetDateTime now);

    void deleteByUser_Id(Long userId);
}
