package com.tbm.recruitment.job.repository;

import com.tbm.recruitment.job.entity.Job;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface JobRepository extends JpaRepository<Job, UUID>, JpaSpecificationExecutor<Job> {

  Optional<Job> findByIdAndCompanyId(UUID id, UUID companyId);

  Page<Job> findAllByCompanyId(UUID companyId, Pageable pageable);
}
