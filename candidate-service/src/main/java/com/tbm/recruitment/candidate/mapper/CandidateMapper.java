package com.tbm.recruitment.candidate.mapper;

import com.tbm.recruitment.candidate.dto.request.CreateCandidateProfileRequest;
import com.tbm.recruitment.candidate.dto.request.UpdateCandidateProfileRequest;
import com.tbm.recruitment.candidate.dto.response.CandidateProfileResponse;
import com.tbm.recruitment.candidate.entity.CandidateProfile;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface CandidateMapper {

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "accountId", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  CandidateProfile toCandidateProfile(CreateCandidateProfileRequest request);

  CandidateProfileResponse toCandidateProfileResponse(CandidateProfile candidateProfile);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "accountId", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  void updateCandidateProfile(
      UpdateCandidateProfileRequest request, @MappingTarget CandidateProfile candidateProfile);
}
