package com.tbm.recruitment.job.service;

import com.tbm.recruitment.job.client.EmployerClient;
import com.tbm.recruitment.job.dto.request.CreateJobRequest;
import com.tbm.recruitment.job.dto.response.CompanySummaryResponse;
import com.tbm.recruitment.job.dto.response.JobResponse;
import com.tbm.recruitment.job.entity.Job;
import com.tbm.recruitment.job.entity.JobStatus;
import com.tbm.recruitment.job.exception.AppException;
import com.tbm.recruitment.job.exception.ErrorCode;
import com.tbm.recruitment.job.mapper.JobMapper;
import com.tbm.recruitment.job.repository.JobRepository;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    validateSalaryRange(request);

    CompanySummaryResponse company = employerClient.getMyCompany(accountIdHeader, accountRole);

    Job job = jobMapper.toJob(request);

    job.setCompanyId(company.id());
    job.setCreatedByAccountId(accountId);
    job.setStatus(JobStatus.DRAFT);

    Job savedJob = jobRepository.save(job);

    return jobMapper.toJobResponse(savedJob);
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

  private void validateSalaryRange(CreateJobRequest request) {

    if (request.salaryMin() != null
        && request.salaryMax() != null
        && request.salaryMax().compareTo(request.salaryMin()) < 0) {

      throw new AppException(ErrorCode.INVALID_SALARY_RANGE);
    }
  }
}
