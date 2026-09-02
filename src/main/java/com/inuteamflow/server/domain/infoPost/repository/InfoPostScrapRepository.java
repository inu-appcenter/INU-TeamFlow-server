package com.inuteamflow.server.domain.infoPost.repository;

import com.inuteamflow.server.domain.infoPost.entity.InfoPost;
import com.inuteamflow.server.domain.infoPost.entity.InfoPostScrap;
import com.inuteamflow.server.domain.user.entity.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InfoPostScrapRepository extends JpaRepository<InfoPostScrap, Long> {

    boolean existsByInfoPostAndUser(InfoPost infoPost, User user);

    // 스크랩한 정보글 목록 (스크랩한 시각 최신순)
    @Query("SELECT ips.infoPost FROM InfoPostScrap ips WHERE ips.user = :user ORDER BY ips.createdAt DESC")
    Slice<InfoPost> findInfoPostsByUser(@Param("user") User user, Pageable pageable);

    // 스크랩 취소 (없으면 0건 삭제)
    @Modifying
    @Query("DELETE FROM InfoPostScrap ips WHERE ips.infoPost = :infoPost AND ips.user = :user")
    int deleteByInfoPostAndUser(@Param("infoPost") InfoPost infoPost, @Param("user") User user);

    // 정보글 삭제 시 그 정보글의 스크랩 전체 삭제
    @Modifying
    @Query("DELETE FROM InfoPostScrap ips WHERE ips.infoPost = :infoPost")
    void deleteByInfoPost(@Param("infoPost") InfoPost infoPost);

    // 회원 탈퇴 시 본인이 스크랩한 기록 삭제
    @Modifying
    @Query("DELETE FROM InfoPostScrap ips WHERE ips.user = :user")
    void deleteByUser(@Param("user") User user);

    // 회원 탈퇴 시 본인이 작성한 정보글을 다른 사람이 스크랩한 기록 삭제
    @Modifying
    @Query("DELETE FROM InfoPostScrap ips WHERE ips.infoPost.createdBy = :userId")
    void deleteByInfoPostCreatedBy(@Param("userId") Long userId);
}
