package com.inuteamflow.server.domain.report.dto.request;

import com.inuteamflow.server.domain.report.enums.ReportReason;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ReportRequest {

    @NotNull
    private ReportReason reason;

    @Size(max = 1000)
    private String detail;

}
