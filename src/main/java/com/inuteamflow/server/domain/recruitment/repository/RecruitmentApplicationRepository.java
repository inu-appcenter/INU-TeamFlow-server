package com.inuteamflow.server.domain.recruitment.repository;

import com.inuteamflow.server.domain.recruitment.entity.Recruitment;
import com.inuteamflow.server.domain.recruitment.entity.RecruitmentApplication;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface RecruitmentApplicationRepository extends JpaRepository<RecruitmentApplication, Long> {

    // CANCELLED 제외
    @Query("SELECT COUNT(ra) > 0 FROM RecruitmentApplication ra " + "WHERE ra.recruitment = :recruitment "
            + "AND ra.createdBy = :userId "
            + "AND ra.applicationStatus != 'CANCELLED'")
    boolean existsByRecruitmentAndCreatedBy(Recruitment recruitment, Long userId);

    Page<RecruitmentApplication> findAllByRecruitment(Recruitment recruitment, Pageable pageable);

    Page<RecruitmentApplication> findAllByCreatedBy(Long createdBy, Pageable pageable);
    ;

    void deleteAllByRecruitmentIn(List<Recruitment> recruitments);

    void deleteByCreatedBy(Long createdBy);

    @Modifying
    @Query("DELETE FROM RecruitmentApplication ra WHERE ra.recruitment.createdBy = :userId")
    void deleteByRecruitmentCreatedBy(@Param("userId") Long userId);
}
