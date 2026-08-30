package com.tbm.recruitment.identity.repository;

import com.tbm.recruitment.identity.entity.Account;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<Account, UUID> {

  boolean existsByEmailIgnoreCase(String email);

  Optional<Account> findByEmailIgnoreCase(String email);
}
