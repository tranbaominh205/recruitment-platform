package com.tbm.recruitment.job.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tbm.recruitment.job.client.EmployerClient;
import com.tbm.recruitment.job.dto.request.CreateJobRequest;
import com.tbm.recruitment.job.dto.request.UpdateJobModerationRequest;
import com.tbm.recruitment.job.dto.request.UpdateJobRequest;
import com.tbm.recruitment.job.dto.response.CompanyModerationStatus;
import com.tbm.recruitment.job.dto.response.CompanySummaryResponse;
import com.tbm.recruitment.job.dto.response.JobResponse;
import com.tbm.recruitment.job.entity.EducationLevel;
import com.tbm.recruitment.job.entity.Job;
import com.tbm.recruitment.job.entity.JobModerationStatus;
import com.tbm.recruitment.job.entity.JobStatus;
import com.tbm.recruitment.job.enums.JobListChange;
import com.tbm.recruitment.job.event.JobListChangedEvent;
import com.tbm.recruitment.job.exception.AppException;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class JobServiceRealtimeEventTest {

  @Mock private JobRepository jobRepository;
  @Mock private JobMapper jobMapper;
  @Mock private EmployerClient employerClient;
  @Mock private ApplicationEventPublisher applicationEventPublisher;

  private JobService service;

  @BeforeEach
  void setUp() {
    service = new JobService(jobRepository, jobMapper, employerClient, applicationEventPublisher);
    lenient()
        .when(jobMapper.toJobResponse(any(Job.class)))
        .thenAnswer(invocation -> mapResponse(invocation.getArgument(0)));
  }

  @Test
  void createPublishesOwnerEventWithoutPublicCatalogChange() {
    String recruiterId = UUID.randomUUID().toString();
    UUID companyId = UUID.randomUUID();
    Job mapped = buildJob(JobStatus.DRAFT, JobModerationStatus.ACTIVE);
    mapped.setCreatedByAccountId(UUID.fromString(recruiterId));
    mapped.setStatus(JobStatus.DRAFT);
    CreateJobRequest request = buildCreateRequest();

    when(employerClient.getMyCompany(recruiterId, "RECRUITER"))
        .thenReturn(new CompanySummaryResponse(companyId, "TBM", CompanyModerationStatus.ACTIVE));
    when(jobMapper.toJob(request)).thenReturn(mapped);
    when(jobRepository.save(any(Job.class))).thenAnswer(invocation -> invocation.getArgument(0));

    service.createJob(recruiterId, "RECRUITER", request);

    JobListChangedEvent event = captureEvent();
    assertEquals(JobListChange.CREATED, event.change());
    assertFalse(event.publicCatalogChanged());
    assertEquals(mapped.getCreatedByAccountId(), event.ownerAccountId());
    assertEquals(mapped.getId(), event.jobId());
  }

  @Test
  void updateDraftPublishesOwnerEventWithoutPublicCatalogChange() {
    String recruiterId = UUID.randomUUID().toString();
    UUID companyId = UUID.randomUUID();
    UUID jobId = UUID.randomUUID();
    Job draft = buildJob(JobStatus.DRAFT, JobModerationStatus.ACTIVE);
    draft.setId(jobId);
    draft.setCompanyId(companyId);
    UpdateJobRequest request = buildUpdateRequest("Updated title");

    when(employerClient.getMyCompany(recruiterId, "RECRUITER"))
        .thenReturn(new CompanySummaryResponse(companyId, "TBM", CompanyModerationStatus.ACTIVE));
    when(jobRepository.findByIdAndCompanyId(jobId, companyId)).thenReturn(Optional.of(draft));
    when(jobRepository.save(any(Job.class))).thenAnswer(invocation -> invocation.getArgument(0));

    service.updateJob(jobId, recruiterId, "RECRUITER", request);

    JobListChangedEvent event = captureEvent();
    assertEquals(JobListChange.UPDATED, event.change());
    assertFalse(event.publicCatalogChanged());
  }

  @Test
  void publishJobPublishesOwnerAndPublicCatalogChangeEvent() {
    String recruiterId = UUID.randomUUID().toString();
    UUID companyId = UUID.randomUUID();
    UUID jobId = UUID.randomUUID();
    Job draft = buildJob(JobStatus.DRAFT, JobModerationStatus.ACTIVE);
    draft.setId(jobId);
    draft.setCompanyId(companyId);

    when(employerClient.getMyCompany(recruiterId, "RECRUITER"))
        .thenReturn(new CompanySummaryResponse(companyId, "TBM", CompanyModerationStatus.ACTIVE));
    when(jobRepository.findByIdAndCompanyId(jobId, companyId)).thenReturn(Optional.of(draft));
    when(jobRepository.save(any(Job.class))).thenAnswer(invocation -> invocation.getArgument(0));

    service.publishJob(jobId, recruiterId, "RECRUITER");

    JobListChangedEvent event = captureEvent();
    assertEquals(JobListChange.PUBLISHED, event.change());
    assertEquals(true, event.publicCatalogChanged());
  }

  @Test
  void closePublicJobPublishesOwnerAndPublicCatalogChangeEvent() {
    String recruiterId = UUID.randomUUID().toString();
    UUID companyId = UUID.randomUUID();
    UUID jobId = UUID.randomUUID();
    Job published = buildJob(JobStatus.PUBLISHED, JobModerationStatus.ACTIVE);
    published.setId(jobId);
    published.setCompanyId(companyId);

    when(employerClient.getMyCompany(recruiterId, "RECRUITER"))
        .thenReturn(new CompanySummaryResponse(companyId, "TBM", CompanyModerationStatus.ACTIVE));
    when(jobRepository.findByIdAndCompanyId(jobId, companyId)).thenReturn(Optional.of(published));
    when(jobRepository.save(any(Job.class))).thenAnswer(invocation -> invocation.getArgument(0));

    service.closeJob(jobId, recruiterId, "RECRUITER");

    JobListChangedEvent event = captureEvent();
    assertEquals(JobListChange.CLOSED, event.change());
    assertEquals(true, event.publicCatalogChanged());
  }

  @Test
  void moderationActiveToHiddenOnPublishedSetsPublicCatalogChanged() {
    UUID jobId = UUID.randomUUID();
    UUID moderatorId = UUID.randomUUID();
    Job job = buildJob(JobStatus.PUBLISHED, JobModerationStatus.ACTIVE);
    job.setId(jobId);
    when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));
    when(jobRepository.save(any(Job.class))).thenAnswer(invocation -> invocation.getArgument(0));

    service.updateJobModeration(
        jobId,
        moderatorId.toString(),
        "ADMIN",
        "JOB_MODERATE",
        new UpdateJobModerationRequest(JobModerationStatus.HIDDEN, "policy"));

    JobListChangedEvent event = captureEvent();
    assertEquals(JobListChange.MODERATION_CHANGED, event.change());
    assertEquals(true, event.publicCatalogChanged());
  }

  @Test
  void moderationHiddenToActiveOnPublishedSetsPublicCatalogChanged() {
    UUID jobId = UUID.randomUUID();
    UUID moderatorId = UUID.randomUUID();
    Job job = buildJob(JobStatus.PUBLISHED, JobModerationStatus.HIDDEN);
    job.setId(jobId);
    when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));
    when(jobRepository.save(any(Job.class))).thenAnswer(invocation -> invocation.getArgument(0));

    service.updateJobModeration(
        jobId,
        moderatorId.toString(),
        "ADMIN",
        "JOB_MODERATE",
        new UpdateJobModerationRequest(JobModerationStatus.ACTIVE, null));

    JobListChangedEvent event = captureEvent();
    assertEquals(JobListChange.MODERATION_CHANGED, event.change());
    assertEquals(true, event.publicCatalogChanged());
  }

  @Test
  void moderationHiddenToRemovedDoesNotChangePublicCatalogVisibility() {
    UUID jobId = UUID.randomUUID();
    UUID moderatorId = UUID.randomUUID();
    Job job = buildJob(JobStatus.PUBLISHED, JobModerationStatus.HIDDEN);
    job.setId(jobId);
    when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));
    when(jobRepository.save(any(Job.class))).thenAnswer(invocation -> invocation.getArgument(0));

    service.updateJobModeration(
        jobId,
        moderatorId.toString(),
        "ADMIN",
        "JOB_MODERATE",
        new UpdateJobModerationRequest(JobModerationStatus.REMOVED, "policy"));

    JobListChangedEvent event = captureEvent();
    assertEquals(JobListChange.MODERATION_CHANGED, event.change());
    assertFalse(event.publicCatalogChanged());
  }

  @Test
  void moderationActiveToHiddenOnDraftDoesNotChangePublicCatalogVisibility() {
    UUID jobId = UUID.randomUUID();
    UUID moderatorId = UUID.randomUUID();
    Job job = buildJob(JobStatus.DRAFT, JobModerationStatus.ACTIVE);
    job.setId(jobId);
    when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));
    when(jobRepository.save(any(Job.class))).thenAnswer(invocation -> invocation.getArgument(0));

    service.updateJobModeration(
        jobId,
        moderatorId.toString(),
        "ADMIN",
        "JOB_MODERATE",
        new UpdateJobModerationRequest(JobModerationStatus.HIDDEN, "policy"));

    JobListChangedEvent event = captureEvent();
    assertEquals(JobListChange.MODERATION_CHANGED, event.change());
    assertFalse(event.publicCatalogChanged());
  }

  @Test
  void failedValidationOrPersistenceDoesNotPublishRealtimeEvent() {
    String recruiterId = UUID.randomUUID().toString();
    UUID companyId = UUID.randomUUID();
    Job mapped = buildJob(JobStatus.DRAFT, JobModerationStatus.ACTIVE);

    when(employerClient.getMyCompany(recruiterId, "RECRUITER"))
        .thenReturn(new CompanySummaryResponse(companyId, "TBM", CompanyModerationStatus.ACTIVE));
    when(jobMapper.toJob(any(CreateJobRequest.class))).thenReturn(mapped);

    CreateJobRequest invalidSalaryRequest =
        new CreateJobRequest(
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
            BigDecimal.valueOf(3000),
            BigDecimal.valueOf(1000));

    assertThrows(
        AppException.class,
        () -> service.createJob(recruiterId, "RECRUITER", invalidSalaryRequest));
    verify(applicationEventPublisher, never()).publishEvent(any(JobListChangedEvent.class));

    when(jobRepository.save(any(Job.class))).thenThrow(new RuntimeException("db down"));
    assertThrows(
        RuntimeException.class,
        () -> service.createJob(recruiterId, "RECRUITER", buildCreateRequest()));
    verify(applicationEventPublisher, never()).publishEvent(any(JobListChangedEvent.class));
  }

  private JobListChangedEvent captureEvent() {
    ArgumentCaptor<JobListChangedEvent> captor = ArgumentCaptor.forClass(JobListChangedEvent.class);
    verify(applicationEventPublisher).publishEvent(captor.capture());
    return captor.getValue();
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
