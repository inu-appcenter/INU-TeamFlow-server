package com.inuteamflow.server.domain.inquiry.service;

import com.inuteamflow.server.domain.inquiry.dto.request.InquiryRequest;
import com.inuteamflow.server.domain.inquiry.dto.response.InquiryDetailResponse;
import com.inuteamflow.server.domain.inquiry.dto.response.InquiryResponse;
import com.inuteamflow.server.domain.inquiry.entity.Inquiry;
import com.inuteamflow.server.domain.inquiry.repository.InquiryRepository;
import com.inuteamflow.server.domain.user.entity.User;
import com.inuteamflow.server.global.exception.error.CustomErrorCode;
import com.inuteamflow.server.global.exception.error.RestApiException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InquiryService {

    private final InquiryRepository inquiryRepository;

    /**
     * 문의를 등록한다.
     *
     * <p>문의자 이름을 등록 시점 기준으로 스냅샷 저장하여, 이후 문의자가 탈퇴해도
     * 문의 내역의 표시 정보는 유지된다.</p>
     */
    @Transactional
    public InquiryResponse createInquiry(InquiryRequest request, User inquirer) {
        Inquiry inquiry = Inquiry.create(
                inquirer.getUserId(), inquirer.getName(), request.getType(), request.getDetail());

        return InquiryResponse.from(inquiryRepository.save(inquiry));
    }

    /**
     * 내가 등록한 문의 목록을 조회한다. (최신순, 페이지네이션 없음)
     */
    public List<InquiryResponse> getMyInquiries(User user) {
        return inquiryRepository.findAllByInquirerIdOrderByCreatedAtDesc(user.getUserId()).stream()
                .map(InquiryResponse::from)
                .toList();
    }

    /**
     * 내 문의 상세를 조회한다. 본인 문의가 아니면 403.
     */
    public InquiryDetailResponse getMyInquiry(Long inquiryId, User user) {
        Inquiry inquiry = inquiryRepository
                .findById(inquiryId)
                .orElseThrow(() -> new RestApiException(CustomErrorCode.INQUIRY_NOT_FOUND));

        if (!inquiry.isOwnedBy(user.getUserId())) {
            throw new RestApiException(CustomErrorCode.INQUIRY_FORBIDDEN);
        }

        return InquiryDetailResponse.from(inquiry);
    }

    /**
     * 내 문의를 취소(삭제)한다. 본인 문의가 아니면 403.
     */
    @Transactional
    public void deleteMyInquiry(Long inquiryId, User user) {
        Inquiry inquiry = inquiryRepository
                .findById(inquiryId)
                .orElseThrow(() -> new RestApiException(CustomErrorCode.INQUIRY_NOT_FOUND));

        if (!inquiry.isOwnedBy(user.getUserId())) {
            throw new RestApiException(CustomErrorCode.INQUIRY_FORBIDDEN);
        }

        inquiryRepository.delete(inquiry);
    }
}