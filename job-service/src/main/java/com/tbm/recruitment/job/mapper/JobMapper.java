package com.tbm.recruitment.job.mapper;

import com.tbm.recruitment.job.dto.request.CreateJobRequest;
import com.tbm.recruitment.job.dto.request.UpdateJobRequest;
import com.tbm.recruitment.job.dto.response.JobResponse;
import com.tbm.recruitment.job.entity.Job;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface JobMapper {

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "companyId", ignore = true)
  @Mapping(target = "createdByAccountId", ignore = true)
  @Mapping(target = "status", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  Job toJob(CreateJobRequest request);

  JobResponse toJobResponse(Job job);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "companyId", ignore = true)
  @Mapping(target = "createdByAccountId", ignore = true)
  @Mapping(target = "status", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  void updateJob(UpdateJobRequest request, @MappingTarget Job job);
}
