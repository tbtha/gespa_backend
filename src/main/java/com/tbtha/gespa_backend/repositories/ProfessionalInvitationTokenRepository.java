package com.tbtha.gespa_backend.repositories;

import com.tbtha.gespa_backend.entities.ProfessionalInvitationToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProfessionalInvitationTokenRepository extends JpaRepository<ProfessionalInvitationToken, Long> {

    Optional<ProfessionalInvitationToken> findByTokenHash(String tokenHash);

    void deleteByUser_Id(Long userId);
}
