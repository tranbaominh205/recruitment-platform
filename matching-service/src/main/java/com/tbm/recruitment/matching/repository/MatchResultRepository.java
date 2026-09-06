package com.tbm.recruitment.matching.repository;

import com.tbm.recruitment.matching.document.MatchResult;
import java.util.UUID;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface MatchResultRepository extends MongoRepository<MatchResult, UUID> {}
