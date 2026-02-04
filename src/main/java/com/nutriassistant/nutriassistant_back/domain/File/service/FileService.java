package com.nutriassistant.nutriassistant_back.domain.File.service;

import com.nutriassistant.nutriassistant_back.domain.Attachment.entity.Attachment;
import com.nutriassistant.nutriassistant_back.domain.Attachment.entity.RelatedType;
import com.nutriassistant.nutriassistant_back.domain.Attachment.repository.AttachmentRepository;
import com.nutriassistant.nutriassistant_back.domain.Board.entity.Board;
import com.nutriassistant.nutriassistant_back.domain.Board.repository.BoardRepository;
import com.nutriassistant.nutriassistant_back.domain.File.dto.FileUploadResponse;
import com.nutriassistant.nutriassistant_back.global.aws.S3Uploader;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@Slf4j
public class FileService {

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB

    private final S3Uploader s3Uploader;
    private final AttachmentRepository attachmentRepository;
    private final BoardRepository boardRepository;

    @Autowired
    public FileService(
            S3Uploader s3Uploader,
            AttachmentRepository attachmentRepository,
            BoardRepository boardRepository
    ) {
        this.s3Uploader = s3Uploader;
        this.attachmentRepository = attachmentRepository;
        this.boardRepository = boardRepository;
    }

    @Transactional
    public FileUploadResponse uploadFile(
            MultipartFile file,
            String relatedType,
            Long relatedId,
            Long schoolId
    ) {
        log.info("파일 업로드 요청: relatedType={}, relatedId={}, fileName={}",
                relatedType, relatedId, file.getOriginalFilename());

        // 0. S3 설정 확인
        if (!s3Uploader.isAvailable()) {
            throw new FileUploadException("SYS_002", "S3 설정이 되어있지 않습니다. AWS 환경변수를 확인해주세요.");
        }

        // 1. 파일 필수 검증
        if (file == null || file.isEmpty()) {
            throw new FileUploadException("FILE_001", "업로드할 파일(file)은 필수입니다.");
        }

        // 2. 파일 크기 검증
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new FileTooLargeException("FILE_413", "파일 용량이 제한(10MB)을 초과했습니다.", MAX_FILE_SIZE);
        }

        // 3. RelatedType 파싱
        RelatedType type;
        try {
            type = RelatedType.valueOf(relatedType.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new FileUploadException("FILE_002", "유효하지 않은 relatedType입니다: " + relatedType);
        }

        // 4. 연결 대상 리소스 검증 (BOARD인 경우)
        if (type == RelatedType.BOARD) {
            Board board = boardRepository.findById(relatedId)
                    .orElseThrow(() -> new FileRelatedNotFoundException(
                            "FILE_404",
                            "연결할 대상 리소스를 찾을 수 없습니다.",
                            relatedType,
                            relatedId
                    ));

            if (board.getDeleted()) {
                throw new FileRelatedNotFoundException(
                        "FILE_404",
                        "연결할 대상 리소스를 찾을 수 없습니다.",
                        relatedType,
                        relatedId
                );
            }

            // schoolId 검증
            if (!board.getSchoolId().equals(schoolId)) {
                throw new FileForbiddenException("AUTH_103", "파일 업로드 권한이 없습니다.");
            }
        }

        // 5. S3 디렉토리 경로 생성
        String directory = buildS3Directory(type, relatedId, schoolId);

        // 6. S3 업로드
        String s3Key = s3Uploader.upload(file, directory);

        // 7. Attachment 저장
        Attachment attachment = new Attachment(
                type,
                relatedId,
                file.getOriginalFilename(),
                s3Key,
                file.getContentType(),
                file.getSize()
        );
        Attachment saved = attachmentRepository.save(attachment);
        log.info("파일 업로드 성공: id={}, s3Path={}", saved.getId(), s3Key);

        // 8. 응답 생성
        return FileUploadResponse.builder()
                .id(saved.getId())
                .relatedType(saved.getRelatedType().name())
                .relatedId(saved.getRelatedId())
                .fileName(saved.getFileName())
                .fileType(saved.getFileType())
                .fileSize(saved.getFileSize())
                .s3Path(s3Key)
                .createdAt(saved.getCreatedAt())
                .build();
    }

    private String buildS3Directory(RelatedType type, Long relatedId, Long schoolId) {
        return switch (type) {
            case BOARD -> String.format("schools/%d/boards/%d", schoolId, relatedId);
            case COMMENT -> String.format("schools/%d/comments/%d", schoolId, relatedId);
            case REPORT -> String.format("schools/%d/reports/%d", schoolId, relatedId);
        };
    }

    // Custom Exceptions
    public static class FileUploadException extends RuntimeException {
        private final String code;

        public FileUploadException(String code, String message) {
            super(message);
            this.code = code;
        }

        public String getCode() {
            return code;
        }
    }

    public static class FileTooLargeException extends RuntimeException {
        private final String code;
        private final long maxBytes;

        public FileTooLargeException(String code, String message, long maxBytes) {
            super(message);
            this.code = code;
            this.maxBytes = maxBytes;
        }

        public String getCode() {
            return code;
        }

        public long getMaxBytes() {
            return maxBytes;
        }
    }

    public static class FileRelatedNotFoundException extends RuntimeException {
        private final String code;
        private final String relatedType;
        private final Long relatedId;

        public FileRelatedNotFoundException(String code, String message, String relatedType, Long relatedId) {
            super(message);
            this.code = code;
            this.relatedType = relatedType;
            this.relatedId = relatedId;
        }

        public String getCode() {
            return code;
        }

        public String getRelatedType() {
            return relatedType;
        }

        public Long getRelatedId() {
            return relatedId;
        }
    }

    public static class FileForbiddenException extends RuntimeException {
        private final String code;

        public FileForbiddenException(String code, String message) {
            super(message);
            this.code = code;
        }

        public String getCode() {
            return code;
        }
    }
}
