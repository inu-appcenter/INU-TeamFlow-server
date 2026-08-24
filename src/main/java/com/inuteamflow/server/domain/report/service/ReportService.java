package com.inuteamflow.server.domain.report.service;

import com.inuteamflow.server.domain.infoPost.entity.InfoPost;
import com.inuteamflow.server.domain.infoPost.repository.InfoPostRepository;
import com.inuteamflow.server.domain.recruitment.entity.Recruitment;
import com.inuteamflow.server.domain.recruitment.repository.RecruitmentRepository;
import com.inuteamflow.server.domain.report.dto.request.ReportRequest;
import com.inuteamflow.server.domain.report.dto.response.ReportResponse;
import com.inuteamflow.server.domain.report.entity.Report;
import com.inuteamflow.server.domain.report.enums.ReportTargetType;
import com.inuteamflow.server.domain.report.repository.ReportRepository;
import com.inuteamflow.server.domain.user.entity.User;
import com.inuteamflow.server.domain.user.repository.UserRepository;
import com.inuteamflow.server.global.exception.error.CustomErrorCode;
import com.inuteamflow.server.global.exception.error.RestApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService {

    private final ReportRepository reportRepository;
    private final RecruitmentRepository recruitmentRepository;
    private final InfoPostRepository infoPostRepository;
    private final UserRepository userRepository;

    /**
     * 모집글을 신고한다.
     *
     * <p>신고 대상 게시글의 제목과 작성자(모집자) 정보를 신고 시점 기준으로 스냅샷 저장한다.
     * 이후 게시글이나 작성자가 삭제/탈퇴되어도 신고 내역의 표시 정보는 유지된다.</p>
     *
     * @param recruitmentId 신고할 모집글 ID
     * @param request 신고 사유 및 상세 내용
     * @param reporter 신고를 요청한 사용자
     * @return 생성된 신고 응답
     * @throws RestApiException 모집글을 찾을 수 없는 경우
     */
    @Transactional
    public ReportResponse reportRecruitment(Long recruitmentId, ReportRequest request, User reporter) {
        Recruitment recruitment = recruitmentRepository
                .findById(recruitmentId)
                .orElseThrow(() -> new RestApiException(CustomErrorCode.RECRUITMENT_NOT_FOUND));

        Report report = Report.create(
                reporter.getUserId(),
                reporter.getName(),
                ReportTargetType.RECRUITMENT,
                recruitment.getRecruitmentId(),
                recruitment.getTitle(),
                recruitment.getRecruiter().getUserId(),
                recruitment.getRecruiter().getName(),
                request.getReason(),
                request.getDetail());

        return ReportResponse.from(reportRepository.save(report));
    }

    /**
     * 정보글을 신고한다.
     *
     * <p>정보글은 작성자를 별도 연관관계로 갖지 않으므로(BaseEntity의 createdBy만 존재),
     * 작성자 이름은 사용자 조회를 통해 스냅샷한다. 작성자가 이미 탈퇴한 경우 이름은 null로 저장된다.</p>
     *
     * @param infoPostId 신고할 정보글 ID
     * @param request 신고 사유 및 상세 내용
     * @param reporter 신고를 요청한 사용자
     * @return 생성된 신고 응답
     * @throws RestApiException 정보글을 찾을 수 없는 경우
     */
    @Transactional
    public ReportResponse reportInfoPost(Long infoPostId, ReportRequest request, User reporter) {
        InfoPost infoPost = infoPostRepository
                .findById(infoPostId)
                .orElseThrow(() -> new RestApiException(CustomErrorCode.INFO_POST_NOT_FOUND));

        String authorName =
                userRepository.findById(infoPost.getCreatedBy()).map(User::getName).orElse(null);

        Report report = Report.create(
                reporter.getUserId(),
                reporter.getName(),
                ReportTargetType.INFO_POST,
                infoPost.getInfoPostId(),
                infoPost.getTitle(),
                infoPost.getCreatedBy(),
                authorName,
                request.getReason(),
                request.getDetail());

        return ReportResponse.from(reportRepository.save(report));
    }

    /**
     * 사용자를 직접 신고한다.
     *
     * <p>게시글 신고와 달리 대상 게시글 정보는 없으며, 대상 사용자 정보만 스냅샷 저장한다.</p>
     *
     * @param userId 신고할 대상 사용자 ID
     * @param request 신고 사유 및 상세 내용
     * @param reporter 신고를 요청한 사용자
     * @return 생성된 신고 응답
     * @throws RestApiException 대상 사용자를 찾을 수 없는 경우
     */
    @Transactional
    public ReportResponse reportUser(Long userId, ReportRequest request, User reporter) {
        User target = userRepository
                .findById(userId)
                .orElseThrow(() -> new RestApiException(CustomErrorCode.USER_NOT_FOUND));

        Report report = Report.create(
                reporter.getUserId(),
                reporter.getName(),
                ReportTargetType.USER,
                null,
                null,
                target.getUserId(),
                target.getName(),
                request.getReason(),
                request.getDetail());

        return ReportResponse.from(reportRepository.save(report));
    }

}
