package com.inuteamflow.server.domain.recruitment.repository;

import com.inuteamflow.server.domain.infoPost.entity.InfoPost;
import com.inuteamflow.server.domain.recruitment.entity.Recruitment;
import com.inuteamflow.server.domain.team.entity.Team;
import com.inuteamflow.server.domain.user.entity.User;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface RecruitmentRepository extends JpaRepository<Recruitment, Long> {

    Page<Recruitment> findAllByRecruiter(User user, Pageable pageable);

    List<Recruitment> findAllByTeam(Team team);

    Page<Recruitment> findAllByInfoPost(InfoPost infoPost, Pageable pageable);

    long countByInfoPost(InfoPost infoPost);

    @Modifying
    @Query("DELETE FROM Recruitment r WHERE r.recruiter = :recruiter")
    void deleteByRecruiter(@Param("recruiter") User recruiter);

    @Modifying
    @Query("UPDATE Recruitment r SET r.infoPost = null WHERE r.infoPost.createdBy = :userId")
    void clearInfoPostByCreatedBy(@Param("userId") Long userId);
}
