package com.tbm.recruitment.recruitment.repository;

import com.tbm.recruitment.recruitment.entity.Application;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApplicationRepository extends JpaRepository<Application, UUID> {

  Page<Application> findAllByCandidateId(UUID candidateId, Pageable pageable);

  Page<Application> findAllByJobId(UUID jobId, Pageable pageable);

  Optional<Application> findByIdAndCandidateId(UUID id, UUID candidateId);
}
