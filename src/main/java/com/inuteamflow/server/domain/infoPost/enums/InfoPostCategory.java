package com.inuteamflow.server.domain.infoPost.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum InfoPostCategory {

    CONTEST(InfoPostType.NOTICE),            // 공모전
    CLUB(InfoPostType.NOTICE),               // 동아리
    EXTERNAL_ACTIVITY(InfoPostType.NOTICE),  // 외부활동
    INTERN(InfoPostType.NOTICE),             // 인턴
    CAREER_ADVICE(InfoPostType.FREE),        // 취업 조언
    CASUAL_TALK(InfoPostType.FREE),          // 잡담
    INFO_SHARING(InfoPostType.FREE);         // 정보 공유

    private final InfoPostType type;
}
