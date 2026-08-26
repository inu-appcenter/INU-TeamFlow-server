package com.inuteamflow.server.domain.inquiry.repository;

import com.inuteamflow.server.domain.inquiry.entity.Inquiry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InquiryRepository extends JpaRepository<Inquiry, Long> {

    List<Inquiry> findAllByInquirerIdOrderByCreatedAtDesc(Long inquirerId);

}
