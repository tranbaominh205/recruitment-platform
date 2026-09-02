package com.tbm.recruitment.resume.service;

import com.tbm.recruitment.resume.dto.response.ResumeResponse;
import com.tbm.recruitment.resume.entity.Resume;
import com.tbm.recruitment.resume.enums.ResumeStatus;
import com.tbm.recruitment.resume.exception.AppException;
import com.tbm.recruitment.resume.exception.ErrorCode;
import com.tbm.recruitment.resume.mapper.ResumeMapper;
import com.tbm.recruitment.resume.repository.ResumeRepository;
import com.tbm.recruitment.resume.storage.ResumeStorageService;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ResumeService {

  ResumeRepository resumeRepository;
  ResumeMapper resumeMapper;
  ResumeStorageService resumeStorageService;

  public ResumeResponse uploadResume(
      String accountIdHeader, String accountRole, MultipartFile file) {

    UUID ownerAccountId = requireCandidateAccount(accountIdHeader, accountRole);

    validateFile(file);

    UUID resumeId = UUID.randomUUID();

    String originalFileName = resolveOriginalFileName(file.getOriginalFilename());

    String storageKey = ownerAccountId + "/" + resumeId;

    String contentType = file.getContentType();

    if (contentType == null || contentType.isBlank()) {
      contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
    }

    resumeStorageService.upload(storageKey, file);

    Resume resume =
        Resume.builder()
            .id(resumeId)
            .ownerAccountId(ownerAccountId)
            .displayName(originalFileName)
            .originalFileName(originalFileName)
            .contentType(contentType)
            .size(file.getSize())
            .storageKey(storageKey)
            .status(ResumeStatus.ACTIVE)
            .createdAt(Instant.now())
            .build();

    Resume savedResume;

    try {
      savedResume = resumeRepository.save(resume);
    } catch (RuntimeException exception) {
      resumeStorageService.deleteQuietly(storageKey);
      throw exception;
    }

    return resumeMapper.toResumeResponse(savedResume);
  }

  private UUID requireCandidateAccount(String accountIdHeader, String accountRole) {

    if (accountIdHeader == null || accountIdHeader.isBlank()) {
      throw new AppException(ErrorCode.UNAUTHENTICATED);
    }

    if (!"CANDIDATE".equals(accountRole)) {
      throw new AppException(ErrorCode.FORBIDDEN);
    }

    try {
      return UUID.fromString(accountIdHeader);
    } catch (IllegalArgumentException exception) {
      throw new AppException(ErrorCode.UNAUTHENTICATED);
    }
  }

  private void validateFile(MultipartFile file) {

    if (file == null || file.isEmpty() || file.getSize() <= 0) {
      throw new AppException(ErrorCode.INVALID_FILE);
    }
  }

  private String resolveOriginalFileName(String originalFileName) {

    if (originalFileName == null || originalFileName.isBlank()) {
      return "resume-file";
    }

    String normalized = originalFileName.replace('\\', '/');

    int lastSlash = normalized.lastIndexOf('/');

    if (lastSlash >= 0) {
      normalized = normalized.substring(lastSlash + 1);
    }

    if (normalized.isBlank()) {
      return "resume-file";
    }

    return normalized;
  }
}
