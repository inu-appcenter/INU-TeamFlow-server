package com.inuteamflow.server.domain.recruitment.dto.request;

import com.inuteamflow.server.domain.recruitment.enums.ApplicationStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ApplicationStatusUpdateRequest {

    @NotNull
    private ApplicationStatus applicationStatus;

}
