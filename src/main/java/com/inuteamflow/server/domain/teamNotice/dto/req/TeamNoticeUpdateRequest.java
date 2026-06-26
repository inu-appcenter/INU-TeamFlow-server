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
@Schema(description = "팀 공지 수정 요청 DTO")
public class TeamNoticeUpdateRequest {

	@NotBlank
	@Schema(description = "공지 제목", example = "5월 정기 회의 안내 (수정)")
	private String title;

	@Schema(description = "공지 내용", example = "일정이 변경되어 5월 31일 오전 11시로 조정되었습니다.")
	private String content;

	@NotNull
	@Schema(description = "상단 고정 여부", example = "true")
	private Boolean isPinned;

	@Schema(description = "유지할 이미지의 S3 키 목록 (기존 이미지를 교체하려면 새 키로 전달)", example = "[\"teams/notices/xyz789.jpg\"]")
	private List<String> imageKeys;
}