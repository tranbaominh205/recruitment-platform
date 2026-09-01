package com.tbm.recruitment.candidate.mapper;

import com.tbm.recruitment.candidate.dto.request.CreateCandidateProfileRequest;
import com.tbm.recruitment.candidate.dto.request.UpdateCandidatePreferencesRequest;
import com.tbm.recruitment.candidate.dto.request.UpdateCandidateProfileRequest;
import com.tbm.recruitment.candidate.dto.response.CandidateProfileResponse;
import com.tbm.recruitment.candidate.entity.CandidateProfile;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface CandidateMapper {

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "accountId", ignore = true)
  @Mapping(target = "desiredJobTitles", ignore = true)
  @Mapping(target = "preferredLocations", ignore = true)
  @Mapping(target = "employmentTypes", ignore = true)
  @Mapping(target = "workplaceTypes", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  CandidateProfile toCandidateProfile(CreateCandidateProfileRequest request);

  CandidateProfileResponse toCandidateProfileResponse(CandidateProfile candidateProfile);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "accountId", ignore = true)
  @Mapping(target = "desiredJobTitles", ignore = true)
  @Mapping(target = "preferredLocations", ignore = true)
  @Mapping(target = "employmentTypes", ignore = true)
  @Mapping(target = "workplaceTypes", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  void updateCandidateProfile(
      UpdateCandidateProfileRequest request, @MappingTarget CandidateProfile candidateProfile);

  @BeanMapping(ignoreByDefault = true)
  @Mapping(target = "desiredJobTitles", source = "desiredJobTitles")
  @Mapping(target = "preferredLocations", source = "preferredLocations")
  @Mapping(target = "employmentTypes", source = "employmentTypes")
  @Mapping(target = "workplaceTypes", source = "workplaceTypes")
  void updateCandidatePreferences(
      UpdateCandidatePreferencesRequest request, @MappingTarget CandidateProfile candidateProfile);
}
