package com.inuteamflow.server.domain.intip.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class IntipNoticeListResponse {

    private Data data;
    private String msg;

    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Data {
        private int pages;
        private int total;
        private List<IntipNoticeResponse> contents;
    }
}
