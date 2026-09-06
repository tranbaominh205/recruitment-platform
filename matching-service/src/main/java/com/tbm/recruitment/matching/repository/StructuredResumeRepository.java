package com.tbm.recruitment.matching.repository;

import com.tbm.recruitment.matching.document.StructuredResume;
import java.util.UUID;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface StructuredResumeRepository extends MongoRepository<StructuredResume, UUID> {}
