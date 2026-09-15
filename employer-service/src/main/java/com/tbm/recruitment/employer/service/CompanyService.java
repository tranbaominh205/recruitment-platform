package com.tbm.recruitment.employer.service;

import com.tbm.recruitment.employer.dto.request.CreateCompanyRequest;
import com.tbm.recruitment.employer.dto.request.UpdateCompanyRequest;
import com.tbm.recruitment.employer.dto.response.AdminCompanyStatisticsResponse;
import com.tbm.recruitment.employer.dto.response.CompanyResponse;
import com.tbm.recruitment.employer.dto.response.PageResponse;
import com.tbm.recruitment.employer.entity.Company;
import com.tbm.recruitment.employer.exception.AppException;
import com.tbm.recruitment.employer.exception.ErrorCode;
import com.tbm.recruitment.employer.mapper.CompanyMapper;
import com.tbm.recruitment.employer.repository.CompanyRepository;
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
  public PageResponse<CompanyResponse> getAdminCompanies(
      String accountIdHeader, String accountRole, String keyword, int page, int size) {
    requireAdminAccount(accountIdHeader, accountRole);
    validatePagination(page, size);

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

          return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };

    PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
    Page<Company> companyPage = companyRepository.findAll(specification, pageRequest);

    return new PageResponse<>(
        companyPage.getContent().stream().map(companyMapper::toCompanyResponse).toList(),
        companyPage.getNumber(),
        companyPage.getSize(),
        companyPage.getTotalElements(),
        companyPage.getTotalPages());
  }

  @Transactional(readOnly = true)
  public CompanyResponse getAdminCompanyById(
      UUID companyId, String accountIdHeader, String accountRole) {
    requireAdminAccount(accountIdHeader, accountRole);
    Company company =
        companyRepository
            .findById(companyId)
            .orElseThrow(() -> new AppException(ErrorCode.COMPANY_NOT_FOUND));
    return companyMapper.toCompanyResponse(company);
  }

  @Transactional(readOnly = true)
  public AdminCompanyStatisticsResponse getAdminStatistics(
      String accountIdHeader, String accountRole) {
    requireAdminAccount(accountIdHeader, accountRole);
    return new AdminCompanyStatisticsResponse(companyRepository.count());
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

  private void validatePagination(int page, int size) {
    if (page < 0 || size < 1 || size > 100) {
      throw new AppException(ErrorCode.INVALID_REQUEST);
    }
  }
}
