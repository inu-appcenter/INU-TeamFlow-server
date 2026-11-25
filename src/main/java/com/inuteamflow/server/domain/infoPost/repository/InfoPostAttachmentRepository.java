package com.inuteamflow.server.domain.infoPost.repository;

import com.inuteamflow.server.domain.infoPost.entity.InfoPost;
import com.inuteamflow.server.domain.infoPost.entity.InfoPostAttachment;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface InfoPostAttachmentRepository extends JpaRepository<InfoPostAttachment, Long> {
    List<InfoPostAttachment> findByInfoPostOrderBySortOrderAsc(InfoPost infoPost);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM InfoPostAttachment a WHERE a.infoPost = :infoPost")
    void deleteByInfoPost(@Param("infoPost") InfoPost infoPost);
}
