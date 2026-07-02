package com.inuteamflow.server.domain.infoPost.repository;

import com.inuteamflow.server.domain.infoPost.entity.InfoPost;
import com.inuteamflow.server.global.enums.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface InfoPostRepository extends JpaRepository<InfoPost, Long> {

    @Query(value = "SELECT p FROM InfoPost p " +
            "WHERE (:category IS NULL OR p.category = :category) " +
            "AND (:keyword IS NULL OR p.title LIKE %:keyword%) " +
            "AND (:linkable IS NULL OR p.linkable = :linkable)",
            countQuery = "SELECT COUNT(p) FROM InfoPost p " +
                    "WHERE (:category IS NULL OR p.category = :category) " +
                    "AND (:keyword IS NULL OR p.title LIKE %:keyword%) " +
                    "AND (:linkable IS NULL OR p.linkable = :linkable)")
    Page<InfoPost> search(@Param("category") Category category,
                          @Param("keyword") String keyword,
                          @Param("linkable") Boolean linkable,
                          Pageable pageable);

    Page<InfoPost> findAllByCreatedBy(Long userId, Pageable pageable);
}
