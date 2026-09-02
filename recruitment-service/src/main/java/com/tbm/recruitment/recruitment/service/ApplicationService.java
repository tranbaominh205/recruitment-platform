package com.tbm.recruitment.recruitment.service;

import com.tbm.recruitment.recruitment.client.CandidateClient;
import com.tbm.recruitment.recruitment.client.JobClient;
import com.tbm.recruitment.recruitment.client.ResumeClient;
import com.tbm.recruitment.recruitment.dto.request.CreateApplicationRequest;
import com.tbm.recruitment.recruitment.dto.response.ApplicationResponse;
import com.tbm.recruitment.recruitment.dto.response.CandidateSummaryResponse;
import com.tbm.recruitment.recruitment.dto.response.JobSummaryResponse;
import com.tbm.recruitment.recruitment.dto.response.ResumeSummaryResponse;
import com.tbm.recruitment.recruitment.entity.Application;
import com.tbm.recruitment.recruitment.enums.ApplicationStatus;
import com.tbm.recruitment.recruitment.exception.AppException;
import com.tbm.recruitment.recruitment.exception.ErrorCode;
import com.tbm.recruitment.recruitment.mapper.ApplicationMapper;
import com.tbm.recruitment.recruitment.repository.ApplicationRepository;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

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
}
