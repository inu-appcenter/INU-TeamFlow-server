package com.inuteamflow.server.domain.recruitment.repository;

import com.inuteamflow.server.domain.recruitment.dto.response.MyApplicationSummaryResponse;
import com.inuteamflow.server.domain.recruitment.entity.Recruitment;
import com.inuteamflow.server.domain.recruitment.entity.RecruitmentApplication;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.mapping.PropertyPath;
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

    default Page<MyApplicationSummaryResponse> findSummariesByCreatedBy(Long createdBy, Pageable pageable) {
        Sort sort = pageable.getSort();
        Sort querySort = Sort.by(sort.stream()
                .map(RecruitmentApplicationRepository::toSummarySortOrder)
                .toList());
        if (querySort.equals(sort)) {
            return findSummaryPageByCreatedBy(createdBy, pageable);
        }

        Pageable queryPageable = pageable.isPaged()
                ? PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), querySort)
                : Pageable.unpaged(querySort);
        var page = findSummaryPageByCreatedBy(createdBy, queryPageable);
        return new PageImpl<>(page.getContent(), pageable, page.getTotalElements());
    }

    private static Sort.Order toSummarySortOrder(Sort.Order order) {
        PropertyPath path = PropertyPath.from(order.getProperty(), RecruitmentApplication.class);
        String property = path.toDotPath();
        if (order.isIgnoreCase() && path.getLeafProperty().getType() != String.class) {
            return new Sort.Order(order.getDirection(), property, order.getNullHandling());
        }
        return order.withProperty(property);
    }

    // 연결된 정보글이 없어도 정보글 기준 정렬에서 지원서를 유지한다.
    @Query(value = """
                    SELECT new com.inuteamflow.server.domain.recruitment.dto.response.MyApplicationSummaryResponse(
                        a.recruitmentApplicationId, a.applicationStatus, r.category, u.name, a.createdAt, a.respondedAt)
                    FROM RecruitmentApplication a
                    JOIN a.recruitment r
                    JOIN r.recruiter u
                    LEFT JOIN r.infoPost ip
                    WHERE a.createdBy = :createdBy
                    """, countQuery = "SELECT COUNT(a) FROM RecruitmentApplication a WHERE a.createdBy = :createdBy")
    Page<MyApplicationSummaryResponse> findSummaryPageByCreatedBy(
            @Param("createdBy") Long createdBy, Pageable pageable);

    void deleteAllByRecruitmentIn(List<Recruitment> recruitments);

    void deleteByCreatedBy(Long createdBy);

    @Modifying
    @Query("DELETE FROM RecruitmentApplication ra WHERE ra.recruitment.createdBy = :userId")
    void deleteByRecruitmentCreatedBy(@Param("userId") Long userId);
}
