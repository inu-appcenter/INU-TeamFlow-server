package com.inuteamflow.server.global.config;

import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class ExpoConfig {

    private static final String EXPO_PUSH_BASE_URL = "https://exp.host/--/api/v2/push";
    private static final Duration CONNECTION_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(10);

    /**
     * Expo Push API 호출에 사용할 RestClient를 생성한다.
     *
     * <p>Expo Push API 기본 주소를 설정하고, Firebase Admin SDK와 같은 연결/읽기 타임아웃을 적용한다.</p>
     *
     * @return Expo Push API 호출용 RestClient
     */
    @Bean
    public RestClient expoRestClient() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(CONNECTION_TIMEOUT);
        requestFactory.setReadTimeout(READ_TIMEOUT);
        return RestClient.builder()
                .baseUrl(EXPO_PUSH_BASE_URL)
                .requestFactory(requestFactory)
                .build();
    }
}
