package com.tbm.recruitment.job.repository;

import com.tbm.recruitment.job.entity.Job;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JobRepository extends JpaRepository<Job, UUID> {}
