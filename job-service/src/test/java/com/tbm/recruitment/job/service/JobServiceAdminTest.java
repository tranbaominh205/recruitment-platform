package com.tbm.recruitment.job.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tbm.recruitment.job.client.EmployerClient;
import com.tbm.recruitment.job.dto.request.CreateJobRequest;
import com.tbm.recruitment.job.dto.request.UpdateJobModerationRequest;
import com.tbm.recruitment.job.dto.request.UpdateJobRequest;
import com.tbm.recruitment.job.dto.response.AdminJobStatisticsResponse;
import com.tbm.recruitment.job.dto.response.CompanyModerationStatus;
import com.tbm.recruitment.job.dto.response.CompanySummaryResponse;
import com.tbm.recruitment.job.dto.response.JobResponse;
import com.tbm.recruitment.job.dto.response.PageResponse;
import com.tbm.recruitment.job.entity.EducationLevel;
import com.tbm.recruitment.job.entity.Job;
import com.tbm.recruitment.job.entity.JobModerationStatus;
import com.tbm.recruitment.job.entity.JobStatus;
import com.tbm.recruitment.job.exception.AppException;
import com.tbm.recruitment.job.exception.ErrorCode;
import com.tbm.recruitment.job.mapper.JobMapper;
import com.tbm.recruitment.job.repository.JobRepository;
import java.math.BigDecimal;
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
import org.springframework.data.domain.Sort;

@ExtendWith(MockitoExtension.class)
class JobServiceAdminTest {

  @Mock private JobRepository jobRepository;
  @Mock private JobMapper jobMapper;
  @Mock private EmployerClient employerClient;

  private JobService jobService;

  @BeforeEach
  void setUp() {
    jobService = new JobService(jobRepository, jobMapper, employerClient);
    lenient()
        .when(jobMapper.toJobResponse(any(Job.class)))
        .thenAnswer(invocation -> mapResponse(invocation.getArgument(0)));
  }

  @Test
  void createJobDefaultsModerationStatusActive() {
    String recruiterId = UUID.randomUUID().toString();
    UUID companyId = UUID.randomUUID();
    CreateJobRequest request = buildCreateRequest();
    Job mapped = buildJob(JobStatus.DRAFT, null);
    mapped.setModerationStatus(null);

    when(employerClient.getMyCompany(recruiterId, "RECRUITER"))
        .thenReturn(new CompanySummaryResponse(companyId, "TBM", CompanyModerationStatus.ACTIVE));
    when(jobMapper.toJob(request)).thenReturn(mapped);
    when(jobRepository.save(any(Job.class))).thenAnswer(invocation -> invocation.getArgument(0));

    JobResponse response = jobService.createJob(recruiterId, "RECRUITER", request);

    assertEquals(JobModerationStatus.ACTIVE, response.moderationStatus());
    verify(jobRepository)
        .save(any(Job.class)); // saved entity is asserted via response mapped from saved instance
  }

  @Test
  void activeCompanyCanCreateUpdateAndPublishDraftJobs() {
    String recruiterId = UUID.randomUUID().toString();
    UUID companyId = UUID.randomUUID();
    UUID updateJobId = UUID.randomUUID();
    UUID publishJobId = UUID.randomUUID();
    CreateJobRequest createRequest = buildCreateRequest();
    UpdateJobRequest updateRequest = buildUpdateRequest("Java Backend Updated");
    Job createMapped = buildJob(JobStatus.DRAFT, JobModerationStatus.ACTIVE);
    Job updateDraft = buildJob(JobStatus.DRAFT, JobModerationStatus.ACTIVE);
    Job publishDraft = buildJob(JobStatus.DRAFT, JobModerationStatus.ACTIVE);
    updateDraft.setId(updateJobId);
    publishDraft.setId(publishJobId);

    when(employerClient.getMyCompany(recruiterId, "RECRUITER"))
        .thenReturn(new CompanySummaryResponse(companyId, "TBM", CompanyModerationStatus.ACTIVE));
    when(jobMapper.toJob(createRequest)).thenReturn(createMapped);
    doAnswer(
            invocation -> {
              UpdateJobRequest request = invocation.getArgument(0);
              Job target = invocation.getArgument(1);
              target.setTitle(request.title());
              target.setDescription(request.description());
              return null;
            })
        .when(jobMapper)
        .updateJob(any(UpdateJobRequest.class), any(Job.class));
    when(jobRepository.findByIdAndCompanyId(updateJobId, companyId))
        .thenReturn(Optional.of(updateDraft));
    when(jobRepository.findByIdAndCompanyId(publishJobId, companyId))
        .thenReturn(Optional.of(publishDraft));
    when(jobRepository.save(any(Job.class))).thenAnswer(invocation -> invocation.getArgument(0));

    JobResponse created = jobService.createJob(recruiterId, "RECRUITER", createRequest);
    JobResponse updated =
        jobService.updateJob(updateJobId, recruiterId, "RECRUITER", updateRequest);
    JobResponse published = jobService.publishJob(publishJobId, recruiterId, "RECRUITER");

    assertEquals(JobStatus.DRAFT, created.status());
    assertEquals("Java Backend Updated", updated.title());
    assertEquals(JobStatus.PUBLISHED, published.status());
  }

  @Test
  void suspendedCompanyCannotCreateJobAndDoesNotSave() {
    String recruiterId = UUID.randomUUID().toString();
    UUID companyId = UUID.randomUUID();

    when(employerClient.getMyCompany(recruiterId, "RECRUITER"))
        .thenReturn(
            new CompanySummaryResponse(companyId, "TBM", CompanyModerationStatus.SUSPENDED));

    AppException exception =
        assertThrows(
            AppException.class,
            () -> jobService.createJob(recruiterId, "RECRUITER", buildCreateRequest()));

    assertEquals(ErrorCode.COMPANY_SUSPENDED, exception.getErrorCode());
    verify(jobMapper, never()).toJob(any(CreateJobRequest.class));
    verify(jobRepository, never()).save(any(Job.class));
  }

  @Test
  void suspendedCompanyCannotUpdateDraftJobAndDoesNotMutate() {
    String recruiterId = UUID.randomUUID().toString();
    UUID companyId = UUID.randomUUID();
    UUID jobId = UUID.randomUUID();
    Job draft = buildJob(JobStatus.DRAFT, JobModerationStatus.ACTIVE);
    draft.setId(jobId);
    UpdateJobRequest request = buildUpdateRequest("Will Not Update");

    when(employerClient.getMyCompany(recruiterId, "RECRUITER"))
        .thenReturn(
            new CompanySummaryResponse(companyId, "TBM", CompanyModerationStatus.SUSPENDED));

    AppException exception =
        assertThrows(
            AppException.class,
            () -> jobService.updateJob(jobId, recruiterId, "RECRUITER", request));

    assertEquals(ErrorCode.COMPANY_SUSPENDED, exception.getErrorCode());
    assertEquals(JobStatus.DRAFT, draft.getStatus());
    verify(jobMapper, never()).updateJob(any(UpdateJobRequest.class), any(Job.class));
    verify(jobRepository, never()).save(any(Job.class));
    verify(jobRepository, never()).findByIdAndCompanyId(any(UUID.class), any(UUID.class));
  }

  @Test
  void suspendedCompanyCannotPublishDraftJobAndDoesNotMutate() {
    String recruiterId = UUID.randomUUID().toString();
    UUID companyId = UUID.randomUUID();
    UUID jobId = UUID.randomUUID();
    Job draft = buildJob(JobStatus.DRAFT, JobModerationStatus.ACTIVE);
    draft.setId(jobId);

    when(employerClient.getMyCompany(recruiterId, "RECRUITER"))
        .thenReturn(
            new CompanySummaryResponse(companyId, "TBM", CompanyModerationStatus.SUSPENDED));

    AppException exception =
        assertThrows(
            AppException.class, () -> jobService.publishJob(jobId, recruiterId, "RECRUITER"));

    assertEquals(ErrorCode.COMPANY_SUSPENDED, exception.getErrorCode());
    assertEquals(JobStatus.DRAFT, draft.getStatus());
    verify(jobRepository, never()).save(any(Job.class));
    verify(jobRepository, never()).findByIdAndCompanyId(any(UUID.class), any(UUID.class));
  }

  @Test
  void suspendedCompanyCanStillClosePublishedJob() {
    String recruiterId = UUID.randomUUID().toString();
    UUID companyId = UUID.randomUUID();
    UUID jobId = UUID.randomUUID();
    Job published = buildJob(JobStatus.PUBLISHED, JobModerationStatus.ACTIVE);
    published.setId(jobId);
    published.setCompanyId(companyId);

    when(employerClient.getMyCompany(recruiterId, "RECRUITER"))
        .thenReturn(
            new CompanySummaryResponse(companyId, "TBM", CompanyModerationStatus.SUSPENDED));
    when(jobRepository.findByIdAndCompanyId(jobId, companyId)).thenReturn(Optional.of(published));
    when(jobRepository.save(any(Job.class))).thenAnswer(invocation -> invocation.getArgument(0));

    JobResponse closed = jobService.closeJob(jobId, recruiterId, "RECRUITER");

    assertEquals(JobStatus.CLOSED, closed.status());
  }

  @Test
  void adminWithJobModeratePermissionCanHideRemoveAndRestoreWithoutChangingJobStatus() {
    UUID adminId = UUID.randomUUID();
    UUID jobId = UUID.randomUUID();
    Job job = buildJob(JobStatus.PUBLISHED, JobModerationStatus.ACTIVE);
    job.setId(jobId);
    when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));
    when(jobRepository.save(any(Job.class))).thenAnswer(invocation -> invocation.getArgument(0));

    JobResponse hidden =
        jobService.updateJobModeration(
            jobId,
            adminId.toString(),
            "ADMIN",
            "JOB_MODERATE,COMPANY_MODERATE",
            new UpdateJobModerationRequest(JobModerationStatus.HIDDEN, "  policy violation  "));

    assertEquals(JobStatus.PUBLISHED, hidden.status());
    assertEquals(JobModerationStatus.HIDDEN, hidden.moderationStatus());
    assertEquals("policy violation", hidden.moderationReason());
    assertNotNull(hidden.moderatedAt());

    JobResponse removed =
        jobService.updateJobModeration(
            jobId,
            adminId.toString(),
            "ADMIN",
            "JOB_MODERATE",
            new UpdateJobModerationRequest(JobModerationStatus.REMOVED, "  spam  "));

    assertEquals(JobStatus.PUBLISHED, removed.status());
    assertEquals(JobModerationStatus.REMOVED, removed.moderationStatus());
    assertEquals("spam", removed.moderationReason());

    JobResponse restored =
        jobService.updateJobModeration(
            jobId,
            adminId.toString(),
            "ADMIN",
            "JOB_MODERATE",
            new UpdateJobModerationRequest(JobModerationStatus.ACTIVE, null));

    assertEquals(JobStatus.PUBLISHED, restored.status());
    assertEquals(JobModerationStatus.ACTIVE, restored.moderationStatus());
    assertNull(restored.moderationReason());
    assertNull(restored.moderatedAt());
  }

  @Test
  void adminWithoutJobModeratePermissionGetsForbidden() {
    AppException exception =
        assertThrows(
            AppException.class,
            () ->
                jobService.updateJobModeration(
                    UUID.randomUUID(),
                    UUID.randomUUID().toString(),
                    "ADMIN",
                    "COMPANY_MODERATE",
                    new UpdateJobModerationRequest(JobModerationStatus.HIDDEN, "reason")));
    assertEquals(ErrorCode.FORBIDDEN, exception.getErrorCode());
  }

  @Test
  void nonAdminGetsForbiddenForModeration() {
    AppException exception =
        assertThrows(
            AppException.class,
            () ->
                jobService.updateJobModeration(
                    UUID.randomUUID(),
                    UUID.randomUUID().toString(),
                    "RECRUITER",
                    "JOB_MODERATE",
                    new UpdateJobModerationRequest(JobModerationStatus.HIDDEN, "reason")));
    assertEquals(ErrorCode.FORBIDDEN, exception.getErrorCode());
  }

  @Test
  void hiddenAndRemovedRequireNonBlankReason() {
    UUID jobId = UUID.randomUUID();
    when(jobRepository.findById(jobId))
        .thenReturn(Optional.of(buildJob(JobStatus.PUBLISHED, null)));

    AppException hiddenException =
        assertThrows(
            AppException.class,
            () ->
                jobService.updateJobModeration(
                    jobId,
                    UUID.randomUUID().toString(),
                    "ADMIN",
                    "JOB_MODERATE",
                    new UpdateJobModerationRequest(JobModerationStatus.HIDDEN, "   ")));
    assertEquals(ErrorCode.INVALID_REQUEST, hiddenException.getErrorCode());

    AppException removedException =
        assertThrows(
            AppException.class,
            () ->
                jobService.updateJobModeration(
                    jobId,
                    UUID.randomUUID().toString(),
                    "ADMIN",
                    "JOB_MODERATE",
                    new UpdateJobModerationRequest(JobModerationStatus.REMOVED, null)));
    assertEquals(ErrorCode.INVALID_REQUEST, removedException.getErrorCode());
  }

  @Test
  void publicDetailExcludesHiddenAndRemovedJobs() {
    UUID hiddenId = UUID.randomUUID();
    Job hidden = buildJob(JobStatus.PUBLISHED, JobModerationStatus.HIDDEN);
    hidden.setId(hiddenId);
    when(jobRepository.findById(hiddenId)).thenReturn(Optional.of(hidden));

    AppException hiddenException =
        assertThrows(AppException.class, () -> jobService.getPublishedJob(hiddenId));
    assertEquals(ErrorCode.JOB_NOT_FOUND, hiddenException.getErrorCode());

    UUID removedId = UUID.randomUUID();
    Job removed = buildJob(JobStatus.PUBLISHED, JobModerationStatus.REMOVED);
    removed.setId(removedId);
    when(jobRepository.findById(removedId)).thenReturn(Optional.of(removed));

    AppException removedException =
        assertThrows(AppException.class, () -> jobService.getPublishedJob(removedId));
    assertEquals(ErrorCode.JOB_NOT_FOUND, removedException.getErrorCode());
  }

  @Test
  void publicSearchExcludesHiddenAndRemovedJobs() {
    Job active = buildJob(JobStatus.PUBLISHED, JobModerationStatus.ACTIVE);
    Job hidden = buildJob(JobStatus.PUBLISHED, JobModerationStatus.HIDDEN);
    Job removed = buildJob(JobStatus.PUBLISHED, JobModerationStatus.REMOVED);
    when(jobRepository.findAll(
            any(org.springframework.data.jpa.domain.Specification.class), any(PageRequest.class)))
        .thenReturn(new PageImpl<>(List.of(active, hidden, removed), PageRequest.of(0, 20), 3));

    PageResponse<JobResponse> response =
        jobService.searchPublishedJobs("java", null, null, null, 0, 20);

    assertEquals(1, response.content().size());
    assertEquals(JobModerationStatus.ACTIVE, response.content().getFirst().moderationStatus());
  }

  @Test
  void recruiterOwnedAndMineApisStillSeeModeratedJobs() {
    String recruiterId = UUID.randomUUID().toString();
    UUID companyId = UUID.randomUUID();
    UUID jobId = UUID.randomUUID();
    Job hidden = buildJob(JobStatus.PUBLISHED, JobModerationStatus.HIDDEN);
    hidden.setId(jobId);
    hidden.setCompanyId(companyId);

    when(employerClient.getMyCompany(recruiterId, "RECRUITER"))
        .thenReturn(
            new CompanySummaryResponse(companyId, "TBM", CompanyModerationStatus.SUSPENDED));
    when(jobRepository.findByIdAndCompanyId(jobId, companyId)).thenReturn(Optional.of(hidden));
    when(jobRepository.findAllByCompanyId(
            companyId, PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"))))
        .thenReturn(new PageImpl<>(List.of(hidden), PageRequest.of(0, 20), 1));

    JobResponse ownership = jobService.getOwnedJobDetails(jobId, recruiterId, "RECRUITER");
    PageResponse<JobResponse> mine = jobService.getMyJobs(recruiterId, "RECRUITER", 0, 20);

    assertEquals(JobModerationStatus.HIDDEN, ownership.moderationStatus());
    assertEquals(1, mine.content().size());
    assertEquals(JobModerationStatus.HIDDEN, mine.content().getFirst().moderationStatus());
  }

  @Test
  void adminModerationStatusFilterWorks() {
    Job active = buildJob(JobStatus.PUBLISHED, JobModerationStatus.ACTIVE);
    Job hidden = buildJob(JobStatus.PUBLISHED, JobModerationStatus.HIDDEN);
    Job removed = buildJob(JobStatus.PUBLISHED, JobModerationStatus.REMOVED);
    when(jobRepository.findAll(
            any(org.springframework.data.jpa.domain.Specification.class), any(PageRequest.class)))
        .thenReturn(new PageImpl<>(List.of(active, hidden, removed), PageRequest.of(0, 20), 3));

    PageResponse<JobResponse> hiddenOnly =
        jobService.getAdminJobs(
            UUID.randomUUID().toString(), "ADMIN", "java", "PUBLISHED", "HIDDEN", 0, 20);

    assertEquals(1, hiddenOnly.content().size());
    assertEquals(JobModerationStatus.HIDDEN, hiddenOnly.content().getFirst().moderationStatus());
  }

  @Test
  void adminCanReadDraftJobsWithoutAffectingPublicFilters() {
    Job draftJob = buildJob(JobStatus.DRAFT, JobModerationStatus.ACTIVE);
    when(jobRepository.findAll(
            any(org.springframework.data.jpa.domain.Specification.class), any(PageRequest.class)))
        .thenReturn(new PageImpl<>(List.of(draftJob), PageRequest.of(0, 20), 1));
    when(jobRepository.findById(draftJob.getId())).thenReturn(Optional.of(draftJob));

    PageResponse<JobResponse> adminList =
        jobService.getAdminJobs(
            UUID.randomUUID().toString(), "ADMIN", "java", "DRAFT", null, 0, 20);
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
            () -> jobService.getAdminJobs(accountId, "CANDIDATE", null, null, null, 0, 20));
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

  private CreateJobRequest buildCreateRequest() {
    return new CreateJobRequest(
        "Java Backend",
        "Build services",
        "Spring Boot",
        List.of("Java", "Spring"),
        2,
        EducationLevel.BACHELOR,
        "Software",
        "HCM",
        "FULL_TIME",
        "HYBRID",
        BigDecimal.valueOf(1000),
        BigDecimal.valueOf(2000));
  }

  private UpdateJobRequest buildUpdateRequest(String title) {
    return new UpdateJobRequest(
        title,
        "Updated description",
        "Updated requirements",
        List.of("Java", "Spring"),
        3,
        EducationLevel.BACHELOR,
        "Software",
        "HCM",
        "FULL_TIME",
        "HYBRID",
        BigDecimal.valueOf(1500),
        BigDecimal.valueOf(2500));
  }

  private Job buildJob(JobStatus status, JobModerationStatus moderationStatus) {
    Instant now = Instant.now();
    return Job.builder()
        .id(UUID.randomUUID())
        .companyId(UUID.randomUUID())
        .createdByAccountId(UUID.randomUUID())
        .title("Java Backend")
        .description("desc")
        .requirements("req")
        .requiredSkills(List.of("Java"))
        .minimumYearsExperience(1)
        .requiredEducationLevel(EducationLevel.BACHELOR)
        .domain("Software")
        .location("HCM")
        .employmentType("FULL_TIME")
        .workplaceType("HYBRID")
        .salaryMin(BigDecimal.valueOf(1000))
        .salaryMax(BigDecimal.valueOf(2000))
        .status(status)
        .moderationStatus(moderationStatus)
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
        job.getModerationStatus() == null ? JobModerationStatus.ACTIVE : job.getModerationStatus(),
        job.getModerationReason(),
        job.getModeratedAt(),
        job.getCreatedAt(),
        job.getUpdatedAt());
  }
}
