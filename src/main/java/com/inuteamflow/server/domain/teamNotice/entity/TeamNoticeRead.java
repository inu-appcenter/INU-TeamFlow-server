package com.inuteamflow.server.domain.teamNotice.entity;

import com.inuteamflow.server.global.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "team_notice_read",
		uniqueConstraints = @UniqueConstraint(columnNames = {"team_notice_id", "created_by"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TeamNoticeRead extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "team_notice_read_id")
	private Long teamNoticeReadId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "team_notice_id", nullable = false)
	private TeamNotice teamNotice;

	@Builder
	private TeamNoticeRead(TeamNotice teamNotice) {
		this.teamNotice = teamNotice;
	}

	public static TeamNoticeRead create(TeamNotice teamNotice) {
		return TeamNoticeRead.builder()
				.teamNotice(teamNotice)
				.build();
	}
}