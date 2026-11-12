package com.inuteamflow.server.domain.push.service;

import java.util.ArrayList;
import java.util.List;

final class TokenBatches {

    private TokenBatches() {}

    /**
     * 토큰 목록을 발송 채널의 요청당 최대 크기로 분할한다.
     *
     * <p>마지막 배치는 남은 토큰 수만 포함하며 원본 목록의 순서를 유지한다.</p>
     *
     * @param tokens 분할할 토큰 목록
     * @param batchSize 배치당 최대 토큰 수
     * @return 최대 batchSize개 단위로 분할된 토큰 목록
     */
    static List<List<String>> partition(List<String> tokens, int batchSize) {
        List<List<String>> batches = new ArrayList<>();
        for (int i = 0; i < tokens.size(); i += batchSize) {
            batches.add(tokens.subList(i, Math.min(i + batchSize, tokens.size())));
        }
        return batches;
    }
}
