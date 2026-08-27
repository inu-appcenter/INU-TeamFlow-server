package com.inuteamflow.server.domain.inquiry.repository;

import com.inuteamflow.server.domain.inquiry.entity.Inquiry;
import com.inuteamflow.server.domain.inquiry.enums.InquiryStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InquiryRepository extends JpaRepository<Inquiry, Long> {

    List<Inquiry> findAllByInquirerIdOrderByCreatedAtDesc(Long inquirerId);

    long countByStatus(InquiryStatus status);
}
