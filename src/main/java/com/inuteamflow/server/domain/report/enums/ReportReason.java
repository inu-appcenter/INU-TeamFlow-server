package com.inuteamflow.server.domain.report.enums;

public enum ReportReason {
    SPAM, // 스팸/광고/도배
    ABUSE, // 욕설/비방/혐오 표현
    INAPPROPRIATE, // 부적절한 콘텐츠
    FRAUD, // 사기/허위 정보
    PRIVACY, // 개인정보 노출
    IMPERSONATION, // 사칭 (주로 사용자 신고)
    ETC // 기타
}
