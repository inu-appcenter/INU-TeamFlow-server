package com.inuteamflow.server.global.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.inuteamflow.server.global.exception.error.CustomErrorCode;
import com.inuteamflow.server.global.exception.error.RestApiException;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;

@Slf4j
@Profile("!test")
@Configuration
public class FirebaseConfig {

    private static final int CONNECTION_TIMEOUT = 5000;
    private static final int READ_TIMEOUT = 10000;

    private final ClassPathResource firebaseResource;
    private final String projectId;

    public FirebaseConfig(
            @Value("${fcm.file_path}") String firebaseFilePath, @Value("${fcm.project_id}") String projectId) {
        this.firebaseResource = new ClassPathResource(firebaseFilePath);
        this.projectId = projectId;
    }

    @PostConstruct
    public void initialize() {
        try (InputStream serviceAccount = firebaseResource.getInputStream()) {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .setProjectId(projectId)
                    .setConnectTimeout(CONNECTION_TIMEOUT)
                    .setReadTimeout(READ_TIMEOUT)
                    .build();

            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
                log.info(
                        "Firebase Admin SDK 초기화 완료 - Project ID: {}, connectTimeout: {}ms, readTimeout: {}ms",
                        projectId,
                        CONNECTION_TIMEOUT,
                        READ_TIMEOUT);
            }

        } catch (IOException e) {
            log.error("Firebase Admin SDK 초기화 실패", e);
            throw new RestApiException(CustomErrorCode.FIREBASE_INITIALIZATION_FAILED);
        }
    }
}
