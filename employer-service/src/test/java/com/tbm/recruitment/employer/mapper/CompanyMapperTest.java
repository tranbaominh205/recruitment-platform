package com.tbm.recruitment.employer.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.tbm.recruitment.employer.dto.response.CompanyResponse;
import com.tbm.recruitment.employer.entity.Company;
import com.tbm.recruitment.employer.entity.CompanyModerationStatus;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class CompanyMapperTest {

  private final CompanyMapper companyMapper = Mappers.getMapper(CompanyMapper.class);

  @Test
  void toCompanyResponseMapsLegacyNullModerationStatusAsActive() {
    Company company = buildCompany(null);

    CompanyResponse response = companyMapper.toCompanyResponse(company);

    assertEquals(CompanyModerationStatus.ACTIVE, response.moderationStatus());
  }

  @Test
  void toCompanyResponseExposesSuspendedModerationStatus() {
    Company company = buildCompany(CompanyModerationStatus.SUSPENDED);

    CompanyResponse response = companyMapper.toCompanyResponse(company);

    assertEquals(CompanyModerationStatus.SUSPENDED, response.moderationStatus());
  }

  private Company buildCompany(CompanyModerationStatus moderationStatus) {
    Instant now = Instant.now();
    return Company.builder()
        .id(UUID.randomUUID())
        .ownerAccountId(UUID.randomUUID())
        .name("TBM Tech")
        .description("desc")
        .website("https://example.com")
        .industry("Technology")
        .location("HCM")
        .moderationStatus(moderationStatus)
        .createdAt(now)
        .updatedAt(now)
        .build();
  }
}
