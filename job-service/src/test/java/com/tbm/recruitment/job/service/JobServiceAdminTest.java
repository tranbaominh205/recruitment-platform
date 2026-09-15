package com.tbm.recruitment.job.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tbm.recruitment.job.client.EmployerClient;
import com.tbm.recruitment.job.dto.response.AdminJobStatisticsResponse;
import com.tbm.recruitment.job.dto.response.JobResponse;
import com.tbm.recruitment.job.dto.response.PageResponse;
import com.tbm.recruitment.job.entity.Job;
import com.tbm.recruitment.job.entity.JobStatus;
import com.tbm.recruitment.job.exception.AppException;
import com.tbm.recruitment.job.exception.ErrorCode;
import com.tbm.recruitment.job.mapper.JobMapper;
import com.tbm.recruitment.job.repository.JobRepository;
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
class JobServiceAdminTest {

  @Mock private JobRepository jobRepository;
  @Mock private JobMapper jobMapper;
  @Mock private EmployerClient employerClient;

  private JobService jobService;

  @BeforeEach
  void setUp() {
    jobService = new JobService(jobRepository, jobMapper, employerClient);
  }

  @Test
  void adminCanReadDraftJobsWithoutAffectingPublicFilters() {
    Job draftJob = buildJob(JobStatus.DRAFT);
    JobResponse draftResponse = mapResponse(draftJob);
    when(jobRepository.findAll(
            any(org.springframework.data.jpa.domain.Specification.class), any(PageRequest.class)))
        .thenReturn(new PageImpl<>(List.of(draftJob), PageRequest.of(0, 20), 1));
    when(jobMapper.toJobResponse(draftJob)).thenReturn(draftResponse);
    when(jobRepository.findById(draftJob.getId())).thenReturn(Optional.of(draftJob));
    when(jobRepository.findByIdAndStatus(draftJob.getId(), JobStatus.PUBLISHED))
        .thenReturn(Optional.empty());

    PageResponse<JobResponse> adminList =
        jobService.getAdminJobs(UUID.randomUUID().toString(), "ADMIN", "java", "DRAFT", 0, 20);
    JobResponse adminDetail =
        jobService.getAdminJobById(draftJob.getId(), UUID.randomUUID().toString(), "ADMIN");

    assertEquals(1, adminList.content().size());
    assertEquals(JobStatus.DRAFT, adminList.content().getFirst().status());
    assertEquals(JobStatus.DRAFT, adminDetail.status());

    AppException publicException =
        assertThrows(AppException.class, () -> jobService.getPublishedJob(draftJob.getId()));
    assertEquals(ErrorCode.JOB_NOT_FOUND, publicException.getErrorCode());
  }

  @Test
  void candidateAndRecruiterAreForbiddenForAdminReads() {
    String accountId = UUID.randomUUID().toString();
    UUID jobId = UUID.randomUUID();

    AppException recruiterException =
        assertThrows(
            AppException.class, () -> jobService.getAdminJobById(jobId, accountId, "RECRUITER"));
    assertEquals(ErrorCode.FORBIDDEN, recruiterException.getErrorCode());

    AppException candidateException =
        assertThrows(
            AppException.class,
            () -> jobService.getAdminJobs(accountId, "CANDIDATE", null, null, 0, 20));
    assertEquals(ErrorCode.FORBIDDEN, candidateException.getErrorCode());
  }

  @Test
  void adminStatisticsAggregatesByStatus() {
    when(jobRepository.count()).thenReturn(9L);
    when(jobRepository.countByStatus(JobStatus.DRAFT)).thenReturn(2L);
    when(jobRepository.countByStatus(JobStatus.PUBLISHED)).thenReturn(4L);
    when(jobRepository.countByStatus(JobStatus.CLOSED)).thenReturn(3L);

    AdminJobStatisticsResponse response =
        jobService.getAdminStatistics(UUID.randomUUID().toString(), "ADMIN");

    assertEquals(9L, response.totalJobs());
    assertEquals(2L, response.draftJobs());
    assertEquals(4L, response.publishedJobs());
    assertEquals(3L, response.closedJobs());
    verify(jobRepository).count();
  }

  private Job buildJob(JobStatus status) {
    Instant now = Instant.now();
    return Job.builder()
        .id(UUID.randomUUID())
        .companyId(UUID.randomUUID())
        .createdByAccountId(UUID.randomUUID())
        .title("Java Backend")
        .description("desc")
        .status(status)
        .createdAt(now)
        .updatedAt(now)
        .build();
  }

  private JobResponse mapResponse(Job job) {
    return new JobResponse(
        job.getId(),
        job.getCompanyId(),
        job.getTitle(),
        job.getDescription(),
        job.getRequirements(),
        job.getRequiredSkills(),
        job.getMinimumYearsExperience(),
        job.getRequiredEducationLevel(),
        job.getDomain(),
        job.getLocation(),
        job.getEmploymentType(),
        job.getWorkplaceType(),
        job.getSalaryMin(),
        job.getSalaryMax(),
        job.getStatus(),
        job.getCreatedAt(),
        job.getUpdatedAt());
  }
}
