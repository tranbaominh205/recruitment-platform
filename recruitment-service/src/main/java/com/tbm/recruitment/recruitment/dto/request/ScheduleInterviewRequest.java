package com.tbm.recruitment.recruitment.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public record ScheduleInterviewRequest(
    @NotNull @Future Instant scheduledAt,
    @NotBlank @Size(max = 500) String location,
    @Size(max = 1000) String note) {}
