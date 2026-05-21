package com.inuteamflow.server.global.s3;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class S3Controller implements S3ControllerDocument{

    private final S3Service s3Service;

    @PostMapping("/users/me/profile/presigned-url")
    public ResponseEntity<PresignedUrlResponse> getProfilePresignedUrl(
            @Valid @RequestBody PresignedUrlRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(s3Service.getProfilePresignedUrl(request));
    }

}
