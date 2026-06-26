package com.inuteamflow.server.domain.teamNotice.repository;

import com.inuteamflow.server.domain.teamNotice.entity.TeamNotice;
import com.inuteamflow.server.domain.teamNotice.entity.TeamNoticeImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TeamNoticeImageRepository extends JpaRepository<TeamNoticeImage, Long> {

	List<TeamNoticeImage> findByTeamNoticeOrderBySortOrderAsc(TeamNotice teamNotice);

	List<TeamNoticeImage> findAllByTeamNoticeIn(List<TeamNotice> notices);

	@Modifying
	@Query("DELETE FROM TeamNoticeImage i WHERE i.teamNotice = :teamNotice")
	void deleteByTeamNotice(@Param("teamNotice") TeamNotice teamNotice);

	@Modifying
	@Query("DELETE FROM TeamNoticeImage i WHERE i.teamNotice IN :notices")
	void deleteAllByTeamNoticeIn(@Param("notices") List<TeamNotice> notices);
}