package com.tbm.recruitment.resume.repository;

import com.tbm.recruitment.resume.entity.Resume;
import com.tbm.recruitment.resume.enums.ResumeStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ResumeRepository extends MongoRepository<Resume, UUID> {

  List<Resume> findAllByOwnerAccountIdOrderByCreatedAtDesc(UUID ownerAccountId);

  Optional<Resume> findByIdAndOwnerAccountId(UUID id, UUID ownerAccountId);

  Page<Resume> findAllByStatus(ResumeStatus status, Pageable pageable);

  Page<Resume> findAllByOwnerAccountId(UUID ownerAccountId, Pageable pageable);

  Page<Resume> findAllByStatusAndOwnerAccountId(
      ResumeStatus status, UUID ownerAccountId, Pageable pageable);

  long countByStatus(ResumeStatus status);
}
