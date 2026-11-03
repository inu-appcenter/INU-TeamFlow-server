package com.inuteamflow.server.global.enums;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * {@link ClientType#from(String)}의 X-Client-Type 헤더 값 변환을 검증한다.
 */
class ClientTypeTest {

    @Test
    @DisplayName("헤더 값이 mobile이면 대소문자와 관계없이 MOBILE이다")
    void from_mobile_returnsMobile() {
        assertThat(ClientType.from("mobile")).isEqualTo(ClientType.MOBILE);
        assertThat(ClientType.from("MOBILE")).isEqualTo(ClientType.MOBILE);
    }

    @Test
    @DisplayName("헤더가 없거나 mobile이 아닌 값이면 WEB이다")
    void from_missingOrOtherValue_returnsWeb() {
        assertThat(ClientType.from(null)).isEqualTo(ClientType.WEB);
        assertThat(ClientType.from("web")).isEqualTo(ClientType.WEB);
        assertThat(ClientType.from("moblie")).isEqualTo(ClientType.WEB);
    }
}
