package com.tbm.recruitment.recruitment.mapper;

import com.tbm.recruitment.recruitment.dto.response.ApplicationResponse;
import com.tbm.recruitment.recruitment.entity.Application;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ApplicationMapper {

  ApplicationResponse toApplicationResponse(Application application);
}
