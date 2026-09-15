package com.tbm.recruitment.resume.service;

import com.tbm.recruitment.resume.dto.response.AdminResumeResponse;
import com.tbm.recruitment.resume.dto.response.AdminResumeStatisticsResponse;
import com.tbm.recruitment.resume.dto.response.PageResponse;
import com.tbm.recruitment.resume.dto.response.ResumeDownloadResponse;
import com.tbm.recruitment.resume.dto.response.ResumeResponse;
import com.tbm.recruitment.resume.entity.Resume;
import com.tbm.recruitment.resume.enums.ResumeStatus;
import com.tbm.recruitment.resume.exception.AppException;
import com.tbm.recruitment.resume.exception.ErrorCode;
import com.tbm.recruitment.resume.mapper.ResumeMapper;
import com.tbm.recruitment.resume.repository.ResumeRepository;
import com.tbm.recruitment.resume.storage.ResumeStorageService;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
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

  public List<ResumeResponse> getMyResumes(String accountIdHeader, String accountRole) {

    UUID ownerAccountId = requireCandidateAccount(accountIdHeader, accountRole);

    return resumeRepository.findAllByOwnerAccountIdOrderByCreatedAtDesc(ownerAccountId).stream()
        .map(resumeMapper::toResumeResponse)
        .toList();
  }

  public ResumeResponse getMyResume(UUID resumeId, String accountIdHeader, String accountRole) {

    UUID ownerAccountId = requireCandidateAccount(accountIdHeader, accountRole);

    Resume resume = findOwnedResume(resumeId, ownerAccountId);

    return resumeMapper.toResumeResponse(resume);
  }

  public ResumeDownloadResponse downloadMyResume(
      UUID resumeId, String accountIdHeader, String accountRole) {

    UUID ownerAccountId = requireCandidateAccount(accountIdHeader, accountRole);

    Resume resume = findOwnedResume(resumeId, ownerAccountId);

    byte[] content = resumeStorageService.download(resume.getStorageKey());

    return new ResumeDownloadResponse(
        resume.getOriginalFileName(), resume.getContentType(), content);
  }

  public ResumeDownloadResponse getResumeContentForInternalUse(UUID resumeId) {

    Resume resume =
        resumeRepository
            .findById(resumeId)
            .orElseThrow(() -> new AppException(ErrorCode.RESUME_NOT_FOUND));

    byte[] content = resumeStorageService.download(resume.getStorageKey());

    return new ResumeDownloadResponse(
        resume.getOriginalFileName(), resume.getContentType(), content);
  }

  public PageResponse<AdminResumeResponse> getAdminResumes(
      String accountIdHeader,
      String accountRole,
      String status,
      String ownerAccountId,
      int page,
      int size) {

    requireAdminAccount(accountIdHeader, accountRole);
    validatePagination(page, size);

    ResumeStatus statusFilter = parseResumeStatus(status);
    UUID ownerFilter = parseOwnerAccountId(ownerAccountId);

    PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
    Page<Resume> resumePage;

    if (statusFilter != null && ownerFilter != null) {
      resumePage =
          resumeRepository.findAllByStatusAndOwnerAccountId(statusFilter, ownerFilter, pageRequest);
    } else if (statusFilter != null) {
      resumePage = resumeRepository.findAllByStatus(statusFilter, pageRequest);
    } else if (ownerFilter != null) {
      resumePage = resumeRepository.findAllByOwnerAccountId(ownerFilter, pageRequest);
    } else {
      resumePage = resumeRepository.findAll(pageRequest);
    }

    return new PageResponse<>(
        resumePage.getContent().stream().map(this::toAdminResumeResponse).toList(),
        resumePage.getNumber(),
        resumePage.getSize(),
        resumePage.getTotalElements(),
        resumePage.getTotalPages());
  }

  public AdminResumeResponse getAdminResumeById(
      UUID resumeId, String accountIdHeader, String accountRole) {

    requireAdminAccount(accountIdHeader, accountRole);

    Resume resume =
        resumeRepository
            .findById(resumeId)
            .orElseThrow(() -> new AppException(ErrorCode.RESUME_NOT_FOUND));

    return toAdminResumeResponse(resume);
  }

  public AdminResumeStatisticsResponse getAdminStatistics(
      String accountIdHeader, String accountRole) {

    requireAdminAccount(accountIdHeader, accountRole);

    long total = resumeRepository.count();
    long active = resumeRepository.countByStatus(ResumeStatus.ACTIVE);
    long archived = resumeRepository.countByStatus(ResumeStatus.ARCHIVED);

    return new AdminResumeStatisticsResponse(total, active, archived);
  }

  private Resume findOwnedResume(UUID resumeId, UUID ownerAccountId) {

    return resumeRepository
        .findByIdAndOwnerAccountId(resumeId, ownerAccountId)
        .orElseThrow(() -> new AppException(ErrorCode.RESUME_NOT_FOUND));
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

  private UUID requireAdminAccount(String accountIdHeader, String accountRole) {

    if (accountIdHeader == null || accountIdHeader.isBlank()) {
      throw new AppException(ErrorCode.UNAUTHENTICATED);
    }

    if (!"ADMIN".equals(accountRole)) {
      throw new AppException(ErrorCode.FORBIDDEN);
    }

    try {
      return UUID.fromString(accountIdHeader);
    } catch (IllegalArgumentException exception) {
      throw new AppException(ErrorCode.UNAUTHENTICATED);
    }
  }

  private ResumeStatus parseResumeStatus(String status) {
    if (!StringUtils.hasText(status)) {
      return null;
    }

    try {
      return ResumeStatus.valueOf(status.trim().toUpperCase());
    } catch (IllegalArgumentException exception) {
      throw new AppException(ErrorCode.INVALID_REQUEST);
    }
  }

  private UUID parseOwnerAccountId(String ownerAccountId) {
    if (!StringUtils.hasText(ownerAccountId)) {
      return null;
    }

    try {
      return UUID.fromString(ownerAccountId.trim());
    } catch (IllegalArgumentException exception) {
      throw new AppException(ErrorCode.INVALID_REQUEST);
    }
  }

  private void validatePagination(int page, int size) {
    if (page < 0 || size < 1 || size > 100) {
      throw new AppException(ErrorCode.INVALID_REQUEST);
    }
  }

  private AdminResumeResponse toAdminResumeResponse(Resume resume) {
    return new AdminResumeResponse(
        resume.getId(),
        resume.getOwnerAccountId(),
        resume.getDisplayName(),
        resume.getOriginalFileName(),
        resume.getContentType(),
        resume.getSize(),
        resume.getStatus(),
        resume.getCreatedAt());
  }

  private void validateFile(MultipartFile file) {

    if (file == null || file.isEmpty() || file.getSize() <= 0) {
      throw new AppException(ErrorCode.INVALID_FILE);
    }

    String originalFileName = resolveOriginalFileName(file.getOriginalFilename());
    if (originalFileName == null
        || originalFileName.isBlank()
        || !originalFileName.toLowerCase(Locale.ROOT).endsWith(".pdf")) {
      throw new AppException(ErrorCode.INVALID_FILE);
    }

    String contentType = file.getContentType();
    if (!"application/pdf".equalsIgnoreCase(contentType == null ? "" : contentType.trim())) {
      throw new AppException(ErrorCode.INVALID_FILE);
    }

    try (InputStream inputStream = file.getInputStream()) {
      byte[] header = inputStream.readNBytes(5);
      String pdfSignature = new String(header, StandardCharsets.US_ASCII);
      if (!"%PDF-".equals(pdfSignature)) {
        throw new AppException(ErrorCode.INVALID_FILE);
      }
    } catch (IOException exception) {
      throw new AppException(ErrorCode.INVALID_FILE, exception);
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
