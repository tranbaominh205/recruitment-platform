package com.tbm.recruitment.resume.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tbm.recruitment.resume.dto.response.AdminResumeResponse;
import com.tbm.recruitment.resume.dto.response.AdminResumeStatisticsResponse;
import com.tbm.recruitment.resume.dto.response.PageResponse;
import com.tbm.recruitment.resume.entity.Resume;
import com.tbm.recruitment.resume.enums.ResumeStatus;
import com.tbm.recruitment.resume.exception.AppException;
import com.tbm.recruitment.resume.exception.ErrorCode;
import com.tbm.recruitment.resume.mapper.ResumeMapper;
import com.tbm.recruitment.resume.repository.ResumeRepository;
import com.tbm.recruitment.resume.storage.ResumeStorageService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;

@ExtendWith(MockitoExtension.class)
class ResumeServiceAdminTest {

  @Mock private ResumeRepository resumeRepository;
  @Mock private ResumeMapper resumeMapper;
  @Mock private ResumeStorageService resumeStorageService;

  private ResumeService resumeService;

  @BeforeEach
  void setUp() {
    resumeService = new ResumeService(resumeRepository, resumeMapper, resumeStorageService);
  }

  @Test
  void adminCanListResumesWithFiltersAndPagination() {
    Resume resume = buildResume(ResumeStatus.ACTIVE);
    when(resumeRepository.findAllByStatusAndOwnerAccountId(
            org.mockito.ArgumentMatchers.eq(ResumeStatus.ACTIVE),
            org.mockito.ArgumentMatchers.eq(resume.getOwnerAccountId()),
            any()))
        .thenReturn(new PageImpl<>(List.of(resume)));

    PageResponse<AdminResumeResponse> response =
        resumeService.getAdminResumes(
            UUID.randomUUID().toString(),
            "ADMIN",
            "ACTIVE",
            resume.getOwnerAccountId().toString(),
            0,
            20);

    assertEquals(1, response.content().size());
    assertEquals(resume.getOwnerAccountId(), response.content().getFirst().ownerAccountId());
    assertEquals(ResumeStatus.ACTIVE, response.content().getFirst().status());
  }

  @Test
  void nonAdminRolesAreForbiddenForAdminResumeRead() {
    String accountId = UUID.randomUUID().toString();

    AppException exception =
        assertThrows(
            AppException.class,
            () -> resumeService.getAdminResumes(accountId, "CANDIDATE", null, null, 0, 20));

    assertEquals(ErrorCode.FORBIDDEN, exception.getErrorCode());

    AppException recruiterException =
        assertThrows(
            AppException.class,
            () -> resumeService.getAdminResumes(accountId, "RECRUITER", null, null, 0, 20));

    assertEquals(ErrorCode.FORBIDDEN, recruiterException.getErrorCode());
  }

  @Test
  void adminStatisticsCountsAllAndByStatus() {
    when(resumeRepository.count()).thenReturn(11L);
    when(resumeRepository.countByStatus(ResumeStatus.ACTIVE)).thenReturn(8L);
    when(resumeRepository.countByStatus(ResumeStatus.ARCHIVED)).thenReturn(3L);

    AdminResumeStatisticsResponse response =
        resumeService.getAdminStatistics(UUID.randomUUID().toString(), "ADMIN");

    assertEquals(11L, response.totalResumes());
    assertEquals(8L, response.activeResumes());
    assertEquals(3L, response.archivedResumes());
    verify(resumeRepository).count();
  }

  private Resume buildResume(ResumeStatus status) {
    return Resume.builder()
        .id(UUID.randomUUID())
        .ownerAccountId(UUID.randomUUID())
        .displayName("resume.pdf")
        .originalFileName("resume.pdf")
        .contentType("application/pdf")
        .size(1024L)
        .storageKey("key")
        .status(status)
        .createdAt(Instant.now())
        .build();
  }
}
