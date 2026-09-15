package com.tbm.recruitment.employer.repository;

import com.tbm.recruitment.employer.entity.Company;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface CompanyRepository
    extends JpaRepository<Company, UUID>, JpaSpecificationExecutor<Company> {

  Optional<Company> findByOwnerAccountId(UUID ownerAccountId);

  boolean existsByOwnerAccountId(UUID ownerAccountId);
}
