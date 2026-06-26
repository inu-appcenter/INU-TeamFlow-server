package com.inuteamflow.server.domain.teamNotice.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Schema(description = "팀 공지 생성 요청 DTO")
public class TeamNoticeCreateRequest {

	@NotBlank
	@Schema(description = "공지 제목", example = "5월 정기 회의 안내")
	private String title;

	@Schema(description = "공지 내용", example = "이번 달 정기 회의는 5월 30일 오전 10시에 진행됩니다.")
	private String content;

	@NotNull
	@Schema(description = "상단 고정 여부", example = "false")
	private Boolean isPinned;

	@Schema(description = "업로드할 이미지의 S3 키 목록", example = "[\"teams/notices/abc123.jpg\"]")
	private List<String> imageKeys;
}