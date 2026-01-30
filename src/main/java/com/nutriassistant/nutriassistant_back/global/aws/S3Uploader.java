package com.nutriassistant.nutriassistant_back.global.aws;//package com.nutriassistant.nutriassistant_back.global.aws;
//
//import com.amazonaws.services.s3.AmazonS3;
//import com.amazonaws.services.s3.model.ObjectMetadata;
//import com.amazonaws.services.s3.model.PutObjectRequest;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.stereotype.Component;
//
//import java.io.ByteArrayInputStream;
//import java.io.IOException;
//import java.io.InputStream;
//
//@Slf4j
//@RequiredArgsConstructor
//@Component
//public class S3Uploader {
//
//    private final AmazonS3 amazonS3;
//
//    @Value("${cloud.aws.s3.bucket}")
//    private String bucket;
//
//    // [MonthlyOpsDocService에서 사용하는 메서드]
//    // 바이트 배열(JSON 문자열 등)을 받아 S3에 업로드하고 URL 반환
//    public String uploadByte(byte[] content, String fileName) {
//        ObjectMetadata metadata = new ObjectMetadata();
//        metadata.setContentLength(content.length);
//        metadata.setContentType("application/json"); // JSON 파일 기준
//
//        try (InputStream inputStream = new ByteArrayInputStream(content)) {
//            amazonS3.putObject(new PutObjectRequest(bucket, fileName, inputStream, metadata));
//        } catch (IOException e) {
//            log.error("S3 업로드 실패: {}", fileName, e);
//            throw new RuntimeException("S3 업로드 중 에러 발생");
//        }
//
//        return amazonS3.getUrl(bucket, fileName).toString();
//    }
//
//    // (필요 시 기존의 MultipartFile 업로드 메서드도 여기에 존재할 것임)
//}