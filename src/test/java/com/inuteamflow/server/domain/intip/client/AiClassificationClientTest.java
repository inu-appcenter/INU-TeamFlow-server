package com.inuteamflow.server.domain.intip.client;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * {@link AiClassificationClient#maskText(String)}가 AI 게이트웨이의 content policy 차단을 유발하는
 * 전화번호/이메일을 마스킹하는지 검증하는 순수 단위 테스트다.
 * - Spring Context, DB, 네트워크(AI 게이트웨이)를 사용하지 않는다.
 */
class AiClassificationClientTest {

    @Test
    @DisplayName("null 입력은 그대로 null을 반환한다")
    void masksNull() {
        assertThat(AiClassificationClient.maskText(null)).isNull();
    }

    @Test
    @DisplayName("구분자 유무와 무관하게 국내 전화번호를 마스킹한다")
    void masksPhoneNumbers() {
        assertThat(AiClassificationClient.maskText("문의 032-835-9576")).isEqualTo("문의 [전화번호]");
        assertThat(AiClassificationClient.maskText("010-1234-5678")).isEqualTo("[전화번호]");
        assertThat(AiClassificationClient.maskText("02.123.4567")).isEqualTo("[전화번호]");
        assertThat(AiClassificationClient.maskText("0212345678")).isEqualTo("[전화번호]");
    }

    @Test
    @DisplayName("이메일 주소를 마스킹한다")
    void masksEmails() {
        assertThat(AiClassificationClient.maskText("info@iccekorea.com")).isEqualTo("[이메일]");
        assertThat(AiClassificationClient.maskText("담당자 donghyeon@inu.ac.kr 참고"))
                .isEqualTo("담당자 [이메일] 참고");
    }

    @Test
    @DisplayName("차단됐던 실제 공지 본문을 마스킹하면 전화번호/이메일이 남지 않는다")
    void masksRealBlockedNotice() {
        String content = "4. 지원방법: 이메일주소(info@iccekorea.com)로 지원 의사 전달 또는 카카오톡(iccekorea)으로 상담신청 "
                + "5. 문의사항: 국제교류과 donghyeon@inu.ac.kr 또는 032-835-9576 "
                + "인턴 실무 관련: 이메일주소(info@iccekorea.com)";

        String masked = AiClassificationClient.maskText(content);

        assertThat(masked).doesNotContain("@").doesNotContain("032-835-9576");
        assertThat(masked).contains("[전화번호]").contains("[이메일]");
    }

    @Test
    @DisplayName("전화번호/이메일이 없는 본문은 원문 그대로 유지한다")
    void keepsCleanTextUnchanged() {
        String content = "1. 자기주도형 파이오니어 인턴 프로그램 안내 - 초기비용 약 650-750만원 내외 예상, 유급인턴임";

        assertThat(AiClassificationClient.maskText(content)).isEqualTo(content);
    }
}
