package com.inuteamflow.server.domain.inquiry.repository;

import com.inuteamflow.server.domain.inquiry.entity.Inquiry;
import com.inuteamflow.server.domain.inquiry.enums.InquiryStatus;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InquiryRepository extends JpaRepository<Inquiry, Long> {

    List<Inquiry> findAllByInquirerIdOrderByCreatedAtDesc(Long inquirerId);

    long countByStatus(InquiryStatus status);

    @Query(
            value = "SELECT i FROM Inquiry i "
                    + "WHERE (:keyword IS NULL OR i.detail LIKE %:keyword% OR i.inquirerName LIKE %:keyword%)",
            countQuery = "SELECT COUNT(i) FROM Inquiry i "
                    + "WHERE (:keyword IS NULL OR i.detail LIKE %:keyword% OR i.inquirerName LIKE %:keyword%)")
    Page<Inquiry> search(@Param("keyword") String keyword, Pageable pageable);
}
