package com.inuteamflow.server.domain.intip.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class IntipNoticeResponse {

    private Long id;
    private String category;
    private String subCategory;
    private String title;
    private String writer;
    private String createDate;
    private String url;
    private String contentText;
    private List<Attachment> attachments;

    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Attachment {
        private String name;
        private String url;
        private String fileType;
    }
}
