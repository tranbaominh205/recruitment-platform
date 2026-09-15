package com.tbm.recruitment.identity.repository;

import com.tbm.recruitment.identity.entity.Account;
import com.tbm.recruitment.identity.entity.Role;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface AccountRepository
    extends JpaRepository<Account, UUID>, JpaSpecificationExecutor<Account> {

  boolean existsByEmailIgnoreCase(String email);

  Optional<Account> findByEmailIgnoreCase(String email);

  long countByRole(Role role);

  long countByEnabled(boolean enabled);
}
