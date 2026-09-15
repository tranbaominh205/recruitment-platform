package com.tbm.recruitment.recruitment.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

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
import com.tbm.recruitment.recruitment.enums.ApplicationListChange;
import com.tbm.recruitment.recruitment.enums.ApplicationStatus;
import com.tbm.recruitment.recruitment.event.ApplicationListChangedEvent;
import com.tbm.recruitment.recruitment.exception.AppException;
import com.tbm.recruitment.recruitment.exception.ErrorCode;
import com.tbm.recruitment.recruitment.mapper.ApplicationMapper;
import com.tbm.recruitment.recruitment.repository.ApplicationRepository;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@ExtendWith(MockitoExtension.class)
class ApplicationServiceTest {

  @Mock private ApplicationRepository applicationRepository;
  @Mock private ApplicationMapper applicationMapper;
  @Mock private ApplicationEventPublisher applicationEventPublisher;
  @Mock private ApplicationSseService applicationSseService;
  @Mock private CandidateClient candidateClient;
  @Mock private ResumeClient resumeClient;
  @Mock private JobClient jobClient;

  private ApplicationService service;

  @BeforeEach
  void setUp() {
    service =
        new ApplicationService(
            applicationRepository,
            applicationMapper,
            applicationEventPublisher,
            applicationSseService,
            candidateClient,
            resumeClient,
            jobClient);
  }

  @Test
  void noFilterDefaultsToSubmittedAtDesc() {
    UUID jobId = UUID.randomUUID();
    Application application = buildApplication(jobId, ApplicationStatus.SUBMITTED);
    stubOwnedJobAndRepository(jobId, application);

    PageResponse<ApplicationResponse> response =
        service.getApplicationsForOwnedJob(
            jobId, UUID.randomUUID().toString(), "RECRUITER", null, "desc", 0, 20);

    assertEquals(1, response.content().size());
    Pageable pageable = capturePageable();
    assertEquals(Sort.Direction.DESC, pageable.getSort().getOrderFor("submittedAt").getDirection());
    assertEquals(Sort.Direction.ASC, pageable.getSort().getOrderFor("id").getDirection());
  }

  @Test
  void submittedFilterIsApplied() {
    UUID jobId = UUID.randomUUID();
    Application application = buildApplication(jobId, ApplicationStatus.SUBMITTED);
    stubOwnedJobAndRepository(jobId, application);

    service.getApplicationsForOwnedJob(
        jobId, UUID.randomUUID().toString(), "RECRUITER", "SUBMITTED", "desc", 0, 20);

    Specification<Application> specification = captureSpecification();
    assertTrue(specificationHasJobAndStatus(specification, jobId, ApplicationStatus.SUBMITTED));
  }

  @Test
  void screeningFilterIsApplied() {
    UUID jobId = UUID.randomUUID();
    Application application = buildApplication(jobId, ApplicationStatus.SCREENING);
    stubOwnedJobAndRepository(jobId, application);

    service.getApplicationsForOwnedJob(
        jobId, UUID.randomUUID().toString(), "RECRUITER", "SCREENING", "desc", 0, 20);

    Specification<Application> specification = captureSpecification();
    assertTrue(specificationHasJobAndStatus(specification, jobId, ApplicationStatus.SCREENING));
  }

  @Test
  void lowercaseStatusParsesSuccessfully() {
    UUID jobId = UUID.randomUUID();
    Application application = buildApplication(jobId, ApplicationStatus.SCREENING);
    stubOwnedJobAndRepository(jobId, application);

    PageResponse<ApplicationResponse> response =
        service.getApplicationsForOwnedJob(
            jobId, UUID.randomUUID().toString(), "RECRUITER", " screening ", "desc", 0, 20);

    assertEquals(ApplicationStatus.SCREENING, response.content().getFirst().status());
  }

  @Test
  void invalidStatusThrowsInvalidRequest() {
    UUID jobId = UUID.randomUUID();
    when(jobClient.getOwnedJob(eq(jobId), any(String.class), eq("RECRUITER")))
        .thenReturn(new JobSummaryResponse(jobId));

    AppException exception =
        assertThrows(
            AppException.class,
            () ->
                service.getApplicationsForOwnedJob(
                    jobId, UUID.randomUUID().toString(), "RECRUITER", "INVALID", "desc", 0, 20));

    assertEquals(ErrorCode.INVALID_REQUEST, exception.getErrorCode());
    verify(applicationRepository, never()).findAll(any(Specification.class), any(Pageable.class));
  }

  @Test
  void sortDirectionDescIsApplied() {
    UUID jobId = UUID.randomUUID();
    Application application = buildApplication(jobId, ApplicationStatus.SUBMITTED);
    stubOwnedJobAndRepository(jobId, application);

    service.getApplicationsForOwnedJob(
        jobId, UUID.randomUUID().toString(), "RECRUITER", null, "DESC", 0, 20);

    Pageable pageable = capturePageable();
    assertEquals(Sort.Direction.DESC, pageable.getSort().getOrderFor("submittedAt").getDirection());
  }

  @Test
  void sortDirectionAscIsApplied() {
    UUID jobId = UUID.randomUUID();
    Application application = buildApplication(jobId, ApplicationStatus.SUBMITTED);
    stubOwnedJobAndRepository(jobId, application);

    service.getApplicationsForOwnedJob(
        jobId, UUID.randomUUID().toString(), "RECRUITER", null, "ASC", 0, 20);

    Pageable pageable = capturePageable();
    assertEquals(Sort.Direction.ASC, pageable.getSort().getOrderFor("submittedAt").getDirection());
    assertEquals(Sort.Direction.ASC, pageable.getSort().getOrderFor("id").getDirection());
  }

  @Test
  void invalidSortDirectionThrowsInvalidRequest() {
    UUID jobId = UUID.randomUUID();
    when(jobClient.getOwnedJob(eq(jobId), any(String.class), eq("RECRUITER")))
        .thenReturn(new JobSummaryResponse(jobId));

    AppException exception =
        assertThrows(
            AppException.class,
            () ->
                service.getApplicationsForOwnedJob(
                    jobId, UUID.randomUUID().toString(), "RECRUITER", null, "newest", 0, 20));

    assertEquals(ErrorCode.INVALID_REQUEST, exception.getErrorCode());
    verify(applicationRepository, never()).findAll(any(Specification.class), any(Pageable.class));
  }

  @Test
  void filterAndSortCanBeCombined() {
    UUID jobId = UUID.randomUUID();
    Application application = buildApplication(jobId, ApplicationStatus.SCREENING);
    stubOwnedJobAndRepository(jobId, application);

    service.getApplicationsForOwnedJob(
        jobId, UUID.randomUUID().toString(), "RECRUITER", "SCREENING", "asc", 1, 10);

    Specification<Application> specification = captureSpecification();
    assertTrue(specificationHasJobAndStatus(specification, jobId, ApplicationStatus.SCREENING));
    Pageable pageable = capturePageable();
    assertEquals(Sort.Direction.ASC, pageable.getSort().getOrderFor("submittedAt").getDirection());
    assertEquals(1, pageable.getPageNumber());
    assertEquals(10, pageable.getPageSize());
  }

  @Test
  void paginationValidationIsPreserved() {
    UUID jobId = UUID.randomUUID();

    AppException exception =
        assertThrows(
            AppException.class,
            () ->
                service.getApplicationsForOwnedJob(
                    jobId, UUID.randomUUID().toString(), "RECRUITER", null, "desc", -1, 20));

    assertEquals(ErrorCode.INVALID_REQUEST, exception.getErrorCode());
    verifyNoInteractions(jobClient);
    verifyNoInteractions(applicationRepository);
  }

  @Test
  void recruiterRoleIsRequired() {
    UUID jobId = UUID.randomUUID();

    AppException exception =
        assertThrows(
            AppException.class,
            () ->
                service.getApplicationsForOwnedJob(
                    jobId, UUID.randomUUID().toString(), "CANDIDATE", null, "desc", 0, 20));

    assertEquals(ErrorCode.FORBIDDEN, exception.getErrorCode());
    verifyNoInteractions(jobClient);
    verifyNoInteractions(applicationRepository);
  }

  @Test
  void ownedJobIsVerifiedBeforeRepositoryQuery() {
    UUID jobId = UUID.randomUUID();
    Application application = buildApplication(jobId, ApplicationStatus.SUBMITTED);
    stubOwnedJobAndRepository(jobId, application);

    service.getApplicationsForOwnedJob(
        jobId, UUID.randomUUID().toString(), "RECRUITER", null, "desc", 0, 20);

    InOrder inOrder = inOrder(jobClient, applicationRepository);
    inOrder.verify(jobClient).getOwnedJob(eq(jobId), any(String.class), eq("RECRUITER"));
    inOrder.verify(applicationRepository).findAll(any(Specification.class), any(Pageable.class));
  }

  @Test
  void specificationAlwaysConstrainsRequestedJob() {
    UUID jobId = UUID.randomUUID();
    Application application = buildApplication(jobId, ApplicationStatus.SUBMITTED);
    stubOwnedJobAndRepository(jobId, application);

    service.getApplicationsForOwnedJob(
        jobId, UUID.randomUUID().toString(), "RECRUITER", null, "desc", 0, 20);

    Specification<Application> specification = captureSpecification();
    assertTrue(specificationHasOnlyJobConstraint(specification, jobId));
  }

  @Test
  void noStatusFilterDoesNotRestrictStatus() {
    UUID jobId = UUID.randomUUID();
    Application application = buildApplication(jobId, ApplicationStatus.SUBMITTED);
    stubOwnedJobAndRepository(jobId, application);

    service.getApplicationsForOwnedJob(
        jobId, UUID.randomUUID().toString(), "RECRUITER", "   ", "desc", 0, 20);

    Specification<Application> specification = captureSpecification();
    assertTrue(specificationHasOnlyJobConstraint(specification, jobId));
  }

  @Test
  void recruiterCanSubscribeToOwnedJobEvents() {
    UUID jobId = UUID.randomUUID();
    String accountId = UUID.randomUUID().toString();
    SseEmitter emitter = new SseEmitter();
    when(jobClient.getOwnedJob(eq(jobId), eq(accountId), eq("RECRUITER")))
        .thenReturn(new JobSummaryResponse(jobId));
    when(applicationSseService.subscribe(jobId)).thenReturn(emitter);

    SseEmitter result = service.subscribeToOwnedJobEvents(jobId, accountId, "RECRUITER");

    assertEquals(emitter, result);
    InOrder inOrder = inOrder(jobClient, applicationSseService);
    inOrder.verify(jobClient).getOwnedJob(jobId, accountId, "RECRUITER");
    inOrder.verify(applicationSseService).subscribe(jobId);
  }

  @Test
  void candidateCannotSubscribeToOwnedJobEvents() {
    AppException exception =
        assertThrows(
            AppException.class,
            () ->
                service.subscribeToOwnedJobEvents(
                    UUID.randomUUID(), UUID.randomUUID().toString(), "CANDIDATE"));

    assertEquals(ErrorCode.FORBIDDEN, exception.getErrorCode());
    verifyNoInteractions(jobClient);
    verifyNoInteractions(applicationSseService);
  }

  @Test
  void adminCannotSubscribeToOwnedJobEvents() {
    AppException exception =
        assertThrows(
            AppException.class,
            () ->
                service.subscribeToOwnedJobEvents(
                    UUID.randomUUID(), UUID.randomUUID().toString(), "ADMIN"));

    assertEquals(ErrorCode.FORBIDDEN, exception.getErrorCode());
    verifyNoInteractions(jobClient);
    verifyNoInteractions(applicationSseService);
  }

  @Test
  void nonOwnerRecruiterCannotSubscribeToOwnedJobEvents() {
    UUID jobId = UUID.randomUUID();
    String accountId = UUID.randomUUID().toString();
    when(jobClient.getOwnedJob(jobId, accountId, "RECRUITER"))
        .thenThrow(new AppException(ErrorCode.FORBIDDEN));

    AppException exception =
        assertThrows(
            AppException.class,
            () -> service.subscribeToOwnedJobEvents(jobId, accountId, "RECRUITER"));

    assertEquals(ErrorCode.FORBIDDEN, exception.getErrorCode());
    verify(applicationSseService, never()).subscribe(any(UUID.class));
  }

  @Test
  void submitApplicationPublishesSubmittedListChangedEvent() {
    UUID accountId = UUID.randomUUID();
    UUID candidateId = UUID.randomUUID();
    UUID resumeId = UUID.randomUUID();
    UUID jobId = UUID.randomUUID();
    UUID applicationId = UUID.randomUUID();
    CreateApplicationRequest request = new CreateApplicationRequest(jobId, resumeId);
    Application saved =
        Application.builder()
            .id(applicationId)
            .candidateId(candidateId)
            .jobId(jobId)
            .resumeId(resumeId)
            .status(ApplicationStatus.SUBMITTED)
            .submittedAt(Instant.now())
            .build();

    when(candidateClient.getMyProfile(accountId.toString(), "CANDIDATE"))
        .thenReturn(new CandidateSummaryResponse(candidateId));
    when(resumeClient.getMyResume(resumeId, accountId.toString(), "CANDIDATE"))
        .thenReturn(new ResumeSummaryResponse(resumeId));
    when(jobClient.getPublishedJob(jobId)).thenReturn(new JobSummaryResponse(jobId));
    when(applicationRepository.save(any(Application.class))).thenReturn(saved);
    when(applicationMapper.toApplicationResponse(saved))
        .thenReturn(
            new ApplicationResponse(
                saved.getId(),
                saved.getCandidateId(),
                saved.getJobId(),
                saved.getResumeId(),
                saved.getStatus(),
                saved.getSubmittedAt()));

    service.submitApplication(accountId.toString(), "CANDIDATE", request);

    ApplicationListChangedEvent event = capturePublishedListChangedEvent();
    assertEquals(jobId, event.jobId());
    assertEquals(applicationId, event.applicationId());
    assertEquals(ApplicationListChange.SUBMITTED, event.change());
  }

  @Test
  void withdrawApplicationPublishesWithdrawnListChangedEvent() {
    UUID accountId = UUID.randomUUID();
    UUID candidateId = UUID.randomUUID();
    UUID applicationId = UUID.randomUUID();
    UUID jobId = UUID.randomUUID();
    UUID resumeId = UUID.randomUUID();
    Application application =
        Application.builder()
            .id(applicationId)
            .candidateId(candidateId)
            .jobId(jobId)
            .resumeId(resumeId)
            .status(ApplicationStatus.SCREENING)
            .submittedAt(Instant.now())
            .build();

    when(candidateClient.getMyProfile(accountId.toString(), "CANDIDATE"))
        .thenReturn(new CandidateSummaryResponse(candidateId));
    when(applicationRepository.findByIdAndCandidateId(applicationId, candidateId))
        .thenReturn(java.util.Optional.of(application));
    when(applicationRepository.save(application)).thenReturn(application);
    when(applicationMapper.toApplicationResponse(application))
        .thenReturn(
            new ApplicationResponse(
                application.getId(),
                application.getCandidateId(),
                application.getJobId(),
                application.getResumeId(),
                application.getStatus(),
                application.getSubmittedAt()));

    service.withdrawApplication(applicationId, accountId.toString(), "CANDIDATE");

    ApplicationListChangedEvent event = capturePublishedListChangedEvent();
    assertEquals(jobId, event.jobId());
    assertEquals(applicationId, event.applicationId());
    assertEquals(ApplicationListChange.WITHDRAWN, event.change());
  }

  @Test
  void updateApplicationStatusPublishesStatusChangedListEvent() {
    UUID accountId = UUID.randomUUID();
    UUID applicationId = UUID.randomUUID();
    UUID jobId = UUID.randomUUID();
    Application application =
        Application.builder()
            .id(applicationId)
            .candidateId(UUID.randomUUID())
            .jobId(jobId)
            .resumeId(UUID.randomUUID())
            .status(ApplicationStatus.SUBMITTED)
            .submittedAt(Instant.now())
            .build();

    when(applicationRepository.findById(applicationId))
        .thenReturn(java.util.Optional.of(application));
    when(jobClient.getOwnedJob(jobId, accountId.toString(), "RECRUITER"))
        .thenReturn(new JobSummaryResponse(jobId));
    when(applicationRepository.save(application)).thenReturn(application);
    when(applicationMapper.toApplicationResponse(application))
        .thenReturn(
            new ApplicationResponse(
                application.getId(),
                application.getCandidateId(),
                application.getJobId(),
                application.getResumeId(),
                application.getStatus(),
                application.getSubmittedAt()));

    service.updateApplicationStatus(
        applicationId,
        accountId.toString(),
        "RECRUITER",
        new UpdateApplicationStatusRequest(ApplicationStatus.SCREENING));

    ApplicationListChangedEvent event = capturePublishedListChangedEvent();
    assertEquals(jobId, event.jobId());
    assertEquals(applicationId, event.applicationId());
    assertEquals(ApplicationListChange.STATUS_CHANGED, event.change());
  }

  @Test
  void failedValidationDoesNotPublishRealtimeEvent() {
    UUID accountId = UUID.randomUUID();
    UUID applicationId = UUID.randomUUID();
    UUID jobId = UUID.randomUUID();
    Application application =
        Application.builder()
            .id(applicationId)
            .candidateId(UUID.randomUUID())
            .jobId(jobId)
            .resumeId(UUID.randomUUID())
            .status(ApplicationStatus.REJECTED)
            .submittedAt(Instant.now())
            .build();

    when(applicationRepository.findById(applicationId))
        .thenReturn(java.util.Optional.of(application));
    when(jobClient.getOwnedJob(jobId, accountId.toString(), "RECRUITER"))
        .thenReturn(new JobSummaryResponse(jobId));

    assertThrows(
        AppException.class,
        () ->
            service.updateApplicationStatus(
                applicationId,
                accountId.toString(),
                "RECRUITER",
                new UpdateApplicationStatusRequest(ApplicationStatus.SCREENING)));

    verify(applicationEventPublisher, never()).publishEvent(any(ApplicationListChangedEvent.class));
  }

  @Test
  void failedPersistenceDoesNotPublishRealtimeEvent() {
    UUID accountId = UUID.randomUUID();
    UUID candidateId = UUID.randomUUID();
    UUID resumeId = UUID.randomUUID();
    UUID jobId = UUID.randomUUID();
    CreateApplicationRequest request = new CreateApplicationRequest(jobId, resumeId);

    when(candidateClient.getMyProfile(accountId.toString(), "CANDIDATE"))
        .thenReturn(new CandidateSummaryResponse(candidateId));
    when(resumeClient.getMyResume(resumeId, accountId.toString(), "CANDIDATE"))
        .thenReturn(new ResumeSummaryResponse(resumeId));
    when(jobClient.getPublishedJob(jobId)).thenReturn(new JobSummaryResponse(jobId));
    when(applicationRepository.save(any(Application.class)))
        .thenThrow(new RuntimeException("db-failed"));

    assertThrows(
        RuntimeException.class,
        () -> service.submitApplication(accountId.toString(), "CANDIDATE", request));

    verify(applicationEventPublisher, never()).publishEvent(any(ApplicationListChangedEvent.class));
  }

  private void stubOwnedJobAndRepository(UUID jobId, Application application) {
    ApplicationResponse mapped =
        new ApplicationResponse(
            application.getId(),
            application.getCandidateId(),
            application.getJobId(),
            application.getResumeId(),
            application.getStatus(),
            application.getSubmittedAt());
    when(jobClient.getOwnedJob(eq(jobId), any(String.class), eq("RECRUITER")))
        .thenReturn(new JobSummaryResponse(jobId));
    when(applicationRepository.findAll(any(Specification.class), any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of(application)));
    when(applicationMapper.toApplicationResponse(application)).thenReturn(mapped);
  }

  private Pageable capturePageable() {
    ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
    verify(applicationRepository).findAll(any(Specification.class), pageableCaptor.capture());
    return pageableCaptor.getValue();
  }

  private ApplicationListChangedEvent capturePublishedListChangedEvent() {
    ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
    verify(applicationEventPublisher, org.mockito.Mockito.atLeastOnce())
        .publishEvent(eventCaptor.capture());
    return eventCaptor.getAllValues().stream()
        .filter(ApplicationListChangedEvent.class::isInstance)
        .map(ApplicationListChangedEvent.class::cast)
        .findFirst()
        .orElseThrow();
  }

  @SuppressWarnings("unchecked")
  private Specification<Application> captureSpecification() {
    ArgumentCaptor<Specification<Application>> specificationCaptor =
        (ArgumentCaptor<Specification<Application>>)
            (ArgumentCaptor<?>) ArgumentCaptor.forClass(Specification.class);
    verify(applicationRepository).findAll(specificationCaptor.capture(), any(Pageable.class));
    return specificationCaptor.getValue();
  }

  private boolean specificationHasOnlyJobConstraint(
      Specification<Application> specification, UUID jobId) {
    Root<Application> root = mock(Root.class);
    @SuppressWarnings("unchecked")
    Path<Object> jobPath = (Path<Object>) mock(Path.class);
    CriteriaQuery<?> query = mock(CriteriaQuery.class);
    CriteriaBuilder criteriaBuilder = mock(CriteriaBuilder.class);
    Predicate jobPredicate = mock(Predicate.class);
    Predicate finalPredicate = mock(Predicate.class);

    when(root.get("jobId")).thenReturn(jobPath);
    when(criteriaBuilder.equal(jobPath, jobId)).thenReturn(jobPredicate);
    when(criteriaBuilder.and(any(Predicate[].class))).thenReturn(finalPredicate);

    Predicate result = specification.toPredicate(root, query, criteriaBuilder);

    verify(root).get("jobId");
    verify(criteriaBuilder).equal(jobPath, jobId);
    verify(root, never()).get("status");
    assertEquals(finalPredicate, result);
    return true;
  }

  private boolean specificationHasJobAndStatus(
      Specification<Application> specification, UUID jobId, ApplicationStatus status) {
    Root<Application> root = mock(Root.class);
    @SuppressWarnings("unchecked")
    Path<Object> jobPath = (Path<Object>) mock(Path.class);
    @SuppressWarnings("unchecked")
    Path<Object> statusPath = (Path<Object>) mock(Path.class);
    CriteriaQuery<?> query = mock(CriteriaQuery.class);
    CriteriaBuilder criteriaBuilder = mock(CriteriaBuilder.class);
    Predicate jobPredicate = mock(Predicate.class);
    Predicate statusPredicate = mock(Predicate.class);
    Predicate finalPredicate = mock(Predicate.class);

    when(root.get("jobId")).thenReturn(jobPath);
    when(root.get("status")).thenReturn(statusPath);
    when(criteriaBuilder.equal(jobPath, jobId)).thenReturn(jobPredicate);
    when(criteriaBuilder.equal(statusPath, status)).thenReturn(statusPredicate);
    when(criteriaBuilder.and(any(Predicate[].class))).thenReturn(finalPredicate);

    Predicate result = specification.toPredicate(root, query, criteriaBuilder);

    verify(root).get("jobId");
    verify(criteriaBuilder).equal(jobPath, jobId);
    verify(root).get("status");
    verify(criteriaBuilder).equal(statusPath, status);
    assertEquals(finalPredicate, result);
    return true;
  }

  private Application buildApplication(UUID jobId, ApplicationStatus status) {
    return Application.builder()
        .id(UUID.randomUUID())
        .candidateId(UUID.randomUUID())
        .jobId(jobId)
        .resumeId(UUID.randomUUID())
        .status(status)
        .submittedAt(Instant.now())
        .build();
  }
}
