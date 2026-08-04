package com.inuteamflow.server.domain.recruitment.repository;

import com.inuteamflow.server.domain.recruitment.entity.Recruitment;
import com.inuteamflow.server.domain.recruitment.entity.RecruitmentScrap;
import com.inuteamflow.server.domain.user.entity.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RecruitmentScrapRepository extends JpaRepository<RecruitmentScrap, Long> {

    boolean existsByRecruitmentAndUser(Recruitment recruitment, User user);

    // 스크랩한 모집글 목록 (스크랩한 시각 최신순)
    @Query("""
        SELECT r FROM RecruitmentScrap rs
        JOIN rs.recruitment r
        JOIN FETCH r.recruiter
        WHERE rs.user = :user
        ORDER BY rs.createdAt DESC
        """)
    Slice<Recruitment> findRecruitmentsByUser(@Param("user") User user, Pageable pageable);

    // 스크랩 취소 (없으면 0건 삭제)
    @Modifying
    @Query("DELETE FROM RecruitmentScrap rs WHERE rs.recruitment = :recruitment AND rs.user = :user")
    long deleteByRecruitmentAndUser(@Param("recruitment") Recruitment recruitment, @Param("user") User user);

    // 모집글 삭제 시 그 모집글의 스크랩 전체 삭제
    @Modifying
    @Query("DELETE FROM RecruitmentScrap rs WHERE rs.recruitment = :recruitment")
    void deleteByRecruitment(@Param("recruitment") Recruitment recruitment);

    // 회원 탈퇴 시 본인이 스크랩한 기록 삭제
    @Modifying
    @Query("DELETE FROM RecruitmentScrap rs WHERE rs.user = :user")
    void deleteByUser(@Param("user") User user);

    // 회원 탈퇴 시 본인이 작성한 모집글을 다른 사람이 스크랩한 기록 삭제
    @Modifying
    @Query("DELETE FROM RecruitmentScrap rs WHERE rs.recruitment.recruiter = :recruiter")
    void deleteByRecruitmentRecruiter(@Param("recruiter") User recruiter);
}
