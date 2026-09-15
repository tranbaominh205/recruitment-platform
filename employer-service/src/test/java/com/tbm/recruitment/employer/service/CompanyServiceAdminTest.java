package com.tbm.recruitment.employer.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tbm.recruitment.employer.dto.response.AdminCompanyStatisticsResponse;
import com.tbm.recruitment.employer.dto.response.CompanyResponse;
import com.tbm.recruitment.employer.dto.response.PageResponse;
import com.tbm.recruitment.employer.entity.Company;
import com.tbm.recruitment.employer.exception.AppException;
import com.tbm.recruitment.employer.exception.ErrorCode;
import com.tbm.recruitment.employer.mapper.CompanyMapper;
import com.tbm.recruitment.employer.repository.CompanyRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class CompanyServiceAdminTest {

  @Mock private CompanyRepository companyRepository;
  @Mock private CompanyMapper companyMapper;

  private CompanyService companyService;

  @BeforeEach
  void setUp() {
    companyService = new CompanyService(companyRepository, companyMapper);
  }

  @Test
  void adminCanListCompaniesWithPaginationAndKeyword() {
    Company company = buildCompany();
    CompanyResponse mapped =
        new CompanyResponse(
            company.getId(),
            company.getName(),
            company.getDescription(),
            company.getWebsite(),
            company.getIndustry(),
            company.getLocation(),
            company.getCreatedAt(),
            company.getUpdatedAt());
    when(companyRepository.findAll(
            any(org.springframework.data.jpa.domain.Specification.class), any(PageRequest.class)))
        .thenReturn(new PageImpl<>(List.of(company), PageRequest.of(0, 20), 1));
    when(companyMapper.toCompanyResponse(company)).thenReturn(mapped);

    PageResponse<CompanyResponse> response =
        companyService.getAdminCompanies(UUID.randomUUID().toString(), "ADMIN", "tech", 0, 20);

    assertEquals(1, response.content().size());
    assertEquals(company.getId(), response.content().getFirst().id());
    assertEquals(1L, response.totalElements());
  }

  @Test
  void nonAdminRolesAreForbiddenForAdminCompanyList() {
    String accountId = UUID.randomUUID().toString();

    AppException exception =
        assertThrows(
            AppException.class,
            () -> companyService.getAdminCompanies(accountId, "RECRUITER", null, 0, 20));

    assertEquals(ErrorCode.FORBIDDEN, exception.getErrorCode());

    AppException candidateException =
        assertThrows(
            AppException.class,
            () -> companyService.getAdminCompanies(accountId, "CANDIDATE", null, 0, 20));

    assertEquals(ErrorCode.FORBIDDEN, candidateException.getErrorCode());
  }

  @Test
  void adminStatisticsUsesRepositoryCount() {
    when(companyRepository.count()).thenReturn(7L);

    AdminCompanyStatisticsResponse response =
        companyService.getAdminStatistics(UUID.randomUUID().toString(), "ADMIN");

    assertEquals(7L, response.totalCompanies());
    verify(companyRepository).count();
  }

  private Company buildCompany() {
    Instant now = Instant.now();
    return Company.builder()
        .id(UUID.randomUUID())
        .ownerAccountId(UUID.randomUUID())
        .name("TBM Tech")
        .description("desc")
        .website("https://example.com")
        .industry("Technology")
        .location("HCM")
        .createdAt(now)
        .updatedAt(now)
        .build();
  }
}
