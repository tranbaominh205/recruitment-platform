package com.tbm.recruitment.resume.mapper;

import com.tbm.recruitment.resume.dto.response.ResumeResponse;
import com.tbm.recruitment.resume.entity.Resume;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ResumeMapper {

  ResumeResponse toResumeResponse(Resume resume);
}
