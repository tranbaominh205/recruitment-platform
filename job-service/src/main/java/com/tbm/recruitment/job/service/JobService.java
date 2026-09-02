package com.tbm.recruitment.job.service;

import com.tbm.recruitment.job.client.EmployerClient;
import com.tbm.recruitment.job.dto.request.CreateJobRequest;
import com.tbm.recruitment.job.dto.request.UpdateJobRequest;
import com.tbm.recruitment.job.dto.response.CompanySummaryResponse;
import com.tbm.recruitment.job.dto.response.JobResponse;
import com.tbm.recruitment.job.dto.response.PageResponse;
import com.tbm.recruitment.job.entity.Job;
import com.tbm.recruitment.job.entity.JobStatus;
import com.tbm.recruitment.job.exception.AppException;
import com.tbm.recruitment.job.exception.ErrorCode;
import com.tbm.recruitment.job.mapper.JobMapper;
import com.tbm.recruitment.job.repository.JobRepository;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
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

  @Transactional
  public JobResponse createJob(
      String accountIdHeader, String accountRole, CreateJobRequest request) {

    UUID accountId = requireRecruiterAccount(accountIdHeader, accountRole);

    validateSalaryRange(request.salaryMin(), request.salaryMax());

    CompanySummaryResponse company = employerClient.getMyCompany(accountIdHeader, accountRole);

    Job job = jobMapper.toJob(request);

    job.setCompanyId(company.id());
    job.setCreatedByAccountId(accountId);
    job.setStatus(JobStatus.DRAFT);

    Job savedJob = jobRepository.save(job);

    return jobMapper.toJobResponse(savedJob);
  }

  @Transactional
  public JobResponse updateJob(
      UUID jobId, String accountIdHeader, String accountRole, UpdateJobRequest request) {

    requireRecruiterAccount(accountIdHeader, accountRole);

    validateSalaryRange(request.salaryMin(), request.salaryMax());

    Job job = getOwnedJob(jobId, accountIdHeader, accountRole);

    if (job.getStatus() != JobStatus.DRAFT) {
      throw new AppException(ErrorCode.INVALID_JOB_STATUS);
    }

    jobMapper.updateJob(request, job);

    Job savedJob = jobRepository.save(job);

    return jobMapper.toJobResponse(savedJob);
  }

  @Transactional
  public JobResponse publishJob(UUID jobId, String accountIdHeader, String accountRole) {

    requireRecruiterAccount(accountIdHeader, accountRole);

    Job job = getOwnedJob(jobId, accountIdHeader, accountRole);

    if (job.getStatus() != JobStatus.DRAFT) {
      throw new AppException(ErrorCode.INVALID_JOB_STATUS);
    }

    job.setStatus(JobStatus.PUBLISHED);

    Job savedJob = jobRepository.save(job);

    return jobMapper.toJobResponse(savedJob);
  }

  @Transactional
  public JobResponse closeJob(UUID jobId, String accountIdHeader, String accountRole) {

    requireRecruiterAccount(accountIdHeader, accountRole);

    Job job = getOwnedJob(jobId, accountIdHeader, accountRole);

    if (job.getStatus() != JobStatus.PUBLISHED) {
      throw new AppException(ErrorCode.INVALID_JOB_STATUS);
    }

    job.setStatus(JobStatus.CLOSED);

    Job savedJob = jobRepository.save(job);

    return jobMapper.toJobResponse(savedJob);
  }

  @Transactional(readOnly = true)
  public JobResponse getPublishedJob(UUID jobId) {

    Job job =
        jobRepository
            .findByIdAndStatus(jobId, JobStatus.PUBLISHED)
            .orElseThrow(() -> new AppException(ErrorCode.JOB_NOT_FOUND));

    return jobMapper.toJobResponse(job);
  }

  @Transactional(readOnly = true)
  public JobResponse getOwnedJobDetails(UUID jobId, String accountIdHeader, String accountRole) {

    requireRecruiterAccount(accountIdHeader, accountRole);

    Job job = getOwnedJob(jobId, accountIdHeader, accountRole);

    return jobMapper.toJobResponse(job);
  }

  @Transactional(readOnly = true)
  public PageResponse<JobResponse> getMyJobs(
      String accountIdHeader, String accountRole, int page, int size) {

    requireRecruiterAccount(accountIdHeader, accountRole);
    validatePagination(page, size);

    CompanySummaryResponse company = employerClient.getMyCompany(accountIdHeader, accountRole);

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

    return toPageResponse(jobPage);
  }

  private Job getOwnedJob(UUID jobId, String accountIdHeader, String accountRole) {

    CompanySummaryResponse company = employerClient.getMyCompany(accountIdHeader, accountRole);

    return jobRepository
        .findByIdAndCompanyId(jobId, company.id())
        .orElseThrow(() -> new AppException(ErrorCode.JOB_NOT_FOUND));
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
