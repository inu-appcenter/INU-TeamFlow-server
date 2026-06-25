package com.inuteamflow.server.domain.recruitment.repository;

import com.inuteamflow.server.domain.recruitment.entity.Recruitment;
import com.inuteamflow.server.domain.team.entity.Team;
import com.inuteamflow.server.domain.user.entity.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Optional;

@Repository
public interface RecruitmentRepository extends JpaRepository<Recruitment, Long> {

    Page<Recruitment> findAllByRecruiter(User user, Pageable pageable);

    List<Recruitment> findAllByTeam(Team team);
}
