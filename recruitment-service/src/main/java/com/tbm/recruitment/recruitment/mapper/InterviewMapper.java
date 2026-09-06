package com.tbm.recruitment.recruitment.mapper;

import com.tbm.recruitment.recruitment.dto.response.InterviewResponse;
import com.tbm.recruitment.recruitment.entity.Interview;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface InterviewMapper {

  InterviewResponse toInterviewResponse(Interview interview);
}
