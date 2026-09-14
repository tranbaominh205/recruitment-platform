package com.tbm.recruitment.identity.repository;

import com.tbm.recruitment.identity.entity.InvalidatedToken;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvalidatedTokenRepository extends JpaRepository<InvalidatedToken, String> {}
