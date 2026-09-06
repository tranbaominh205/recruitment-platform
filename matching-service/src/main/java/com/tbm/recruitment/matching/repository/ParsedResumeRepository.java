package com.tbm.recruitment.matching.repository;

import com.tbm.recruitment.matching.document.ParsedResume;
import java.util.UUID;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ParsedResumeRepository extends MongoRepository<ParsedResume, UUID> {}
