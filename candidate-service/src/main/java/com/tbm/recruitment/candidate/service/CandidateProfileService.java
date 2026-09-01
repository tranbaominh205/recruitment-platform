package com.tbm.recruitment.candidate.service;

import com.tbm.recruitment.candidate.dto.request.CreateCandidateProfileRequest;
import com.tbm.recruitment.candidate.dto.request.UpdateCandidateProfileRequest;
import com.tbm.recruitment.candidate.dto.response.CandidateProfileResponse;
import com.tbm.recruitment.candidate.entity.CandidateProfile;
import com.tbm.recruitment.candidate.exception.AppException;
import com.tbm.recruitment.candidate.exception.ErrorCode;
import com.tbm.recruitment.candidate.mapper.CandidateMapper;
import com.tbm.recruitment.candidate.repository.CandidateProfileRepository;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CandidateProfileService {

  CandidateProfileRepository candidateProfileRepository;
  CandidateMapper candidateMapper;

  @Transactional
  public CandidateProfileResponse createProfile(
      String accountIdHeader, String accountRole, CreateCandidateProfileRequest request) {

    UUID accountId = requireCandidateAccount(accountIdHeader, accountRole);

    if (candidateProfileRepository.existsByAccountId(accountId)) {
      throw new AppException(ErrorCode.PROFILE_ALREADY_EXISTS);
    }

    CandidateProfile candidateProfile = candidateMapper.toCandidateProfile(request);
    candidateProfile.setAccountId(accountId);

    CandidateProfile savedCandidateProfile = candidateProfileRepository.save(candidateProfile);

    return candidateMapper.toCandidateProfileResponse(savedCandidateProfile);
  }

  @Transactional(readOnly = true)
  public CandidateProfileResponse getMyProfile(String accountIdHeader, String accountRole) {

    UUID accountId = requireCandidateAccount(accountIdHeader, accountRole);

    CandidateProfile candidateProfile =
        candidateProfileRepository
            .findByAccountId(accountId)
            .orElseThrow(() -> new AppException(ErrorCode.PROFILE_NOT_FOUND));

    return candidateMapper.toCandidateProfileResponse(candidateProfile);
  }

  @Transactional
  public CandidateProfileResponse updateMyProfile(
      String accountIdHeader, String accountRole, UpdateCandidateProfileRequest request) {

    UUID accountId = requireCandidateAccount(accountIdHeader, accountRole);

    CandidateProfile candidateProfile =
        candidateProfileRepository
            .findByAccountId(accountId)
            .orElseThrow(() -> new AppException(ErrorCode.PROFILE_NOT_FOUND));

    candidateMapper.updateCandidateProfile(request, candidateProfile);

    CandidateProfile savedCandidateProfile = candidateProfileRepository.save(candidateProfile);

    return candidateMapper.toCandidateProfileResponse(savedCandidateProfile);
  }

  private UUID requireCandidateAccount(String accountIdHeader, String accountRole) {

    if (accountIdHeader == null || accountIdHeader.isBlank()) {
      throw new AppException(ErrorCode.UNAUTHENTICATED);
    }

    if (!"CANDIDATE".equals(accountRole)) {
      throw new AppException(ErrorCode.FORBIDDEN);
    }

    try {
      return UUID.fromString(accountIdHeader);
    } catch (IllegalArgumentException exception) {
      throw new AppException(ErrorCode.UNAUTHENTICATED);
    }
  }
}
