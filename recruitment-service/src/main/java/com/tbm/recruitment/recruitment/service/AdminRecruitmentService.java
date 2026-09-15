package com.tbm.recruitment.recruitment.service;

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
public class AdminRecruitmentService {

  ApplicationRepository applicationRepository;
  InterviewRepository interviewRepository;
  ApplicationMapper applicationMapper;
  InterviewMapper interviewMapper;

  @Transactional(readOnly = true)
  public PageResponse<ApplicationResponse> getApplications(
      String accountIdHeader,
      String accountRole,
      String status,
      UUID jobId,
      UUID candidateId,
      int page,
      int size) {
    requireAdminAccount(accountIdHeader, accountRole);
    validatePagination(page, size);

    ApplicationStatus statusFilter = parseStatus(status);
    Specification<Application> specification =
        (root, query, criteriaBuilder) -> {
          List<Predicate> predicates = new ArrayList<>();

          if (statusFilter != null) {
            predicates.add(criteriaBuilder.equal(root.get("status"), statusFilter));
          }
          if (jobId != null) {
            predicates.add(criteriaBuilder.equal(root.get("jobId"), jobId));
          }
          if (candidateId != null) {
            predicates.add(criteriaBuilder.equal(root.get("candidateId"), candidateId));
          }

          return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };

    PageRequest pageRequest =
        PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "submittedAt"));
    Page<Application> result = applicationRepository.findAll(specification, pageRequest);

    return new PageResponse<>(
        result.getContent().stream().map(applicationMapper::toApplicationResponse).toList(),
        result.getNumber(),
        result.getSize(),
        result.getTotalElements(),
        result.getTotalPages());
  }

  @Transactional(readOnly = true)
  public ApplicationResponse getApplicationById(
      UUID applicationId, String accountIdHeader, String accountRole) {
    requireAdminAccount(accountIdHeader, accountRole);
    Application application =
        applicationRepository
            .findById(applicationId)
            .orElseThrow(() -> new AppException(ErrorCode.APPLICATION_NOT_FOUND));
    return applicationMapper.toApplicationResponse(application);
  }

  @Transactional(readOnly = true)
  public PageResponse<InterviewResponse> getInterviews(
      String accountIdHeader, String accountRole, int page, int size) {
    requireAdminAccount(accountIdHeader, accountRole);
    validatePagination(page, size);

    PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
    Page<Interview> result = interviewRepository.findAll(pageRequest);

    return new PageResponse<>(
        result.getContent().stream().map(interviewMapper::toInterviewResponse).toList(),
        result.getNumber(),
        result.getSize(),
        result.getTotalElements(),
        result.getTotalPages());
  }

  @Transactional(readOnly = true)
  public InterviewResponse getInterviewById(
      UUID interviewId, String accountIdHeader, String accountRole) {
    requireAdminAccount(accountIdHeader, accountRole);
    Interview interview =
        interviewRepository
            .findById(interviewId)
            .orElseThrow(() -> new AppException(ErrorCode.INTERVIEW_NOT_FOUND));
    return interviewMapper.toInterviewResponse(interview);
  }

  @Transactional(readOnly = true)
  public AdminRecruitmentStatisticsResponse getStatistics(
      String accountIdHeader, String accountRole) {
    requireAdminAccount(accountIdHeader, accountRole);

    long totalApplications = applicationRepository.count();
    long submitted = applicationRepository.countByStatus(ApplicationStatus.SUBMITTED);
    long screening = applicationRepository.countByStatus(ApplicationStatus.SCREENING);
    long interview = applicationRepository.countByStatus(ApplicationStatus.INTERVIEW);
    long offer = applicationRepository.countByStatus(ApplicationStatus.OFFER);
    long hired = applicationRepository.countByStatus(ApplicationStatus.HIRED);
    long rejected = applicationRepository.countByStatus(ApplicationStatus.REJECTED);
    long withdrawn = applicationRepository.countByStatus(ApplicationStatus.WITHDRAWN);
    long totalInterviews = interviewRepository.count();

    return new AdminRecruitmentStatisticsResponse(
        totalApplications,
        submitted,
        screening,
        interview,
        offer,
        hired,
        rejected,
        withdrawn,
        totalInterviews);
  }

  private UUID requireAdminAccount(String accountIdHeader, String accountRole) {
    if (accountIdHeader == null || accountIdHeader.isBlank()) {
      throw new AppException(ErrorCode.UNAUTHENTICATED);
    }
    if (!"ADMIN".equals(accountRole)) {
      throw new AppException(ErrorCode.FORBIDDEN);
    }
    try {
      return UUID.fromString(accountIdHeader);
    } catch (IllegalArgumentException exception) {
      throw new AppException(ErrorCode.UNAUTHENTICATED);
    }
  }

  private ApplicationStatus parseStatus(String status) {
    if (!StringUtils.hasText(status)) {
      return null;
    }
    try {
      return ApplicationStatus.valueOf(status.trim().toUpperCase());
    } catch (IllegalArgumentException exception) {
      throw new AppException(ErrorCode.INVALID_REQUEST);
    }
  }

  private void validatePagination(int page, int size) {
    if (page < 0 || size < 1 || size > 100) {
      throw new AppException(ErrorCode.INVALID_REQUEST);
    }
  }
}
