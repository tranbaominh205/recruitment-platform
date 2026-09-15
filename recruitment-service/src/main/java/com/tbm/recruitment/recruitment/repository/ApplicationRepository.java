package com.tbm.recruitment.recruitment.repository;

import com.tbm.recruitment.recruitment.entity.Application;
import com.tbm.recruitment.recruitment.enums.ApplicationStatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ApplicationRepository
    extends JpaRepository<Application, UUID>, JpaSpecificationExecutor<Application> {

  Page<Application> findAllByCandidateId(UUID candidateId, Pageable pageable);

  Page<Application> findAllByJobId(UUID jobId, Pageable pageable);

  Optional<Application> findByIdAndCandidateId(UUID id, UUID candidateId);

  long countByStatus(ApplicationStatus status);
}
