package com.tbm.recruitment.recruitment.service;

import com.tbm.recruitment.recruitment.client.CandidateClient;
import com.tbm.recruitment.recruitment.client.JobClient;
import com.tbm.recruitment.recruitment.dto.request.ScheduleInterviewRequest;
import com.tbm.recruitment.recruitment.dto.response.CandidateSummaryResponse;
import com.tbm.recruitment.recruitment.dto.response.InterviewResponse;
import com.tbm.recruitment.recruitment.entity.Application;
import com.tbm.recruitment.recruitment.entity.Interview;
import com.tbm.recruitment.recruitment.enums.ApplicationStatus;
import com.tbm.recruitment.recruitment.event.InterviewScheduledEvent;
import com.tbm.recruitment.recruitment.exception.AppException;
import com.tbm.recruitment.recruitment.exception.ErrorCode;
import com.tbm.recruitment.recruitment.mapper.InterviewMapper;
import com.tbm.recruitment.recruitment.repository.ApplicationRepository;
import com.tbm.recruitment.recruitment.repository.InterviewRepository;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class InterviewService {

  InterviewRepository interviewRepository;
  ApplicationRepository applicationRepository;
  InterviewMapper interviewMapper;

  CandidateClient candidateClient;
  JobClient jobClient;
  ApplicationEventPublisher applicationEventPublisher;

  @Transactional
  public InterviewResponse scheduleInterview(
      UUID applicationId,
      String accountIdHeader,
      String accountRole,
      ScheduleInterviewRequest request) {

    UUID recruiterAccountId = requireRecruiterAccount(accountIdHeader, accountRole);

    Application application = getRecruiterApplication(applicationId, accountIdHeader, accountRole);

    if (application.getStatus() != ApplicationStatus.INTERVIEW) {
      throw new AppException(ErrorCode.APPLICATION_NOT_READY_FOR_INTERVIEW);
    }

    if (interviewRepository.existsByApplicationId(applicationId)) {
      throw new AppException(ErrorCode.INTERVIEW_ALREADY_SCHEDULED);
    }

    Interview interview =
        Interview.builder()
            .applicationId(applicationId)
            .scheduledAt(request.scheduledAt())
            .location(request.location().trim())
            .note(normalizeNote(request.note()))
            .scheduledByAccountId(recruiterAccountId)
            .build();

    try {

      Interview savedInterview = interviewRepository.save(interview);

      publishInterviewScheduledEvent(savedInterview, application.getCandidateId());

      return interviewMapper.toInterviewResponse(savedInterview);

    } catch (DataIntegrityViolationException exception) {

      throw new AppException(ErrorCode.INTERVIEW_ALREADY_SCHEDULED);
    }
  }

  @Transactional(readOnly = true)
  public InterviewResponse getInterview(
      UUID applicationId, String accountIdHeader, String accountRole) {

    requireAuthenticatedAccount(accountIdHeader);

    if ("CANDIDATE".equals(accountRole)) {

      getCandidateApplication(applicationId, accountIdHeader, accountRole);

    } else if ("RECRUITER".equals(accountRole)) {

      getRecruiterApplication(applicationId, accountIdHeader, accountRole);

    } else {
      throw new AppException(ErrorCode.FORBIDDEN);
    }

    Interview interview =
        interviewRepository
            .findByApplicationId(applicationId)
            .orElseThrow(() -> new AppException(ErrorCode.INTERVIEW_NOT_FOUND));

    return interviewMapper.toInterviewResponse(interview);
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

  private void publishInterviewScheduledEvent(Interview interview, UUID candidateId) {

    InterviewScheduledEvent event =
        new InterviewScheduledEvent(
            UUID.randomUUID(),
            interview.getApplicationId(),
            candidateId,
            interview.getScheduledAt(),
            interview.getLocation(),
            Instant.now());

    applicationEventPublisher.publishEvent(event);
  }

  private String normalizeNote(String note) {

    if (note == null) {
      return null;
    }

    String normalized = note.trim();

    return normalized.isEmpty() ? null : normalized;
  }
}
