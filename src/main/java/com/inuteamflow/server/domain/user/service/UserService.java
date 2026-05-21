package com.inuteamflow.server.domain.user.service;

import com.inuteamflow.server.domain.user.dto.request.UserUpdateRequest;
import com.inuteamflow.server.domain.user.dto.response.MyInfoResponse;
import com.inuteamflow.server.domain.user.entity.User;
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
        String oldImageKey = user.getImageKey();
        String encodedPassword = StringUtils.hasText(request.getPassword())
                ? bCryptPasswordEncoder.encode(request.getPassword())
                : null;
        user.update(request, encodedPassword);

        String newImageKey = user.getImageKey();
        if (StringUtils.hasText(oldImageKey) && !oldImageKey.equals(newImageKey)) {
            s3Service.deleteImage(oldImageKey);
        }

        String imageUrl = s3Service.getImageUrl(newImageKey);
        return MyInfoResponse.create(user, imageUrl);
    }
}
