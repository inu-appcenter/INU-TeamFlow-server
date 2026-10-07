package com.inuteamflow.server.domain.push.dto;

import java.util.Map;

/**
 * 발송 채널(FCM, Expo)과 무관하게 기기에 전달할 푸시 알림 내용.
 *
 * @param title 알림 제목
 * @param body 알림 본문
 * @param data 알림 선택 시 클라이언트가 사용하는 데이터 (redirectUrl, type 등)
 * @param collapseKey 같은 키의 알림을 하나로 묶기 위한 축약 키, 묶지 않으면 null
 */
public record PushMessage(String title, String body, Map<String, String> data, String collapseKey) {}
