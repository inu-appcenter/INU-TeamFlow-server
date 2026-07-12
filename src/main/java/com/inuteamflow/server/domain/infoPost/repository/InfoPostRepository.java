package com.inuteamflow.server.domain.infoPost.repository;

import com.inuteamflow.server.domain.infoPost.entity.InfoPost;
import com.inuteamflow.server.domain.infoPost.enums.InfoPostCategory;
import com.inuteamflow.server.global.enums.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InfoPostRepository extends JpaRepository<InfoPost, Long> {

    @Query(value = "SELECT p FROM InfoPost p " +
            "WHERE (:categories IS NULL OR p.category IN :categories) " +
            "AND (:keyword IS NULL OR p.title LIKE %:keyword%)",
            countQuery = "SELECT COUNT(p) FROM InfoPost p " +
                    "WHERE (:categories IS NULL OR p.category IN :categories) " +
                    "AND (:keyword IS NULL OR p.title LIKE %:keyword%)")
    Page<InfoPost> search(@Param("categories") List<InfoPostCategory> categories,
                          @Param("keyword") String keyword,
                          Pageable pageable);


    Page<InfoPost> findAllByCreatedBy(Long userId, Pageable pageable);

    @Modifying
    @Query("DELETE FROM InfoPost i WHERE i.createdBy = :createdBy")
    void deleteByCreatedBy(@Param("createdBy") Long createdBy);
}
