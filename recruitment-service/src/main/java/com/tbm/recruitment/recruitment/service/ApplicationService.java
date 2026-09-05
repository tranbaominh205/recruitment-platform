package com.tbm.recruitment.recruitment.service;

import com.tbm.recruitment.recruitment.client.CandidateClient;
import com.tbm.recruitment.recruitment.client.JobClient;
import com.tbm.recruitment.recruitment.client.ResumeClient;
import com.tbm.recruitment.recruitment.dto.request.CreateApplicationRequest;
import com.tbm.recruitment.recruitment.dto.request.UpdateApplicationStatusRequest;
import com.tbm.recruitment.recruitment.dto.response.ApplicationResponse;
import com.tbm.recruitment.recruitment.dto.response.CandidateSummaryResponse;
import com.tbm.recruitment.recruitment.dto.response.JobSummaryResponse;
import com.tbm.recruitment.recruitment.dto.response.PageResponse;
import com.tbm.recruitment.recruitment.dto.response.ResumeSummaryResponse;
import com.tbm.recruitment.recruitment.entity.Application;
import com.tbm.recruitment.recruitment.enums.ApplicationStatus;
import com.tbm.recruitment.recruitment.exception.AppException;
import com.tbm.recruitment.recruitment.exception.ErrorCode;
import com.tbm.recruitment.recruitment.mapper.ApplicationMapper;
import com.tbm.recruitment.recruitment.repository.ApplicationRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ApplicationService {

  ApplicationRepository applicationRepository;
  ApplicationMapper applicationMapper;

  CandidateClient candidateClient;
  ResumeClient resumeClient;
  JobClient jobClient;

  public ApplicationResponse submitApplication(
      String accountIdHeader, String accountRole, CreateApplicationRequest request) {

    requireCandidateAccount(accountIdHeader, accountRole);

    CandidateSummaryResponse candidate = candidateClient.getMyProfile(accountIdHeader, accountRole);

    ResumeSummaryResponse resume =
        resumeClient.getMyResume(request.resumeId(), accountIdHeader, accountRole);

    JobSummaryResponse job = jobClient.getPublishedJob(request.jobId());

    Application application =
        Application.builder()
            .candidateId(candidate.id())
            .jobId(job.id())
            .resumeId(resume.id())
            .status(ApplicationStatus.SUBMITTED)
            .submittedAt(Instant.now())
            .build();

    Application savedApplication = applicationRepository.save(application);

    return applicationMapper.toApplicationResponse(savedApplication);
  }

  public PageResponse<ApplicationResponse> getMyApplications(
      String accountIdHeader, String accountRole, int page, int size) {

    requireCandidateAccount(accountIdHeader, accountRole);

    validatePagination(page, size);

    CandidateSummaryResponse candidate = candidateClient.getMyProfile(accountIdHeader, accountRole);

    PageRequest pageRequest =
        PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "submittedAt"));

    Page<Application> applicationPage =
        applicationRepository.findAllByCandidateId(candidate.id(), pageRequest);

    return toPageResponse(applicationPage);
  }

  public PageResponse<ApplicationResponse> getApplicationsForOwnedJob(
      UUID jobId, String accountIdHeader, String accountRole, int page, int size) {

    requireRecruiterAccount(accountIdHeader, accountRole);

    validatePagination(page, size);

    jobClient.getOwnedJob(jobId, accountIdHeader, accountRole);

    PageRequest pageRequest =
        PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "submittedAt"));

    Page<Application> applicationPage = applicationRepository.findAllByJobId(jobId, pageRequest);

    return toPageResponse(applicationPage);
  }

  public ApplicationResponse getApplication(
      UUID applicationId, String accountIdHeader, String accountRole) {

    requireAuthenticatedAccount(accountIdHeader);

    Application application;

    if ("CANDIDATE".equals(accountRole)) {
      application = getCandidateApplication(applicationId, accountIdHeader, accountRole);

    } else if ("RECRUITER".equals(accountRole)) {
      application = getRecruiterApplication(applicationId, accountIdHeader, accountRole);

    } else {
      throw new AppException(ErrorCode.FORBIDDEN);
    }

    return applicationMapper.toApplicationResponse(application);
  }

  @Transactional
  public ApplicationResponse updateApplicationStatus(
      UUID applicationId,
      String accountIdHeader,
      String accountRole,
      UpdateApplicationStatusRequest request) {

    requireRecruiterAccount(accountIdHeader, accountRole);

    Application application = getRecruiterApplication(applicationId, accountIdHeader, accountRole);

    validateRecruiterStatusTransition(application.getStatus(), request.status());

    application.setStatus(request.status());

    Application savedApplication = applicationRepository.save(application);

    return applicationMapper.toApplicationResponse(savedApplication);
  }

  private Application getCandidateApplication(
      UUID applicationId, String accountIdHeader, String accountRole) {

    CandidateSummaryResponse candidate = candidateClient.getMyProfile(accountIdHeader, accountRole);

    return applicationRepository
        .findByIdAndCandidateId(applicationId, candidate.id())
        .orElseThrow(() -> new AppException(ErrorCode.APPLICATION_NOT_FOUND));
  }

  private Application getRecruiterApplication(
      UUID applicationId, String accountIdHeader, String accountRole) {

    Application application =
        applicationRepository
            .findById(applicationId)
            .orElseThrow(() -> new AppException(ErrorCode.APPLICATION_NOT_FOUND));

    try {
      jobClient.getOwnedJob(application.getJobId(), accountIdHeader, accountRole);

    } catch (AppException exception) {

      if (exception.getErrorCode() == ErrorCode.JOB_NOT_FOUND) {

        throw new AppException(ErrorCode.APPLICATION_NOT_FOUND);
      }

      throw exception;
    }

    return application;
  }

  private void validatePagination(int page, int size) {

    if (page < 0 || size < 1 || size > 100) {

      throw new AppException(ErrorCode.INVALID_REQUEST);
    }
  }

  private void validateRecruiterStatusTransition(
      ApplicationStatus currentStatus, ApplicationStatus targetStatus) {

    boolean allowed =
        switch (currentStatus) {
          case SUBMITTED ->
              targetStatus == ApplicationStatus.SCREENING
                  || targetStatus == ApplicationStatus.REJECTED;

          case SCREENING ->
              targetStatus == ApplicationStatus.INTERVIEW
                  || targetStatus == ApplicationStatus.REJECTED;

          case INTERVIEW ->
              targetStatus == ApplicationStatus.OFFER || targetStatus == ApplicationStatus.REJECTED;

          case OFFER ->
              targetStatus == ApplicationStatus.HIRED || targetStatus == ApplicationStatus.REJECTED;

          case HIRED, REJECTED, WITHDRAWN -> false;
        };

    if (!allowed) {
      throw new AppException(ErrorCode.INVALID_APPLICATION_STATUS_TRANSITION);
    }
  }

  private PageResponse<ApplicationResponse> toPageResponse(Page<Application> applicationPage) {

    List<ApplicationResponse> content =
        applicationPage.getContent().stream()
            .map(applicationMapper::toApplicationResponse)
            .toList();

    return new PageResponse<>(
        content,
        applicationPage.getNumber(),
        applicationPage.getSize(),
        applicationPage.getTotalElements(),
        applicationPage.getTotalPages());
  }

  private UUID requireCandidateAccount(String accountIdHeader, String accountRole) {

    UUID accountId = requireAuthenticatedAccount(accountIdHeader);

    if (!"CANDIDATE".equals(accountRole)) {
      throw new AppException(ErrorCode.FORBIDDEN);
    }

    return accountId;
  }

  private UUID requireRecruiterAccount(String accountIdHeader, String accountRole) {

    UUID accountId = requireAuthenticatedAccount(accountIdHeader);

    if (!"RECRUITER".equals(accountRole)) {
      throw new AppException(ErrorCode.FORBIDDEN);
    }

    return accountId;
  }

  private UUID requireAuthenticatedAccount(String accountIdHeader) {

    if (accountIdHeader == null || accountIdHeader.isBlank()) {

      throw new AppException(ErrorCode.UNAUTHENTICATED);
    }

    try {
      return UUID.fromString(accountIdHeader);

    } catch (IllegalArgumentException exception) {
      throw new AppException(ErrorCode.UNAUTHENTICATED);
    }
  }
}
