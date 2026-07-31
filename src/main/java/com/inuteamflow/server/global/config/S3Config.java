package com.inuteamflow.server.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
public class S3Config {

    @Value("${aws.credentials.access-key}")
    private String accessKey;

    @Value("${aws.credentials.secret-key}")
    private String secretKey;

    @Value("${aws.region.static}")
    private String region;

    @Bean
    public StaticCredentialsProvider awsCredentialsProvider() {
        AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);
        return StaticCredentialsProvider.create(credentials);
    }

    @Bean
    public Region awsRegion() {
        return Region.of(region);
    }

    @Bean
    public S3Client s3Client(StaticCredentialsProvider awsCredentialsProvider, Region awsRegion) {
        return S3Client.builder()
                .credentialsProvider(awsCredentialsProvider)
                .region(awsRegion)
                .build();
    }

    @Bean
    public S3Presigner s3Presigner(StaticCredentialsProvider awsCredentialsProvider, Region awsRegion) {
        return S3Presigner.builder()
                .credentialsProvider(awsCredentialsProvider)
                .region(awsRegion)
                .build();
    }
}
