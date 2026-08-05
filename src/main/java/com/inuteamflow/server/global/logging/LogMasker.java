package com.inuteamflow.server.global.logging;

public final class LogMasker {

    private LogMasker() {}

    /** 학번/아이디 등 식별자: 앞 2자리만 남기고 마스킹 (예: 202012345 -> 20*******) */
    public static String maskId(String value) {
        if (value == null || value.isBlank()) {
            return "***";
        }
        if (value.length() <= 2) {
            return "*".repeat(value.length());
        }
        return value.substring(0, 2) + "*".repeat(value.length() - 2);
    }
}
