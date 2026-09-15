package com.tbm.recruitment.employer.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class CompanyServiceAdminTest {

  @Mock private CompanyRepository companyRepository;
  @Mock private CompanyMapper companyMapper;

  private CompanyService companyService;

  @BeforeEach
  void setUp() {
    companyService = new CompanyService(companyRepository, companyMapper);
    lenient()
        .when(companyMapper.toCompanyResponse(any(Company.class)))
        .thenAnswer(invocation -> mapCompanyResponse(invocation.getArgument(0)));
    lenient()
        .when(companyMapper.toAdminCompanyResponse(any(Company.class)))
        .thenAnswer(invocation -> mapAdminCompanyResponse(invocation.getArgument(0)));
  }

  @Test
  void newCompanyDefaultsToActiveAndUnverified() {
    String recruiterId = UUID.randomUUID().toString();
    CreateCompanyRequest request =
        new CreateCompanyRequest("TBM Tech", "desc", "https://example.com", "Tech", "HCM");
    Company mapped = buildCompany(null, null);
    when(companyRepository.existsByOwnerAccountId(UUID.fromString(recruiterId))).thenReturn(false);
    when(companyMapper.toCompany(request)).thenReturn(mapped);
    when(companyRepository.save(any(Company.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    CompanyResponse response = companyService.createCompany(recruiterId, "RECRUITER", request);

    ArgumentCaptor<Company> captor = ArgumentCaptor.forClass(Company.class);
    verify(companyRepository).save(captor.capture());
    assertEquals(CompanyModerationStatus.ACTIVE, captor.getValue().getModerationStatus());
    assertEquals(CompanyVerificationStatus.UNVERIFIED, captor.getValue().getVerificationStatus());
    assertEquals(mapped.getId(), response.id());
  }

  @Test
  void legacyNullStatesMapToEffectiveDefaultsForAdminResponsesAndFilters() {
    Company legacy = buildCompany(null, null);
    Company activeVerified =
        buildCompany(CompanyModerationStatus.ACTIVE, CompanyVerificationStatus.VERIFIED);
    Company suspendedRejected =
        buildCompany(CompanyModerationStatus.SUSPENDED, CompanyVerificationStatus.REJECTED);
    when(companyRepository.findAll(any(Specification.class), any(PageRequest.class)))
        .thenReturn(
            new PageImpl<>(
                List.of(legacy, activeVerified, suspendedRejected), PageRequest.of(0, 20), 3));

    PageResponse<AdminCompanyResponse> response =
        companyService.getAdminCompanies(
            UUID.randomUUID().toString(), "ADMIN", null, "ACTIVE", "UNVERIFIED", 0, 20);

    assertEquals(1, response.content().size());
    assertEquals(CompanyModerationStatus.ACTIVE, response.content().getFirst().moderationStatus());
    assertEquals(
        CompanyVerificationStatus.UNVERIFIED, response.content().getFirst().verificationStatus());
  }

  @Test
  void adminCanSuspendAndRestoreCompanyWithoutDeletingIt() {
    UUID companyId = UUID.randomUUID();
    UUID adminId = UUID.randomUUID();
    Company company =
        buildCompany(CompanyModerationStatus.ACTIVE, CompanyVerificationStatus.UNVERIFIED);
    company.setId(companyId);
    when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
    when(companyRepository.save(any(Company.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    AdminCompanyResponse suspended =
        companyService.updateCompanyModeration(
            companyId,
            adminId.toString(),
            "ADMIN",
            "COMPANY_MODERATE",
            new UpdateCompanyModerationRequest(CompanyModerationStatus.SUSPENDED, "  policy  "));

    assertEquals(CompanyModerationStatus.SUSPENDED, suspended.moderationStatus());
    assertEquals("policy", suspended.moderationReason());
    assertEquals(CompanyVerificationStatus.UNVERIFIED, suspended.verificationStatus());
    assertEquals(companyId, suspended.id());

    AdminCompanyResponse restored =
        companyService.updateCompanyModeration(
            companyId,
            adminId.toString(),
            "ADMIN",
            "COMPANY_MODERATE",
            new UpdateCompanyModerationRequest(CompanyModerationStatus.ACTIVE, null));

    assertEquals(CompanyModerationStatus.ACTIVE, restored.moderationStatus());
    assertNull(restored.moderationReason());
    assertNull(restored.moderatedAt());
    verify(companyRepository, never()).delete(any(Company.class));
  }

  @Test
  void suspendedRequiresReason() {
    UUID companyId = UUID.randomUUID();
    when(companyRepository.findById(companyId)).thenReturn(Optional.of(buildCompany(null, null)));

    AppException exception =
        assertThrows(
            AppException.class,
            () ->
                companyService.updateCompanyModeration(
                    companyId,
                    UUID.randomUUID().toString(),
                    "ADMIN",
                    "COMPANY_MODERATE",
                    new UpdateCompanyModerationRequest(CompanyModerationStatus.SUSPENDED, "   ")));

    assertEquals(ErrorCode.INVALID_REQUEST, exception.getErrorCode());
  }

  @Test
  void adminWithoutCompanyModerateGetsForbidden() {
    AppException exception =
        assertThrows(
            AppException.class,
            () ->
                companyService.updateCompanyModeration(
                    UUID.randomUUID(),
                    UUID.randomUUID().toString(),
                    "ADMIN",
                    "COMPANY_VERIFY",
                    new UpdateCompanyModerationRequest(
                        CompanyModerationStatus.SUSPENDED, "reason")));

    assertEquals(ErrorCode.FORBIDDEN, exception.getErrorCode());
  }

  @Test
  void adminCanVerifyRejectAndResetCompany() {
    UUID companyId = UUID.randomUUID();
    UUID adminId = UUID.randomUUID();
    Company company =
        buildCompany(CompanyModerationStatus.ACTIVE, CompanyVerificationStatus.UNVERIFIED);
    company.setId(companyId);
    when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
    when(companyRepository.save(any(Company.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    AdminCompanyResponse verified =
        companyService.updateCompanyVerification(
            companyId,
            adminId.toString(),
            "ADMIN",
            "COMPANY_VERIFY",
            new UpdateCompanyVerificationRequest(
                CompanyVerificationStatus.VERIFIED, "  optional  "));
    assertEquals(CompanyVerificationStatus.VERIFIED, verified.verificationStatus());
    assertEquals("optional", verified.verificationReason());

    AdminCompanyResponse rejected =
        companyService.updateCompanyVerification(
            companyId,
            adminId.toString(),
            "ADMIN",
            "COMPANY_VERIFY",
            new UpdateCompanyVerificationRequest(
                CompanyVerificationStatus.REJECTED, "  docs missing  "));
    assertEquals(CompanyVerificationStatus.REJECTED, rejected.verificationStatus());
    assertEquals("docs missing", rejected.verificationReason());

    AdminCompanyResponse reset =
        companyService.updateCompanyVerification(
            companyId,
            adminId.toString(),
            "ADMIN",
            "COMPANY_VERIFY",
            new UpdateCompanyVerificationRequest(CompanyVerificationStatus.UNVERIFIED, null));
    assertEquals(CompanyVerificationStatus.UNVERIFIED, reset.verificationStatus());
    assertNull(reset.verificationReason());
    assertNull(reset.verifiedAt());
    verify(companyRepository, never()).delete(any(Company.class));
  }

  @Test
  void rejectedRequiresReason() {
    UUID companyId = UUID.randomUUID();
    when(companyRepository.findById(companyId)).thenReturn(Optional.of(buildCompany(null, null)));

    AppException exception =
        assertThrows(
            AppException.class,
            () ->
                companyService.updateCompanyVerification(
                    companyId,
                    UUID.randomUUID().toString(),
                    "ADMIN",
                    "COMPANY_VERIFY",
                    new UpdateCompanyVerificationRequest(CompanyVerificationStatus.REJECTED, " ")));

    assertEquals(ErrorCode.INVALID_REQUEST, exception.getErrorCode());
  }

  @Test
  void adminWithoutCompanyVerifyGetsForbidden() {
    AppException exception =
        assertThrows(
            AppException.class,
            () ->
                companyService.updateCompanyVerification(
                    UUID.randomUUID(),
                    UUID.randomUUID().toString(),
                    "ADMIN",
                    "COMPANY_MODERATE",
                    new UpdateCompanyVerificationRequest(
                        CompanyVerificationStatus.VERIFIED, null)));

    assertEquals(ErrorCode.FORBIDDEN, exception.getErrorCode());
  }

  @Test
  void companyModeratePermissionCannotVerifyAndCompanyVerifyCannotModerate() {
    UUID companyId = UUID.randomUUID();

    AppException moderateOnly =
        assertThrows(
            AppException.class,
            () ->
                companyService.updateCompanyVerification(
                    companyId,
                    UUID.randomUUID().toString(),
                    "ADMIN",
                    "COMPANY_MODERATE",
                    new UpdateCompanyVerificationRequest(
                        CompanyVerificationStatus.VERIFIED, null)));
    assertEquals(ErrorCode.FORBIDDEN, moderateOnly.getErrorCode());

    AppException verifyOnly =
        assertThrows(
            AppException.class,
            () ->
                companyService.updateCompanyModeration(
                    companyId,
                    UUID.randomUUID().toString(),
                    "ADMIN",
                    "COMPANY_VERIFY",
                    new UpdateCompanyModerationRequest(
                        CompanyModerationStatus.SUSPENDED, "reason")));
    assertEquals(ErrorCode.FORBIDDEN, verifyOnly.getErrorCode());
  }

  @Test
  void nonAdminGetsForbiddenForAdminMutations() {
    AppException exception =
        assertThrows(
            AppException.class,
            () ->
                companyService.updateCompanyModeration(
                    UUID.randomUUID(),
                    UUID.randomUUID().toString(),
                    "RECRUITER",
                    "COMPANY_MODERATE",
                    new UpdateCompanyModerationRequest(
                        CompanyModerationStatus.SUSPENDED, "reason")));

    assertEquals(ErrorCode.FORBIDDEN, exception.getErrorCode());
  }

  @Test
  void existingRecruiterCreateGetUpdateBehaviorRemainsIntact() {
    String recruiterId = UUID.randomUUID().toString();
    UUID ownerAccountId = UUID.fromString(recruiterId);
    Company createdCompany = buildCompany(null, null);
    createdCompany.setOwnerAccountId(ownerAccountId);
    Company existingCompany =
        buildCompany(CompanyModerationStatus.SUSPENDED, CompanyVerificationStatus.REJECTED);
    existingCompany.setOwnerAccountId(ownerAccountId);
    when(companyRepository.existsByOwnerAccountId(ownerAccountId)).thenReturn(false);
    when(companyMapper.toCompany(any(CreateCompanyRequest.class))).thenReturn(createdCompany);
    doAnswer(
            invocation -> {
              UpdateCompanyRequest request = invocation.getArgument(0);
              Company target = invocation.getArgument(1);
              target.setName(request.name());
              target.setDescription(request.description());
              target.setWebsite(request.website());
              target.setIndustry(request.industry());
              target.setLocation(request.location());
              return null;
            })
        .when(companyMapper)
        .updateCompany(any(UpdateCompanyRequest.class), any(Company.class));
    when(companyRepository.save(any(Company.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(companyRepository.findByOwnerAccountId(ownerAccountId))
        .thenReturn(Optional.of(existingCompany));

    CompanyResponse created =
        companyService.createCompany(
            recruiterId,
            "RECRUITER",
            new CreateCompanyRequest("TBM Tech", "desc", "https://example.com", "Tech", "HCM"));
    CompanyResponse found = companyService.getMyCompany(recruiterId, "RECRUITER");
    CompanyResponse updated =
        companyService.updateMyCompany(
            recruiterId,
            "RECRUITER",
            new UpdateCompanyRequest("TBM Tech 2", "desc2", "https://example.com", "Tech", "HCM"));

    assertEquals(createdCompany.getId(), created.id());
    assertEquals(existingCompany.getId(), found.id());
    assertEquals(CompanyModerationStatus.SUSPENDED, found.moderationStatus());
    assertEquals("TBM Tech 2", updated.name());
    assertEquals(CompanyVerificationStatus.REJECTED, existingCompany.getVerificationStatus());
  }

  @Test
  void adminStatusFilteringWorks() {
    Company legacyActive = buildCompany(null, null);
    Company suspendedVerified =
        buildCompany(CompanyModerationStatus.SUSPENDED, CompanyVerificationStatus.VERIFIED);
    when(companyRepository.findAll(any(Specification.class), any(PageRequest.class)))
        .thenReturn(
            new PageImpl<>(List.of(legacyActive, suspendedVerified), PageRequest.of(0, 20), 2));

    PageResponse<AdminCompanyResponse> response =
        companyService.getAdminCompanies(
            UUID.randomUUID().toString(), "ADMIN", null, "ACTIVE", "UNVERIFIED", 0, 20);

    assertEquals(1, response.content().size());
    assertEquals(CompanyModerationStatus.ACTIVE, response.content().getFirst().moderationStatus());
    assertEquals(
        CompanyVerificationStatus.UNVERIFIED, response.content().getFirst().verificationStatus());
  }

  @Test
  void adminStatisticsUsesRepositoryCount() {
    when(companyRepository.count()).thenReturn(7L);

    AdminCompanyStatisticsResponse response =
        companyService.getAdminStatistics(UUID.randomUUID().toString(), "ADMIN");

    assertEquals(7L, response.totalCompanies());
    verify(companyRepository).count();
  }

  private Company buildCompany(
      CompanyModerationStatus moderationStatus, CompanyVerificationStatus verificationStatus) {
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
        .verificationStatus(verificationStatus)
        .createdAt(now)
        .updatedAt(now)
        .build();
  }

  private CompanyResponse mapCompanyResponse(Company company) {
    return new CompanyResponse(
        company.getId(),
        company.getName(),
        company.getDescription(),
        company.getWebsite(),
        company.getIndustry(),
        company.getLocation(),
        company.getModerationStatus() == null
            ? CompanyModerationStatus.ACTIVE
            : company.getModerationStatus(),
        company.getCreatedAt(),
        company.getUpdatedAt());
  }

  private AdminCompanyResponse mapAdminCompanyResponse(Company company) {
    return new AdminCompanyResponse(
        company.getId(),
        company.getName(),
        company.getDescription(),
        company.getWebsite(),
        company.getIndustry(),
        company.getLocation(),
        company.getCreatedAt(),
        company.getUpdatedAt(),
        company.getModerationStatus() == null
            ? CompanyModerationStatus.ACTIVE
            : company.getModerationStatus(),
        company.getModerationReason(),
        company.getModeratedAt(),
        company.getVerificationStatus() == null
            ? CompanyVerificationStatus.UNVERIFIED
            : company.getVerificationStatus(),
        company.getVerificationReason(),
        company.getVerifiedAt());
  }
}
