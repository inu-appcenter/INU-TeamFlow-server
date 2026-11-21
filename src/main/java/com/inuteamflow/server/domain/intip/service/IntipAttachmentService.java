package com.inuteamflow.server.domain.intip.service;

import com.inuteamflow.server.domain.infoPost.entity.InfoPost;
import com.inuteamflow.server.domain.infoPost.entity.InfoPostAttachment;
import com.inuteamflow.server.domain.infoPost.entity.InfoPostImage;
import com.inuteamflow.server.domain.infoPost.repository.InfoPostAttachmentRepository;
import com.inuteamflow.server.domain.infoPost.repository.InfoPostImageRepository;
import com.inuteamflow.server.domain.intip.client.IntipClient;
import com.inuteamflow.server.domain.intip.dto.IntipNoticeResponse.Attachment;
import com.inuteamflow.server.global.s3.S3Service;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * INTIP 공지 첨부파일을 정보글에 붙인다.
 *
 * <p>외부 호출(학교 서버 다운로드, S3 업로드)은 {@link #prepare}에서 트랜잭션 밖에서, DB 저장은 {@link #save}에서
 * 정보글 저장 트랜잭션 안에서 처리한다.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IntipAttachmentService {

    // 학교 서버는 이미지도 application/x-msdownload로 내려주므로 확장자로 Content-Type을 정한다
    private static final Map<String, String> IMAGE_CONTENT_TYPES = Map.of(
            "jpg", "image/jpeg",
            "jpeg", "image/jpeg",
            "png", "image/png",
            "gif", "image/gif",
            "webp", "image/webp");

    private final IntipClient intipClient;
    private final S3Service s3Service;
    private final InfoPostImageRepository infoPostImageRepository;
    private final InfoPostAttachmentRepository infoPostAttachmentRepository;

    /**
     * 업로드를 마친 이미지 키와, 다운로드 URL만 저장할 나머지 첨부파일.
     *
     * @param imageKeys S3에 올린 이미지 키 (게시된 순서, 첫 이미지가 썸네일)
     * @param files 이미지 외 첨부파일 (게시된 순서)
     */
    public record PreparedAttachments(List<String> imageKeys, List<Attachment> files) {
        public static final PreparedAttachments EMPTY = new PreparedAttachments(List.of(), List.of());
    }

    /**
     * 이미지 첨부를 원본 그대로 S3에 올리고 나머지 첨부와 나눈다. 트랜잭션 밖에서 호출한다.
     *
     * <p>이미지 업로드에 실패하면 내려받을 수는 있도록 나머지 첨부로 돌린다. 이름이나 URL이 없는 첨부는 건너뛴다.</p>
     *
     * @param attachments INTIP 공지의 첨부파일 목록 (게시된 순서)
     * @return 업로드한 이미지 키와 나머지 첨부
     */
    public PreparedAttachments prepare(List<Attachment> attachments) {
        if (attachments == null || attachments.isEmpty()) {
            return PreparedAttachments.EMPTY;
        }

        List<String> imageKeys = new ArrayList<>();
        List<Attachment> files = new ArrayList<>();
        for (Attachment attachment : attachments) {
            if (!StringUtils.hasText(attachment.getName()) || !StringUtils.hasText(attachment.getUrl())) {
                continue; // 보여줄 수도 받을 수도 없음
            }

            String fileType = normalizeFileType(attachment.getFileType());
            String contentType = fileType == null ? null : IMAGE_CONTENT_TYPES.get(fileType);
            String imageKey = contentType == null ? null : uploadImage(attachment, contentType);
            if (imageKey != null) {
                imageKeys.add(imageKey);
            } else {
                files.add(attachment);
            }
        }
        return new PreparedAttachments(imageKeys, files);
    }

    /**
     * 준비한 이미지와 첨부파일을 정보글에 연결해 저장한다. 정보글 저장 트랜잭션 안에서 호출한다.
     *
     * @param infoPost 첨부파일을 연결할 정보글
     * @param prepared {@link #prepare}의 결과
     */
    public void save(InfoPost infoPost, PreparedAttachments prepared) {
        List<InfoPostImage> images = new ArrayList<>();
        for (String imageKey : prepared.imageKeys()) {
            images.add(InfoPostImage.create(imageKey, images.size(), infoPost)); // 첫 이미지 = 썸네일
        }

        List<InfoPostAttachment> files = new ArrayList<>();
        for (Attachment file : prepared.files()) {
            files.add(InfoPostAttachment.create(
                    file.getName(), file.getUrl(), normalizeFileType(file.getFileType()), files.size(), infoPost));
        }

        infoPostImageRepository.saveAll(images);
        infoPostAttachmentRepository.saveAll(files);
    }

    private String uploadImage(Attachment attachment, String contentType) {
        try {
            byte[] content = intipClient.downloadFile(attachment.getUrl());
            if (content == null || content.length == 0) {
                log.warn("[INTIP 동기화] 첨부 이미지가 비어 있어 첨부파일로 저장, url={}", attachment.getUrl());
                return null;
            }
            return s3Service.uploadInfoPostImage(attachment.getName(), contentType, content);
        } catch (Exception e) {
            log.warn("[INTIP 동기화] 첨부 이미지 업로드 실패, 첨부파일로 저장, url={}", attachment.getUrl(), e);
            return null;
        }
    }

    private String normalizeFileType(String fileType) {
        return fileType == null ? null : fileType.toLowerCase(Locale.ROOT);
    }
}
