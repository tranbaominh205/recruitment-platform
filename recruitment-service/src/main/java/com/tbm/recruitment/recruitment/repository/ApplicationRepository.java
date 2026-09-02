package com.tbm.recruitment.recruitment.repository;

import com.tbm.recruitment.recruitment.entity.Application;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApplicationRepository extends JpaRepository<Application, UUID> {}
