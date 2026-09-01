package com.tbm.recruitment.employer.repository;

import com.tbm.recruitment.employer.entity.Company;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanyRepository extends JpaRepository<Company, UUID> {

  Optional<Company> findByOwnerAccountId(UUID ownerAccountId);

  boolean existsByOwnerAccountId(UUID ownerAccountId);
}
