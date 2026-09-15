package com.tbm.recruitment.recruitment.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.tbm.recruitment.recruitment.dto.response.AdminRecruitmentStatisticsResponse;
import com.tbm.recruitment.recruitment.dto.response.ApplicationResponse;
import com.tbm.recruitment.recruitment.dto.response.InterviewResponse;
import com.tbm.recruitment.recruitment.dto.response.PageResponse;
import com.tbm.recruitment.recruitment.entity.Application;
import com.tbm.recruitment.recruitment.entity.Interview;
import com.tbm.recruitment.recruitment.enums.ApplicationStatus;
import com.tbm.recruitment.recruitment.exception.AppException;
import com.tbm.recruitment.recruitment.exception.ErrorCode;
import com.tbm.recruitment.recruitment.mapper.ApplicationMapper;
import com.tbm.recruitment.recruitment.mapper.InterviewMapper;
import com.tbm.recruitment.recruitment.repository.ApplicationRepository;
import com.tbm.recruitment.recruitment.repository.InterviewRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class AdminRecruitmentServiceTest {

  @Mock private ApplicationRepository applicationRepository;
  @Mock private InterviewRepository interviewRepository;
  @Mock private ApplicationMapper applicationMapper;
  @Mock private InterviewMapper interviewMapper;

  private AdminRecruitmentService service;

  @BeforeEach
  void setUp() {
    service =
        new AdminRecruitmentService(
            applicationRepository, interviewRepository, applicationMapper, interviewMapper);
  }

  @Test
  void adminCanReadApplicationsWithFiltersAndPagination() {
    Application application = buildApplication(ApplicationStatus.SCREENING);
    ApplicationResponse mapped =
        new ApplicationResponse(
            application.getId(),
            application.getCandidateId(),
            application.getJobId(),
            application.getResumeId(),
            application.getStatus(),
            application.getSubmittedAt());
    when(applicationRepository.findAll(
            any(org.springframework.data.jpa.domain.Specification.class), any(PageRequest.class)))
        .thenReturn(new PageImpl<>(List.of(application), PageRequest.of(0, 20), 1));
    when(applicationMapper.toApplicationResponse(application)).thenReturn(mapped);

    PageResponse<ApplicationResponse> response =
        service.getApplications(
            UUID.randomUUID().toString(),
            "ADMIN",
            "SCREENING",
            application.getJobId(),
            application.getCandidateId(),
            0,
            20);

    assertEquals(1, response.content().size());
    assertEquals(ApplicationStatus.SCREENING, response.content().getFirst().status());
  }

  @Test
  void nonAdminRolesAreForbidden() {
    String accountId = UUID.randomUUID().toString();

    AppException exception =
        assertThrows(
            AppException.class, () -> service.getInterviews(accountId, "RECRUITER", 0, 20));

    assertEquals(ErrorCode.FORBIDDEN, exception.getErrorCode());

    AppException candidateException =
        assertThrows(
            AppException.class, () -> service.getInterviews(accountId, "CANDIDATE", 0, 20));

    assertEquals(ErrorCode.FORBIDDEN, candidateException.getErrorCode());
  }

  @Test
  void adminCanReadInterviewDetail() {
    Interview interview = buildInterview();
    InterviewResponse mapped =
        new InterviewResponse(
            interview.getId(),
            interview.getApplicationId(),
            interview.getScheduledAt(),
            interview.getLocation(),
            interview.getNote(),
            interview.getCreatedAt());
    when(interviewRepository.findById(interview.getId())).thenReturn(Optional.of(interview));
    when(interviewMapper.toInterviewResponse(interview)).thenReturn(mapped);

    InterviewResponse response =
        service.getInterviewById(interview.getId(), UUID.randomUUID().toString(), "ADMIN");

    assertEquals(interview.getId(), response.id());
  }

  @Test
  void statisticsCountsEachStatusAndInterviews() {
    when(applicationRepository.count()).thenReturn(20L);
    when(applicationRepository.countByStatus(ApplicationStatus.SUBMITTED)).thenReturn(4L);
    when(applicationRepository.countByStatus(ApplicationStatus.SCREENING)).thenReturn(3L);
    when(applicationRepository.countByStatus(ApplicationStatus.INTERVIEW)).thenReturn(5L);
    when(applicationRepository.countByStatus(ApplicationStatus.OFFER)).thenReturn(2L);
    when(applicationRepository.countByStatus(ApplicationStatus.HIRED)).thenReturn(1L);
    when(applicationRepository.countByStatus(ApplicationStatus.REJECTED)).thenReturn(3L);
    when(applicationRepository.countByStatus(ApplicationStatus.WITHDRAWN)).thenReturn(2L);
    when(interviewRepository.count()).thenReturn(6L);

    AdminRecruitmentStatisticsResponse response =
        service.getStatistics(UUID.randomUUID().toString(), "ADMIN");

    assertEquals(20L, response.totalApplications());
    assertEquals(4L, response.submittedApplications());
    assertEquals(3L, response.screeningApplications());
    assertEquals(5L, response.interviewApplications());
    assertEquals(2L, response.offerApplications());
    assertEquals(1L, response.hiredApplications());
    assertEquals(3L, response.rejectedApplications());
    assertEquals(2L, response.withdrawnApplications());
    assertEquals(6L, response.totalInterviews());
  }

  private Application buildApplication(ApplicationStatus status) {
    return Application.builder()
        .id(UUID.randomUUID())
        .candidateId(UUID.randomUUID())
        .jobId(UUID.randomUUID())
        .resumeId(UUID.randomUUID())
        .status(status)
        .submittedAt(Instant.now())
        .build();
  }

  private Interview buildInterview() {
    Instant now = Instant.now();
    return Interview.builder()
        .id(UUID.randomUUID())
        .applicationId(UUID.randomUUID())
        .scheduledAt(now)
        .location("Online")
        .note("Bring CV")
        .scheduledByAccountId(UUID.randomUUID())
        .createdAt(now)
        .build();
  }
}
