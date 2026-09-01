package com.tbm.recruitment.employer.service;

import com.tbm.recruitment.employer.dto.request.CreateCompanyRequest;
import com.tbm.recruitment.employer.dto.request.UpdateCompanyRequest;
import com.tbm.recruitment.employer.dto.response.CompanyResponse;
import com.tbm.recruitment.employer.entity.Company;
import com.tbm.recruitment.employer.exception.AppException;
import com.tbm.recruitment.employer.exception.ErrorCode;
import com.tbm.recruitment.employer.mapper.CompanyMapper;
import com.tbm.recruitment.employer.repository.CompanyRepository;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
}
