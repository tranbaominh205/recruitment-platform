package com.tbm.recruitment.resume.repository;

import com.tbm.recruitment.resume.entity.Resume;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ResumeRepository extends MongoRepository<Resume, UUID> {

  List<Resume> findAllByOwnerAccountIdOrderByCreatedAtDesc(UUID ownerAccountId);

  Optional<Resume> findByIdAndOwnerAccountId(UUID id, UUID ownerAccountId);
}
