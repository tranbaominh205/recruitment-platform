package com.tbm.recruitment.employer.mapper;

import com.tbm.recruitment.employer.dto.request.CreateCompanyRequest;
import com.tbm.recruitment.employer.dto.request.UpdateCompanyRequest;
import com.tbm.recruitment.employer.dto.response.CompanyResponse;
import com.tbm.recruitment.employer.entity.Company;
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

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "ownerAccountId", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  void updateCompany(UpdateCompanyRequest request, @MappingTarget Company company);
}
