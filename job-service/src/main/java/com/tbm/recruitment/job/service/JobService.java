package com.tbm.recruitment.job.service;

import com.tbm.recruitment.job.client.EmployerClient;
import com.tbm.recruitment.job.dto.request.CreateJobRequest;
import com.tbm.recruitment.job.dto.request.UpdateJobModerationRequest;
import com.tbm.recruitment.job.dto.request.UpdateJobRequest;
import com.tbm.recruitment.job.dto.response.AdminJobStatisticsResponse;
import com.tbm.recruitment.job.dto.response.CompanyModerationStatus;
import com.tbm.recruitment.job.dto.response.CompanySummaryResponse;
import com.tbm.recruitment.job.dto.response.JobResponse;
import com.tbm.recruitment.job.dto.response.PageResponse;
import com.tbm.recruitment.job.entity.Job;
import com.tbm.recruitment.job.entity.JobModerationStatus;
import com.tbm.recruitment.job.entity.JobStatus;
import com.tbm.recruitment.job.enums.JobListChange;
import com.tbm.recruitment.job.event.JobListChangedEvent;
import com.tbm.recruitment.job.exception.AppException;
import com.tbm.recruitment.job.exception.ErrorCode;
import com.tbm.recruitment.job.mapper.JobMapper;
import com.tbm.recruitment.job.repository.JobRepository;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class JobService {

  JobRepository jobRepository;
  JobMapper jobMapper;
  EmployerClient employerClient;
  ApplicationEventPublisher applicationEventPublisher;

  @Transactional
  public JobResponse createJob(
      String accountIdHeader, String accountRole, CreateJobRequest request) {

    UUID accountId = requireRecruiterAccount(accountIdHeader, accountRole);

    validateSalaryRange(request.salaryMin(), request.salaryMax());

    CompanySummaryResponse company = getMyCompany(accountIdHeader, accountRole);
    requireActiveCompanyForMutation(company);

    Job job = jobMapper.toJob(request);

    job.setCompanyId(company.id());
    job.setCreatedByAccountId(accountId);
    job.setStatus(JobStatus.DRAFT);
    job.setModerationStatus(JobModerationStatus.ACTIVE);

    Job savedJob = jobRepository.save(job);
    publishJobListChangedEvent(savedJob, JobListChange.CREATED, false);

    return jobMapper.toJobResponse(savedJob);
  }

  @Transactional
  public JobResponse updateJob(
      UUID jobId, String accountIdHeader, String accountRole, UpdateJobRequest request) {

    requireRecruiterAccount(accountIdHeader, accountRole);

    validateSalaryRange(request.salaryMin(), request.salaryMax());

    CompanySummaryResponse company = getMyCompany(accountIdHeader, accountRole);
    requireActiveCompanyForMutation(company);
    Job job = getOwnedJob(jobId, company.id());

    if (job.getStatus() != JobStatus.DRAFT) {
      throw new AppException(ErrorCode.INVALID_JOB_STATUS);
    }

    jobMapper.updateJob(request, job);

    Job savedJob = jobRepository.save(job);
    publishJobListChangedEvent(savedJob, JobListChange.UPDATED, false);

    return jobMapper.toJobResponse(savedJob);
  }

  @Transactional
  public JobResponse publishJob(UUID jobId, String accountIdHeader, String accountRole) {

    requireRecruiterAccount(accountIdHeader, accountRole);

    CompanySummaryResponse company = getMyCompany(accountIdHeader, accountRole);
    requireActiveCompanyForMutation(company);
    Job job = getOwnedJob(jobId, company.id());

    if (job.getStatus() != JobStatus.DRAFT) {
      throw new AppException(ErrorCode.INVALID_JOB_STATUS);
    }

    boolean wasPubliclyVisible = isPubliclyVisible(job);
    job.setStatus(JobStatus.PUBLISHED);

    Job savedJob = jobRepository.save(job);
    boolean isPubliclyVisible = isPubliclyVisible(savedJob);
    publishJobListChangedEvent(
        savedJob, JobListChange.PUBLISHED, wasPubliclyVisible != isPubliclyVisible);

    return jobMapper.toJobResponse(savedJob);
  }

  @Transactional
  public JobResponse closeJob(UUID jobId, String accountIdHeader, String accountRole) {

    requireRecruiterAccount(accountIdHeader, accountRole);

    CompanySummaryResponse company = getMyCompany(accountIdHeader, accountRole);
    Job job = getOwnedJob(jobId, company.id());

    if (job.getStatus() != JobStatus.PUBLISHED) {
      throw new AppException(ErrorCode.INVALID_JOB_STATUS);
    }

    boolean wasPubliclyVisible = isPubliclyVisible(job);
    job.setStatus(JobStatus.CLOSED);

    Job savedJob = jobRepository.save(job);
    boolean isPubliclyVisible = isPubliclyVisible(savedJob);
    publishJobListChangedEvent(
        savedJob, JobListChange.CLOSED, wasPubliclyVisible != isPubliclyVisible);

    return jobMapper.toJobResponse(savedJob);
  }

  @Transactional(readOnly = true)
  public JobResponse getPublishedJob(UUID jobId) {

    Job job =
        jobRepository.findById(jobId).orElseThrow(() -> new AppException(ErrorCode.JOB_NOT_FOUND));
    if (!isPubliclyVisible(job)) {
      throw new AppException(ErrorCode.JOB_NOT_FOUND);
    }

    return jobMapper.toJobResponse(job);
  }

  @Transactional(readOnly = true)
  public JobResponse getOwnedJobDetails(UUID jobId, String accountIdHeader, String accountRole) {

    requireRecruiterAccount(accountIdHeader, accountRole);

    CompanySummaryResponse company = getMyCompany(accountIdHeader, accountRole);
    Job job = getOwnedJob(jobId, company.id());

    return jobMapper.toJobResponse(job);
  }

  @Transactional(readOnly = true)
  public PageResponse<JobResponse> getMyJobs(
      String accountIdHeader, String accountRole, int page, int size) {

    requireRecruiterAccount(accountIdHeader, accountRole);
    validatePagination(page, size);

    CompanySummaryResponse company = getMyCompany(accountIdHeader, accountRole);

    PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

    Page<Job> jobPage = jobRepository.findAllByCompanyId(company.id(), pageRequest);

    return toPageResponse(jobPage);
  }

  @Transactional(readOnly = true)
  public PageResponse<JobResponse> searchPublishedJobs(
      String keyword,
      String location,
      String employmentType,
      String workplaceType,
      int page,
      int size) {

    validatePagination(page, size);

    Specification<Job> specification =
        (root, query, criteriaBuilder) -> {
          List<Predicate> predicates = new ArrayList<>();

          predicates.add(criteriaBuilder.equal(root.get("status"), JobStatus.PUBLISHED));
          predicates.add(
              criteriaBuilder.or(
                  criteriaBuilder.equal(root.get("moderationStatus"), JobModerationStatus.ACTIVE),
                  criteriaBuilder.isNull(root.get("moderationStatus"))));

          if (StringUtils.hasText(keyword)) {
            predicates.add(
                criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("title")),
                    "%" + keyword.trim().toLowerCase() + "%"));
          }

          if (StringUtils.hasText(location)) {
            predicates.add(
                criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("location")),
                    "%" + location.trim().toLowerCase() + "%"));
          }

          if (StringUtils.hasText(employmentType)) {
            predicates.add(
                criteriaBuilder.equal(
                    criteriaBuilder.lower(root.get("employmentType")),
                    employmentType.trim().toLowerCase()));
          }

          if (StringUtils.hasText(workplaceType)) {
            predicates.add(
                criteriaBuilder.equal(
                    criteriaBuilder.lower(root.get("workplaceType")),
                    workplaceType.trim().toLowerCase()));
          }

          return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };

    PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

    Page<Job> jobPage = jobRepository.findAll(specification, pageRequest);
    List<JobResponse> content =
        jobPage.getContent().stream()
            .filter(this::isPubliclyVisible)
            .map(jobMapper::toJobResponse)
            .toList();
    return new PageResponse<>(
        content,
        jobPage.getNumber(),
        jobPage.getSize(),
        jobPage.getTotalElements(),
        jobPage.getTotalPages());
  }

  @Transactional(readOnly = true)
  public PageResponse<JobResponse> getAdminJobs(
      String accountIdHeader,
      String accountRole,
      String keyword,
      String status,
      String moderationStatus,
      int page,
      int size) {

    requireAdminAccount(accountIdHeader, accountRole);
    validatePagination(page, size);

    JobStatus statusFilter = parseJobStatus(status);
    JobModerationStatus moderationStatusFilter = parseModerationStatus(moderationStatus);

    Specification<Job> specification =
        (root, query, criteriaBuilder) -> {
          List<Predicate> predicates = new ArrayList<>();

          if (StringUtils.hasText(keyword)) {
            predicates.add(
                criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("title")),
                    "%" + keyword.trim().toLowerCase() + "%"));
          }

          if (statusFilter != null) {
            predicates.add(criteriaBuilder.equal(root.get("status"), statusFilter));
          }

          if (moderationStatusFilter != null) {
            if (moderationStatusFilter == JobModerationStatus.ACTIVE) {
              predicates.add(
                  criteriaBuilder.or(
                      criteriaBuilder.equal(
                          root.get("moderationStatus"), JobModerationStatus.ACTIVE),
                      criteriaBuilder.isNull(root.get("moderationStatus"))));
            } else {
              predicates.add(
                  criteriaBuilder.equal(root.get("moderationStatus"), moderationStatusFilter));
            }
          }

          return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };

    PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
    Page<Job> jobPage = jobRepository.findAll(specification, pageRequest);
    List<JobResponse> content =
        jobPage.getContent().stream()
            .filter(job -> matchModerationFilter(job, moderationStatusFilter))
            .map(jobMapper::toJobResponse)
            .toList();
    return new PageResponse<>(
        content,
        jobPage.getNumber(),
        jobPage.getSize(),
        jobPage.getTotalElements(),
        jobPage.getTotalPages());
  }

  @Transactional(readOnly = true)
  public JobResponse getAdminJobById(UUID jobId, String accountIdHeader, String accountRole) {
    requireAdminAccount(accountIdHeader, accountRole);

    Job job =
        jobRepository.findById(jobId).orElseThrow(() -> new AppException(ErrorCode.JOB_NOT_FOUND));
    return jobMapper.toJobResponse(job);
  }

  @Transactional(readOnly = true)
  public AdminJobStatisticsResponse getAdminStatistics(String accountIdHeader, String accountRole) {
    requireAdminAccount(accountIdHeader, accountRole);

    long total = jobRepository.count();
    long draft = jobRepository.countByStatus(JobStatus.DRAFT);
    long published = jobRepository.countByStatus(JobStatus.PUBLISHED);
    long closed = jobRepository.countByStatus(JobStatus.CLOSED);

    return new AdminJobStatisticsResponse(total, draft, published, closed);
  }

  @Transactional
  public JobResponse updateJobModeration(
      UUID jobId,
      String accountIdHeader,
      String accountRole,
      String accountPermissions,
      UpdateJobModerationRequest request) {
    UUID moderatorId =
        requireAdminAccountWithPermission(
            accountIdHeader, accountRole, accountPermissions, "JOB_MODERATE");

    Job job =
        jobRepository.findById(jobId).orElseThrow(() -> new AppException(ErrorCode.JOB_NOT_FOUND));

    boolean wasPubliclyVisible = isPubliclyVisible(job);
    JobModerationStatus nextModerationStatus = request.moderationStatus();
    if (nextModerationStatus == JobModerationStatus.ACTIVE) {
      job.setModerationStatus(JobModerationStatus.ACTIVE);
      job.setModerationReason(null);
      job.setModeratedByAccountId(null);
      job.setModeratedAt(null);
    } else {
      String reason = requireModerationReason(request.reason());
      job.setModerationStatus(nextModerationStatus);
      job.setModerationReason(reason);
      job.setModeratedByAccountId(moderatorId);
      job.setModeratedAt(java.time.Instant.now());
    }

    Job savedJob = jobRepository.save(job);
    boolean isPubliclyVisible = isPubliclyVisible(savedJob);
    publishJobListChangedEvent(
        savedJob, JobListChange.MODERATION_CHANGED, wasPubliclyVisible != isPubliclyVisible);

    return jobMapper.toJobResponse(savedJob);
  }

  private void publishJobListChangedEvent(
      Job job, JobListChange change, boolean publicCatalogChanged) {
    applicationEventPublisher.publishEvent(
        new JobListChangedEvent(
            job.getId(), job.getCreatedByAccountId(), change, publicCatalogChanged));
  }

  private Job getOwnedJob(UUID jobId, UUID companyId) {
    return jobRepository
        .findByIdAndCompanyId(jobId, companyId)
        .orElseThrow(() -> new AppException(ErrorCode.JOB_NOT_FOUND));
  }

  private CompanySummaryResponse getMyCompany(String accountIdHeader, String accountRole) {
    return employerClient.getMyCompany(accountIdHeader, accountRole);
  }

  private void requireActiveCompanyForMutation(CompanySummaryResponse company) {
    if (company.moderationStatus() != CompanyModerationStatus.ACTIVE) {
      throw new AppException(ErrorCode.COMPANY_SUSPENDED);
    }
  }

  private UUID requireRecruiterAccount(String accountIdHeader, String accountRole) {

    if (accountIdHeader == null || accountIdHeader.isBlank()) {
      throw new AppException(ErrorCode.UNAUTHENTICATED);
    }

    if (!"RECRUITER".equals(accountRole)) {
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

  private UUID requireAdminAccountWithPermission(
      String accountIdHeader,
      String accountRole,
      String accountPermissions,
      String requiredPermission) {
    UUID accountId = requireAdminAccount(accountIdHeader, accountRole);
    if (accountPermissions == null || accountPermissions.isBlank()) {
      throw new AppException(ErrorCode.FORBIDDEN);
    }

    Set<String> permissions =
        java.util.Arrays.stream(accountPermissions.split(","))
            .map(String::trim)
            .filter(value -> !value.isBlank())
            .collect(java.util.stream.Collectors.toSet());
    if (!permissions.contains(requiredPermission)) {
      throw new AppException(ErrorCode.FORBIDDEN);
    }
    return accountId;
  }

  private JobStatus parseJobStatus(String status) {
    if (!StringUtils.hasText(status)) {
      return null;
    }

    try {
      return JobStatus.valueOf(status.trim().toUpperCase());
    } catch (IllegalArgumentException exception) {
      throw new AppException(ErrorCode.INVALID_REQUEST);
    }
  }

  private JobModerationStatus parseModerationStatus(String moderationStatus) {
    if (!StringUtils.hasText(moderationStatus)) {
      return null;
    }

    try {
      return JobModerationStatus.valueOf(moderationStatus.trim().toUpperCase());
    } catch (IllegalArgumentException exception) {
      throw new AppException(ErrorCode.INVALID_REQUEST);
    }
  }

  private String requireModerationReason(String reason) {
    if (!StringUtils.hasText(reason)) {
      throw new AppException(ErrorCode.INVALID_REQUEST);
    }
    return reason.trim();
  }

  private boolean isPubliclyVisible(Job job) {
    return job.getStatus() == JobStatus.PUBLISHED
        && (job.getModerationStatus() == null
            || job.getModerationStatus() == JobModerationStatus.ACTIVE);
  }

  private boolean matchModerationFilter(Job job, JobModerationStatus moderationStatusFilter) {
    if (moderationStatusFilter == null) {
      return true;
    }
    if (moderationStatusFilter == JobModerationStatus.ACTIVE) {
      return job.getModerationStatus() == null
          || job.getModerationStatus() == JobModerationStatus.ACTIVE;
    }
    return job.getModerationStatus() == moderationStatusFilter;
  }

  private void validateSalaryRange(java.math.BigDecimal salaryMin, java.math.BigDecimal salaryMax) {

    if (salaryMin != null && salaryMax != null && salaryMax.compareTo(salaryMin) < 0) {

      throw new AppException(ErrorCode.INVALID_SALARY_RANGE);
    }
  }

  private void validatePagination(int page, int size) {

    if (page < 0 || size < 1 || size > 100) {
      throw new AppException(ErrorCode.INVALID_REQUEST);
    }
  }

  private PageResponse<JobResponse> toPageResponse(Page<Job> jobPage) {

    List<JobResponse> content =
        jobPage.getContent().stream().map(jobMapper::toJobResponse).toList();

    return new PageResponse<>(
        content,
        jobPage.getNumber(),
        jobPage.getSize(),
        jobPage.getTotalElements(),
        jobPage.getTotalPages());
  }
}
