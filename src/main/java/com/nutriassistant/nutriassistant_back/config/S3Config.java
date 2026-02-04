package com.nutriassistant.nutriassistant_back.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Slf4j
@Configuration
public class S3Config {

    @Value("${cloud.aws.credentials.access-key:}")
    private String accessKey;

    @Value("${cloud.aws.credentials.secret-key:}")
    private String secretKey;

    @Value("${cloud.aws.region.static:ap-northeast-2}")
    private String region;

    @Bean
    public S3Client s3Client() {
        if (accessKey == null || accessKey.isEmpty() || secretKey == null || secretKey.isEmpty()) {
            log.warn("AWS S3 자격 증명이 설정되지 않았습니다. S3 기능이 비활성화됩니다.");
            return null;
        }

        AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);

        log.info("AWS S3 Client 초기화 완료: region={}", region);
        return S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .build();
    }
}
