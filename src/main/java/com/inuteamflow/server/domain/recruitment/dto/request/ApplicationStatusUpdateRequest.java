package com.inuteamflow.server.domain.recruitment.dto.request;

import com.inuteamflow.server.global.enums.Status;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ApplicationStatusUpdateRequest {

    @NotNull
    private Status applicationStatus;

}
