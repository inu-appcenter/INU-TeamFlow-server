package com.inuteamflow.server.domain.infoPost.repository;

import com.inuteamflow.server.domain.infoPost.entity.InfoPost;
import com.inuteamflow.server.domain.infoPost.entity.InfoPostImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InfoPostImageRepository extends JpaRepository<InfoPostImage, Long> {
    List<InfoPostImage> findByInfoPostOrderBySortOrderAsc(InfoPost infoPost);

    // 대표 이미지(sortOrder=0) 목록 조회용 — 요약 응답의 썸네일 N+1 방지
    List<InfoPostImage> findAllByInfoPostInAndSortOrder(List<InfoPost> infoPosts, Integer sortOrder);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM InfoPostImage i WHERE i.infoPost = :infoPost")
    void deleteByInfoPost(@Param("infoPost") InfoPost infoPost);

    @Query("SELECT i.imageKey FROM InfoPostImage i WHERE i.infoPost.createdBy = :userId")
    List<String> findImageKeysByInfoPostCreatedBy(@Param("userId") Long userId);

    @Modifying
    @Query("DELETE FROM InfoPostImage i WHERE i.infoPost.createdBy = :userId")
    void deleteByInfoPostCreatedBy(@Param("userId") Long userId);
}
