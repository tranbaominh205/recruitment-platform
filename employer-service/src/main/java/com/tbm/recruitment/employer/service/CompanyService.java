package com.tbm.recruitment.employer.service;

import com.tbm.recruitment.employer.dto.request.CreateCompanyRequest;
import com.tbm.recruitment.employer.dto.request.UpdateCompanyModerationRequest;
import com.tbm.recruitment.employer.dto.request.UpdateCompanyRequest;
import com.tbm.recruitment.employer.dto.request.UpdateCompanyVerificationRequest;
import com.tbm.recruitment.employer.dto.response.AdminCompanyResponse;
import com.tbm.recruitment.employer.dto.response.AdminCompanyStatisticsResponse;
import com.tbm.recruitment.employer.dto.response.CompanyResponse;
import com.tbm.recruitment.employer.dto.response.PageResponse;
import com.tbm.recruitment.employer.entity.Company;
import com.tbm.recruitment.employer.entity.CompanyModerationStatus;
import com.tbm.recruitment.employer.entity.CompanyVerificationStatus;
import com.tbm.recruitment.employer.exception.AppException;
import com.tbm.recruitment.employer.exception.ErrorCode;
import com.tbm.recruitment.employer.mapper.CompanyMapper;
import com.tbm.recruitment.employer.repository.CompanyRepository;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
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
public class CompanyService {

  CompanyRepository companyRepository;
  CompanyMapper companyMapper;

  @Transactional
  public CompanyResponse createCompany(
      String accountIdHeader, String accountRole, CreateCompanyRequest request) {

    UUID accountId = requireRecruiterAccount(accountIdHeader, accountRole);

    if (companyRepository.existsByOwnerAccountId(accountId)) {
      throw new AppException(ErrorCode.COMPANY_ALREADY_EXISTS);
    }

    Company company = companyMapper.toCompany(request);
    company.setOwnerAccountId(accountId);
    company.setModerationStatus(CompanyModerationStatus.ACTIVE);
    company.setVerificationStatus(CompanyVerificationStatus.UNVERIFIED);
    company.setModerationReason(null);
    company.setModeratedByAccountId(null);
    company.setModeratedAt(null);
    company.setVerificationReason(null);
    company.setVerifiedByAccountId(null);
    company.setVerifiedAt(null);

    Company savedCompany = companyRepository.save(company);

    return companyMapper.toCompanyResponse(savedCompany);
  }

  @Transactional(readOnly = true)
  public CompanyResponse getMyCompany(String accountIdHeader, String accountRole) {

    UUID accountId = requireRecruiterAccount(accountIdHeader, accountRole);

    Company company =
        companyRepository
            .findByOwnerAccountId(accountId)
            .orElseThrow(() -> new AppException(ErrorCode.COMPANY_NOT_FOUND));

    return companyMapper.toCompanyResponse(company);
  }

  @Transactional
  public CompanyResponse updateMyCompany(
      String accountIdHeader, String accountRole, UpdateCompanyRequest request) {

    UUID accountId = requireRecruiterAccount(accountIdHeader, accountRole);

    Company company =
        companyRepository
            .findByOwnerAccountId(accountId)
            .orElseThrow(() -> new AppException(ErrorCode.COMPANY_NOT_FOUND));

    companyMapper.updateCompany(request, company);

    Company savedCompany = companyRepository.save(company);

    return companyMapper.toCompanyResponse(savedCompany);
  }

  @Transactional(readOnly = true)
  public PageResponse<AdminCompanyResponse> getAdminCompanies(
      String accountIdHeader,
      String accountRole,
      String keyword,
      String moderationStatus,
      String verificationStatus,
      int page,
      int size) {
    requireAdminAccount(accountIdHeader, accountRole);
    validatePagination(page, size);

    CompanyModerationStatus moderationStatusFilter = parseModerationStatus(moderationStatus);
    CompanyVerificationStatus verificationStatusFilter =
        parseVerificationStatus(verificationStatus);

    Specification<Company> specification =
        (root, query, criteriaBuilder) -> {
          List<Predicate> predicates = new ArrayList<>();

          if (StringUtils.hasText(keyword)) {
            String likeValue = "%" + keyword.trim().toLowerCase() + "%";
            predicates.add(
                criteriaBuilder.or(
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), likeValue),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("industry")), likeValue),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("location")), likeValue)));
          }

          if (moderationStatusFilter != null) {
            if (moderationStatusFilter == CompanyModerationStatus.ACTIVE) {
              predicates.add(
                  criteriaBuilder.or(
                      criteriaBuilder.equal(
                          root.get("moderationStatus"), CompanyModerationStatus.ACTIVE),
                      criteriaBuilder.isNull(root.get("moderationStatus"))));
            } else {
              predicates.add(
                  criteriaBuilder.equal(root.get("moderationStatus"), moderationStatusFilter));
            }
          }

          if (verificationStatusFilter != null) {
            if (verificationStatusFilter == CompanyVerificationStatus.UNVERIFIED) {
              predicates.add(
                  criteriaBuilder.or(
                      criteriaBuilder.equal(
                          root.get("verificationStatus"), CompanyVerificationStatus.UNVERIFIED),
                      criteriaBuilder.isNull(root.get("verificationStatus"))));
            } else {
              predicates.add(
                  criteriaBuilder.equal(root.get("verificationStatus"), verificationStatusFilter));
            }
          }

          return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };

    PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
    Page<Company> companyPage = companyRepository.findAll(specification, pageRequest);

    return new PageResponse<>(
        companyPage.getContent().stream()
            .filter(company -> matchModerationFilter(company, moderationStatusFilter))
            .filter(company -> matchVerificationFilter(company, verificationStatusFilter))
            .map(companyMapper::toAdminCompanyResponse)
            .toList(),
        companyPage.getNumber(),
        companyPage.getSize(),
        companyPage.getTotalElements(),
        companyPage.getTotalPages());
  }

  @Transactional(readOnly = true)
  public AdminCompanyResponse getAdminCompanyById(
      UUID companyId, String accountIdHeader, String accountRole) {
    requireAdminAccount(accountIdHeader, accountRole);
    Company company =
        companyRepository
            .findById(companyId)
            .orElseThrow(() -> new AppException(ErrorCode.COMPANY_NOT_FOUND));
    return companyMapper.toAdminCompanyResponse(company);
  }

  @Transactional(readOnly = true)
  public AdminCompanyStatisticsResponse getAdminStatistics(
      String accountIdHeader, String accountRole) {
    requireAdminAccount(accountIdHeader, accountRole);
    return new AdminCompanyStatisticsResponse(companyRepository.count());
  }

  @Transactional
  public AdminCompanyResponse updateCompanyModeration(
      UUID companyId,
      String accountIdHeader,
      String accountRole,
      String accountPermissions,
      UpdateCompanyModerationRequest request) {
    UUID moderatorId =
        requireAdminAccountWithPermission(
            accountIdHeader, accountRole, accountPermissions, "COMPANY_MODERATE");

    Company company =
        companyRepository
            .findById(companyId)
            .orElseThrow(() -> new AppException(ErrorCode.COMPANY_NOT_FOUND));

    CompanyModerationStatus nextModerationStatus = request.moderationStatus();
    if (nextModerationStatus == CompanyModerationStatus.ACTIVE) {
      company.setModerationStatus(CompanyModerationStatus.ACTIVE);
      company.setModerationReason(null);
      company.setModeratedByAccountId(null);
      company.setModeratedAt(null);
    } else {
      String reason = requireNonBlankReason(request.reason());
      company.setModerationStatus(CompanyModerationStatus.SUSPENDED);
      company.setModerationReason(reason);
      company.setModeratedByAccountId(moderatorId);
      company.setModeratedAt(java.time.Instant.now());
    }

    Company savedCompany = companyRepository.save(company);
    return companyMapper.toAdminCompanyResponse(savedCompany);
  }

  @Transactional
  public AdminCompanyResponse updateCompanyVerification(
      UUID companyId,
      String accountIdHeader,
      String accountRole,
      String accountPermissions,
      UpdateCompanyVerificationRequest request) {
    UUID verifierId =
        requireAdminAccountWithPermission(
            accountIdHeader, accountRole, accountPermissions, "COMPANY_VERIFY");

    Company company =
        companyRepository
            .findById(companyId)
            .orElseThrow(() -> new AppException(ErrorCode.COMPANY_NOT_FOUND));

    CompanyVerificationStatus nextVerificationStatus = request.verificationStatus();
    if (nextVerificationStatus == CompanyVerificationStatus.UNVERIFIED) {
      company.setVerificationStatus(CompanyVerificationStatus.UNVERIFIED);
      company.setVerificationReason(null);
      company.setVerifiedByAccountId(null);
      company.setVerifiedAt(null);
    } else if (nextVerificationStatus == CompanyVerificationStatus.VERIFIED) {
      company.setVerificationStatus(CompanyVerificationStatus.VERIFIED);
      company.setVerificationReason(normalizeOptionalReason(request.reason()));
      company.setVerifiedByAccountId(verifierId);
      company.setVerifiedAt(java.time.Instant.now());
    } else {
      String reason = requireNonBlankReason(request.reason());
      company.setVerificationStatus(CompanyVerificationStatus.REJECTED);
      company.setVerificationReason(reason);
      company.setVerifiedByAccountId(verifierId);
      company.setVerifiedAt(java.time.Instant.now());
    }

    Company savedCompany = companyRepository.save(company);
    return companyMapper.toAdminCompanyResponse(savedCompany);
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

  private UUID requireAdminAccountWithPermission(
      String accountIdHeader,
      String accountRole,
      String accountPermissions,
      String requiredPermission) {
    UUID accountId = requireAdminAccount(accountIdHeader, accountRole);
    if (accountPermissions == null || accountPermissions.isBlank()) {
      throw new AppException(ErrorCode.FORBIDDEN);
    }

    Set<String> permissions =
        java.util.Arrays.stream(accountPermissions.split(","))
            .map(String::trim)
            .filter(value -> !value.isBlank())
            .collect(java.util.stream.Collectors.toSet());
    if (!permissions.contains(requiredPermission)) {
      throw new AppException(ErrorCode.FORBIDDEN);
    }
    return accountId;
  }

  private CompanyModerationStatus parseModerationStatus(String moderationStatus) {
    if (!StringUtils.hasText(moderationStatus)) {
      return null;
    }

    try {
      return CompanyModerationStatus.valueOf(moderationStatus.trim().toUpperCase());
    } catch (IllegalArgumentException exception) {
      throw new AppException(ErrorCode.INVALID_REQUEST);
    }
  }

  private CompanyVerificationStatus parseVerificationStatus(String verificationStatus) {
    if (!StringUtils.hasText(verificationStatus)) {
      return null;
    }

    try {
      return CompanyVerificationStatus.valueOf(verificationStatus.trim().toUpperCase());
    } catch (IllegalArgumentException exception) {
      throw new AppException(ErrorCode.INVALID_REQUEST);
    }
  }

  private boolean matchModerationFilter(Company company, CompanyModerationStatus filter) {
    if (filter == null) {
      return true;
    }
    if (filter == CompanyModerationStatus.ACTIVE) {
      return company.getModerationStatus() == null
          || company.getModerationStatus() == CompanyModerationStatus.ACTIVE;
    }
    return company.getModerationStatus() == filter;
  }

  private boolean matchVerificationFilter(Company company, CompanyVerificationStatus filter) {
    if (filter == null) {
      return true;
    }
    if (filter == CompanyVerificationStatus.UNVERIFIED) {
      return company.getVerificationStatus() == null
          || company.getVerificationStatus() == CompanyVerificationStatus.UNVERIFIED;
    }
    return company.getVerificationStatus() == filter;
  }

  private String requireNonBlankReason(String reason) {
    if (!StringUtils.hasText(reason)) {
      throw new AppException(ErrorCode.INVALID_REQUEST);
    }
    return reason.trim();
  }

  private String normalizeOptionalReason(String reason) {
    if (!StringUtils.hasText(reason)) {
      return null;
    }
    return reason.trim();
  }

  private void validatePagination(int page, int size) {
    if (page < 0 || size < 1 || size > 100) {
      throw new AppException(ErrorCode.INVALID_REQUEST);
    }
  }
}
