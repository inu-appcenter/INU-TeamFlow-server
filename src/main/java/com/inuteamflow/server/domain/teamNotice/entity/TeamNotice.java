package com.inuteamflow.server.domain.teamNotice.entity;

import com.inuteamflow.server.domain.team.entity.Team;
import com.inuteamflow.server.domain.teamNotice.dto.req.TeamNoticeCreateRequest;
import com.inuteamflow.server.domain.teamNotice.dto.req.TeamNoticeUpdateRequest;
import com.inuteamflow.server.global.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "team_notice")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TeamNotice extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "team_notice_id")
	private Long teamNoticeId;

	@Column(name = "title", nullable = false)
	private String title;

	@Column(name = "content")
	private String content;

	@Column(name = "is_pinned")
	private Boolean isPinned;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "team_id", nullable = false)
	private Team team;

	@Builder
	private TeamNotice(
			String title,
			String content,
			Boolean isPinned,
			Team team
	) {
		this.title = title;
		this.content = content;
		this.isPinned = isPinned;
		this.team = team;
	}

	public static TeamNotice create(Team team, TeamNoticeCreateRequest request) {
		return TeamNotice.builder()
				.team(team)
				.title(request.getTitle())
				.content(request.getContent())
				.isPinned(request.getIsPinned())
				.build();
	}

	public void update(TeamNoticeUpdateRequest request) {
		this.title = request.getTitle();
		this.content = request.getContent();
		this.isPinned = request.getIsPinned();
	}
}