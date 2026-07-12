package com.inuteamflow.server.domain.vote.repository;

import com.inuteamflow.server.domain.team.entity.Team;
import com.inuteamflow.server.domain.vote.entity.Vote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VoteRepository extends JpaRepository<Vote, Long> {

    List<Vote> findByTeam(Team team);

    boolean existsByCreatedByAndIsOpenedTrue(Long createdBy);

}
