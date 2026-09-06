package com.tbm.recruitment.recruitment.repository;

import com.tbm.recruitment.recruitment.entity.Interview;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InterviewRepository extends JpaRepository<Interview, UUID> {

  Optional<Interview> findByApplicationId(UUID applicationId);

  boolean existsByApplicationId(UUID applicationId);
}
