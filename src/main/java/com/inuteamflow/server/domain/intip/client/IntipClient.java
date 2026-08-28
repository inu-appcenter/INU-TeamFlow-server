package com.inuteamflow.server.domain.intip.client;

import com.inuteamflow.server.domain.intip.dto.IntipNoticeListResponse;
import com.inuteamflow.server.domain.intip.dto.IntipNoticeResponse;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
public class IntipClient {

    private final RestClient restClient;

    public IntipClient(@Value("${intip.base-url}") String baseUrl) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
    }

    /**
     * INTIP 공지사항 목록(본문 포함)을 페이지 단위로 조회한다.
     *
     * @param page 1부터 시작하는 페이지 번호
     * @return 해당 페이지의 공지 목록 (최신순, sort=date 고정)
     */
    public List<IntipNoticeResponse> fetchNoticesWithContent(int page) {
        IntipNoticeListResponse response = restClient
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/notices/with-content")
                        .queryParam("sort", "date")
                        .queryParam("page", page)
                        .build())
                .retrieve()
                .body(IntipNoticeListResponse.class);

        if (response == null || response.getData() == null || response.getData().getContents() == null) {
            return List.of();
        }
        return response.getData().getContents();
    }
}
