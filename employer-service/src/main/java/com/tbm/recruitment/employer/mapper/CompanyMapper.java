package com.tbm.recruitment.employer.mapper;

import com.tbm.recruitment.employer.dto.request.CreateCompanyRequest;
import com.tbm.recruitment.employer.dto.request.UpdateCompanyRequest;
import com.tbm.recruitment.employer.dto.response.AdminCompanyResponse;
import com.tbm.recruitment.employer.dto.response.CompanyResponse;
import com.tbm.recruitment.employer.entity.Company;
import com.tbm.recruitment.employer.entity.CompanyModerationStatus;
import com.tbm.recruitment.employer.entity.CompanyVerificationStatus;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface CompanyMapper {

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "ownerAccountId", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  Company toCompany(CreateCompanyRequest request);

  CompanyResponse toCompanyResponse(Company company);

  @Mapping(target = "moderationStatus", expression = "java(effectiveModerationStatus(company))")
  @Mapping(target = "verificationStatus", expression = "java(effectiveVerificationStatus(company))")
  AdminCompanyResponse toAdminCompanyResponse(Company company);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "ownerAccountId", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  void updateCompany(UpdateCompanyRequest request, @MappingTarget Company company);

  default CompanyModerationStatus effectiveModerationStatus(Company company) {
    if (company.getModerationStatus() == null) {
      return CompanyModerationStatus.ACTIVE;
    }
    return company.getModerationStatus();
  }

  default CompanyVerificationStatus effectiveVerificationStatus(Company company) {
    if (company.getVerificationStatus() == null) {
      return CompanyVerificationStatus.UNVERIFIED;
    }
    return company.getVerificationStatus();
  }
}
