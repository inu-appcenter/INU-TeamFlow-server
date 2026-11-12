package com.inuteamflow.server.domain.intip.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inuteamflow.server.domain.intip.dto.AiChatCompletionRequest;
import com.inuteamflow.server.domain.intip.dto.AiChatCompletionResponse;
import com.inuteamflow.server.domain.intip.dto.AiChatMessage;
import com.inuteamflow.server.domain.intip.dto.AiClassificationResult;
import com.inuteamflow.server.domain.intip.dto.IntipNoticeResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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

    private static final String FORMAT_PROMPT = """
            너는 대학교 공지 본문을 읽기 좋게 줄을 정리하는 편집기다.

            [규칙]
            - 원문의 글자(한글, 영문, 숫자, 기호, ⟦1⟧ 같은 자리표시자)를 하나도 추가, 삭제, 수정하지 마라. 순서도 바꾸지 마라.
            - 할 수 있는 것은 아래 세 가지뿐이다.
              1. 줄바꿈과 빈 줄을 추가하거나 삭제한다.
              2. 글머리 기호가 없는 섹션 제목 줄 맨 앞에 "■ "를 붙인다.
              3. 글머리 기호가 없는 항목 줄 맨 앞에 "• "를 붙인다.
            - 섹션(가., 1., □ 등) 사이에는 빈 줄을 넣어라.
            - 이미 글머리 기호(-, ※, *, 1), 가., □, ○ 등)로 시작하는 줄에는 기호를 붙이지 마라.
            - 표처럼 한 줄에 이어진 내용은 행 단위로 줄을 나눠라.
            - 설명이나 코드 블록 없이 정리된 본문만 출력하라.
            """;

    private static final int MAX_RETRY = 3;

    // AI 게이트웨이가 전화번호와 이메일이 포함된 요청을 400 content policy 위반으로 차단하므로, 해당 데이터를 마스킹해서 보낸다.
    private static final Pattern PHONE_PATTERN = Pattern.compile("0\\d{1,2}[-.\\s]?\\d{3,4}[-.\\s]?\\d{4}");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("[\\w.+-]+@[\\w.-]+\\.[A-Za-z]{2,}");

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String model;
    private final String formatModel; // 본문 정리는 분류보다 지시를 잘 지키는 모델이 필요해 별도로 지정

    public AiClassificationClient(
            @Value("${ai.base-url}") String baseUrl,
            @Value("${ai.api-key}") String apiKey,
            @Value("${ai.model.classification}") String model,
            @Value("${ai.model.format}") String formatModel,
            ObjectMapper objectMapper) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .build();
        this.objectMapper = objectMapper;
        this.model = model;
        this.formatModel = formatModel;
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
        String userContent = "제목: " + maskText(notice.getTitle()) + "\n본문: " + maskText(notice.getContentText());

        AiChatCompletionRequest request = AiChatCompletionRequest.builder()
                .model(model)
                .messages(List.of(new AiChatMessage("system", SYSTEM_PROMPT), new AiChatMessage("user", userContent)))
                .temperature(0)
                // .chatTemplateKwargs(Map.of("enable_thinking", false))
                .responseFormat(Map.of("type", "json_object"))
                .build();

        String rawContent = callWithRetry(request, notice.getId());
        log.info("[AI 분류] 원본 응답: {}", rawContent); // 임시 디버그용, 확인 후 제거
        return parseJson(rawContent);
    }

    /**
     * 공지 본문을 AI 서버에 보내 줄바꿈과 허용 기호만 더한 평문으로 정리받는다.
     *
     * <p>전화번호/이메일은 자리표시자로 보낸 뒤 응답에서 원래 값으로 되돌린다. 응답이 원문과 같은 글자인지는 호출부에서 검증한다.
     *
     * @return 정리된 본문, 응답이 비어 있으면 null
     * @throws RestClientException AI 서버 호출 자체가 실패한 경우
     */
    public String formatContent(Long noticeId, String content) {
        PlaceholderMaskedText masked = maskWithPlaceholders(content);

        AiChatCompletionRequest request = AiChatCompletionRequest.builder()
                .model(formatModel)
                .messages(List.of(new AiChatMessage("system", FORMAT_PROMPT), new AiChatMessage("user", masked.text())))
                .temperature(0)
                .build();

        String rawContent = callWithRetry(request, noticeId);
        return rawContent == null ? null : masked.restore(rawContent);
    }

    static String maskText(String text) {
        if (text == null) {
            return null;
        }
        String masked = text;
        masked = PHONE_PATTERN.matcher(masked).replaceAll("[전화번호]");
        masked = EMAIL_PATTERN.matcher(masked).replaceAll("[이메일]");
        return masked;
    }

    /**
     * 전화번호/이메일을 {@code ⟦1⟧}, {@code ⟦2⟧}처럼 번호를 붙인 자리표시자로 바꾼다.
     *
     * <p>{@link #maskText(String)}와 달리 {@link PlaceholderMaskedText#restore(String)}로 원래 값을 되돌릴 수 있다.
     */
    static PlaceholderMaskedText maskWithPlaceholders(String text) {
        List<String> originals = new ArrayList<>();
        String masked = text;
        for (Pattern pattern : List.of(PHONE_PATTERN, EMAIL_PATTERN)) {
            masked = pattern.matcher(masked).replaceAll(match -> {
                originals.add(match.group());
                return "⟦" + originals.size() + "⟧";
            });
        }
        return new PlaceholderMaskedText(masked, originals);
    }

    record PlaceholderMaskedText(String text, List<String> originals) {

        private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("⟦(\\d+)⟧");

        /**
         * 자리표시자를 원래 값으로 되돌린다. 번호가 범위를 벗어나면 그대로 둔다.
         */
        String restore(String maskedText) {
            return PLACEHOLDER_PATTERN.matcher(maskedText).replaceAll(match -> {
                int index = Integer.parseInt(match.group(1)) - 1;
                String value = index < originals.size() ? originals.get(index) : match.group();
                return Matcher.quoteReplacement(value);
            });
        }
    }

    private String callWithRetry(AiChatCompletionRequest request, Long noticeId) {
        RestClientException lastError = null;

        for (int attempt = 1; attempt <= MAX_RETRY; attempt++) {
            try {
                AiChatCompletionResponse response = restClient
                        .post()
                        .uri("/chat/completions/")
                        .body(request)
                        .retrieve()
                        .body(AiChatCompletionResponse.class);

                return response == null ? null : response.getFirstMessageContent();
            } catch (RestClientException e) {
                lastError = e;
                log.warn("[AI 분류] 호출 실패 (notice id={}, 시도 {}/{})", noticeId, attempt, MAX_RETRY, e);
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
