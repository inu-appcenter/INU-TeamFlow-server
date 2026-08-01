package com.inuteamflow.server.domain.team.repository;

import com.inuteamflow.server.domain.team.entity.Team;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeamRepository extends JpaRepository<Team, Long> {}
