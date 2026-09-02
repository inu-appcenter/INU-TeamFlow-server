package com.inuteamflow.server.domain.intip.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inuteamflow.server.domain.intip.dto.AiChatCompletionRequest;
import com.inuteamflow.server.domain.intip.dto.AiChatCompletionResponse;
import com.inuteamflow.server.domain.intip.dto.AiChatMessage;
import com.inuteamflow.server.domain.intip.dto.AiClassificationResult;
import com.inuteamflow.server.domain.intip.dto.IntipNoticeResponse;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Slf4j
@Component
public class AiClassificationClient {

    private static final String SYSTEM_PROMPT = """
            너는 인천대학교 공지사항을 학생 팀플 서비스 "모이미"에 필요한 데이터인지 분류하는 분류기다.
            아래 기준에 따라 이 공지가 모이미에 노출할 가치가 있는지, 있다면 어떤 카테고리인지 판단하라.

            [카테고리 정의]
            - CONTEST: 공모전/대회 관련 공지
            - CLUB: 동아리 모집/활동 관련 공지
            - EXTERNAL_ACTIVITY: 대외활동(서포터즈, 봉사, 프로그램 등) 공지
            - INTERN: 인턴/채용 연계 공지

            [불필요한 데이터 예시 - isRelevant=false]
            - 행정/입찰 공지
            - 시설물 관련 공지
            - 특정 학과 성적/수강 관련 공지
            - 위 4개 카테고리 중 어디에도 명확히 속하지 않는 공지

            아래 JSON 형식으로 답하라.
            {"isRelevant": true, "category": "CONTEST", "reason": "분류 근거"}
            isRelevant가 false면 category와 reason은 null로 채워라.
            """;

    private static final int MAX_RETRY = 3;

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String model;

    public AiClassificationClient(
            @Value("${ai.base-url}") String baseUrl,
            @Value("${ai.api-key}") String apiKey,
            @Value("${ai.model}") String model,
            ObjectMapper objectMapper) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .build();
        this.objectMapper = objectMapper;
        this.model = model;
    }

    /**
     * 공지 1건을 AI 서버에 분류 요청한다.
     *
     * <p>AI 서버가 response_format(json_object)을 통한 JSON 강제 출력을 지원하므로,
     * 응답 전체가 곧바로 유효한 JSON이라고 가정하고 파싱한다.</p>
     *
     * @throws AiClassificationParseException 응답 JSON 파싱에 실패한 경우 (재시도 대상 아님, 호출부에서 스킵 처리)
     * @throws RestClientException AI 서버 호출 자체가 실패한 경우 (호출부에서 재시도)
     */
    public AiClassificationResult classify(IntipNoticeResponse notice) {
        String userContent = "제목: " + notice.getTitle() + "\n본문: " + notice.getContentText();

        AiChatCompletionRequest request = AiChatCompletionRequest.builder()
                .model(model)
                .messages(List.of(new AiChatMessage("system", SYSTEM_PROMPT), new AiChatMessage("user", userContent)))
                .temperature(0)
                .chatTemplateKwargs(Map.of("enable_thinking", false))
                .responseFormat(Map.of("type", "json_object"))
                .build();

        String rawContent = callWithRetry(request);
        return parseJson(rawContent);
    }

    private String callWithRetry(AiChatCompletionRequest request) {
        RestClientException lastError = null;

        for (int attempt = 1; attempt <= MAX_RETRY; attempt++) {
            try {
                AiChatCompletionResponse response = restClient
                        .post()
                        .uri("/v1/chat/completions")
                        .body(request)
                        .retrieve()
                        .body(AiChatCompletionResponse.class);

                return response == null ? null : response.getFirstMessageContent();
            } catch (RestClientException e) {
                lastError = e;
                log.warn("[AI 분류] 호출 실패 (시도 {}/{})", attempt, MAX_RETRY, e);
                sleep(attempt);
            }
        }
        throw lastError;
    }

    private void sleep(int attempt) {
        try {
            Thread.sleep(1000L * (1L << (attempt - 1))); // 1s, 2s, 4s 지수 백오프
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private AiClassificationResult parseJson(String rawContent) {
        if (rawContent == null) {
            throw new AiClassificationParseException("AI 응답이 비어있음", null);
        }

        try {
            return objectMapper.readValue(rawContent, AiClassificationResult.class);
        } catch (Exception e) {
            throw new AiClassificationParseException("AI 응답 JSON 파싱 실패: " + rawContent, e);
        }
    }
}
