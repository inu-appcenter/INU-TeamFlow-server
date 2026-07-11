package com.inuteamflow.server.domain.teamNotice.repository;

import com.inuteamflow.server.domain.teamNotice.entity.TeamNotice;
import com.inuteamflow.server.domain.teamNotice.entity.TeamNoticeRead;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Set;

public interface TeamNoticeReadRepository extends JpaRepository<TeamNoticeRead, Long> {

	boolean existsByTeamNoticeAndCreatedBy(TeamNotice teamNotice, Long createdBy);

	@Query("SELECT r.teamNotice.teamNoticeId FROM TeamNoticeRead r WHERE r.teamNotice IN :notices AND r.createdBy = :userId")
	Set<Long> findReadNoticeIds(@Param("notices") List<TeamNotice> notices, @Param("userId") Long userId);

	@Modifying
	@Query("DELETE FROM TeamNoticeRead r WHERE r.teamNotice IN :notices")
	void deleteAllByTeamNoticeIn(@Param("notices") List<TeamNotice> notices);

	@Modifying
	@Query("DELETE FROM TeamNoticeRead r WHERE r.teamNotice = :notice")
    void deleteByTeamNotice(@Param("notice") TeamNotice notice);
}