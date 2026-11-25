package com.inuteamflow.server.domain.intip.client;

import com.inuteamflow.server.domain.intip.dto.IntipNoticeListResponse;
import com.inuteamflow.server.domain.intip.dto.IntipNoticeResponse;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
public class IntipClient {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(10);

    private final RestClient restClient;

    public IntipClient(@Value("${intip.base-url}") String baseUrl) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(CONNECT_TIMEOUT);
        requestFactory.setReadTimeout(READ_TIMEOUT);
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
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

    /**
     * 공지 첨부파일을 내려받는다.
     *
     * @param url 학교 서버의 첨부파일 다운로드 URL (절대 경로라 base-url을 쓰지 않음)
     * @return 파일 바이트
     */
    public byte[] downloadFile(String url) {
        return restClient.get().uri(URI.create(url)).retrieve().body(byte[].class);
    }
}
