package com.inuteamflow.server.domain.user.service;

import com.inuteamflow.server.domain.user.dto.request.UserUpdateRequest;
import com.inuteamflow.server.domain.user.dto.response.MyInfoResponse;
import com.inuteamflow.server.domain.user.entity.User;
import com.inuteamflow.server.domain.user.repository.UserRepository;
import com.inuteamflow.server.global.exception.error.CustomErrorCode;
import com.inuteamflow.server.global.exception.error.RestApiException;
import com.inuteamflow.server.global.s3.S3Service;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final S3Service s3Service;
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;

    public MyInfoResponse getMyInfo(
            User user
    ) {
        String imageUrl = s3Service.getImageUrl(user.getImageKey());
        return MyInfoResponse.create(user, imageUrl);
    }

    @Transactional
    public MyInfoResponse updateMyInfo(
            User user,
            UserUpdateRequest request
    ) {
        // Repository 에서 로그인한 사용자를 조회 → 수정 사항을 DB에 저장하기 위함
        User requester = userRepository.findById(user.getUserId())
                .orElseThrow(() -> new RestApiException(CustomErrorCode.USER_NOT_FOUND));

        // 오래된 이미지키를 변경하기 위해 삭제 전 저장
        String oldImageKey = requester.getImageKey();

        // 사용자 정보 수정
        String encodedPassword = StringUtils.hasText(request.getPassword())
                ? bCryptPasswordEncoder.encode(request.getPassword())
                : null;
        requester.update(request, encodedPassword);

        // 요청 DTO에 담긴 이미지키가 새로운거라면 기존의 S3의 파일을 삭제
        String newImageKey = requester.getImageKey();
        if (StringUtils.hasText(oldImageKey) && !oldImageKey.equals(newImageKey)) {
            s3Service.deleteImage(oldImageKey);
        }

        // 새로운 이미지키 저장
        String imageUrl = s3Service.getImageUrl(newImageKey);
        return MyInfoResponse.create(requester, imageUrl);
    }
}
