package com.nutriassistant.nutriassistant_back.global.aws;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@Component
public class S3Uploader {

    private final S3Client s3Client;

    @Value("${cloud.aws.s3.bucket:}")
    private String bucket;

    @Value("${cloud.aws.region.static:ap-northeast-2}")
    private String region;

    @Autowired
    public S3Uploader(@Autowired(required = false) S3Client s3Client) {
        this.s3Client = s3Client;
    }

    public boolean isAvailable() {
        return s3Client != null;
    }

    /**
     * MultipartFile을 S3에 업로드
     *
     * @param file       업로드할 파일
     * @param directory  S3 경로 (예: schools/1/boards/101)
     * @return S3 경로 (key)
     */
    public String upload(MultipartFile file, String directory) {
        if (s3Client == null) {
            throw new S3UploadException("S3 Client가 설정되지 않았습니다.", null);
        }

        String originalFilename = file.getOriginalFilename();
        String extension = extractExtension(originalFilename);
        String uniqueFileName = UUID.randomUUID().toString() + extension;
        String s3Key = directory + "/" + uniqueFileName;

        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(s3Key)
                    .contentType(file.getContentType())
                    .contentLength(file.getSize())
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromBytes(file.getBytes()));
            log.info("S3 업로드 성공: {}", s3Key);

            return s3Key;
        } catch (IOException e) {
            log.error("S3 업로드 실패: {}", originalFilename, e);
            throw new S3UploadException("파일 업로드 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 바이트 배열을 S3에 업로드 (월간 운영자료 등에서 사용)
     *
     * @param content     업로드할 내용
     * @param s3Key       S3 경로 (key)
     * @param contentType 콘텐츠 타입
     * @return S3 URL
     */
    public String uploadBytes(byte[] content, String s3Key, String contentType) {
        if (s3Client == null) {
            throw new S3UploadException("S3 Client가 설정되지 않았습니다.", null);
        }

        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(s3Key)
                    .contentType(contentType)
                    .contentLength((long) content.length)
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromBytes(content));
            log.info("S3 바이트 업로드 성공: {}", s3Key);

            return getS3Url(s3Key);
        } catch (Exception e) {
            log.error("S3 바이트 업로드 실패: {}", s3Key, e);
            throw new S3UploadException("파일 업로드 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * S3 파일 삭제
     *
     * @param s3Key 삭제할 파일의 S3 경로
     */
    public void delete(String s3Key) {
        if (s3Client == null) {
            throw new S3UploadException("S3 Client가 설정되지 않았습니다.", null);
        }

        try {
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(s3Key)
                    .build();

            s3Client.deleteObject(deleteObjectRequest);
            log.info("S3 파일 삭제 성공: {}", s3Key);
        } catch (Exception e) {
            log.error("S3 파일 삭제 실패: {}", s3Key, e);
            throw new S3UploadException("파일 삭제 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * S3 URL 생성
     *
     * @param s3Key S3 경로
     * @return 전체 S3 URL
     */
    public String getS3Url(String s3Key) {
        return String.format("https://%s.s3.%s.amazonaws.com/%s", bucket, region, s3Key);
    }

    private String extractExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf("."));
    }

    public static class S3UploadException extends RuntimeException {
        public S3UploadException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
