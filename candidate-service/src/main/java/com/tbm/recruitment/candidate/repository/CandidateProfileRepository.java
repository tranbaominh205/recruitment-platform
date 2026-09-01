package com.tbm.recruitment.candidate.repository;

import com.tbm.recruitment.candidate.entity.CandidateProfile;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CandidateProfileRepository extends JpaRepository<CandidateProfile, UUID> {

  Optional<CandidateProfile> findByAccountId(UUID accountId);

  boolean existsByAccountId(UUID accountId);
}
