package com.nutriassistant.nutriassistant_back.domain.Board.service;

import com.nutriassistant.nutriassistant_back.domain.Attachment.entity.Attachment;
import com.nutriassistant.nutriassistant_back.domain.Attachment.entity.RelatedType;
import com.nutriassistant.nutriassistant_back.domain.Attachment.repository.AttachmentRepository;
import com.nutriassistant.nutriassistant_back.domain.Auth.entity.Dietitian;
import com.nutriassistant.nutriassistant_back.domain.Auth.entity.Student;
import com.nutriassistant.nutriassistant_back.domain.Auth.repository.DietitianRepository;
import com.nutriassistant.nutriassistant_back.domain.Auth.repository.StudentRepository;
import com.nutriassistant.nutriassistant_back.domain.Board.DTO.BoardCreateRequest;
import com.nutriassistant.nutriassistant_back.domain.Board.DTO.BoardCreateResponse;
import com.nutriassistant.nutriassistant_back.domain.Board.DTO.BoardDeleteResponse;
import com.nutriassistant.nutriassistant_back.domain.Board.DTO.BoardDetailResponse;
import com.nutriassistant.nutriassistant_back.domain.Board.DTO.BoardListResponse;
import com.nutriassistant.nutriassistant_back.domain.Board.DTO.BoardUpdateRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import com.nutriassistant.nutriassistant_back.domain.Board.entity.AuthorType;
import com.nutriassistant.nutriassistant_back.domain.Board.entity.Board;
import com.nutriassistant.nutriassistant_back.domain.Board.entity.CategoryType;
import com.nutriassistant.nutriassistant_back.domain.Board.repository.BoardRepository;
import com.nutriassistant.nutriassistant_back.domain.NewMenu.service.NewMenuService;
import com.nutriassistant.nutriassistant_back.global.aws.S3Uploader;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class BoardService {

    private static final int MAX_FILE_COUNT = 3;
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB

    private final BoardRepository boardRepository;
    private final AttachmentRepository attachmentRepository;
    private final NewMenuService newMenuService;
    private final DietitianRepository dietitianRepository;
    private final StudentRepository studentRepository;
    private final S3Uploader s3Uploader;

    public BoardService(BoardRepository boardRepository,
                        AttachmentRepository attachmentRepository,
                        NewMenuService newMenuService,
                        DietitianRepository dietitianRepository,
                        StudentRepository studentRepository,
                        S3Uploader s3Uploader) {
        this.boardRepository = boardRepository;
        this.attachmentRepository = attachmentRepository;
        this.newMenuService = newMenuService;
        this.dietitianRepository = dietitianRepository;
        this.studentRepository = studentRepository;
        this.s3Uploader = s3Uploader;
    }

    @Transactional
    public BoardCreateResponse createBoard(BoardCreateRequest request, Long schoolId, Long authorId, String role) {
        log.info("📝 게시글 등록 요청: category={}, title={}, authorId={}, role={}",
                request.getCategory(), request.getTitle(), authorId, role);

        // 1. 카테고리 파싱
        CategoryType category;
        try {
            category = CategoryType.valueOf(request.getCategory().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("유효하지 않은 카테고리입니다: " + request.getCategory());
        }

        // 2. 작성자 타입 결정 (JWT role에서 추출)
        AuthorType authorType;
        if ("ROLE_DIETITIAN".equals(role)) {
            authorType = AuthorType.DIETITIAN;
        } else if ("ROLE_STUDENT".equals(role)) {
            authorType = AuthorType.STUDENT;
        } else {
            throw new IllegalArgumentException("유효하지 않은 사용자 권한입니다: " + role);
        }

        // 3. 작성자 이름 조회
        String authorName = resolveAuthorName(authorId, authorType);

        // 4. 게시글 저장
        Board board = new Board(
                schoolId,
                category,
                request.getTitle(),
                request.getContent(),
                authorId,
                authorName,
                authorType
        );
        Board savedBoard = boardRepository.save(board);
        log.info("✅ 게시글 저장 완료: id={}", savedBoard.getId());

        // 4. 첨부파일 저장
        List<Attachment> savedAttachments = new ArrayList<>();
        if (request.getAttachments() != null && !request.getAttachments().isEmpty()) {
            for (BoardCreateRequest.AttachmentRequest attachmentReq : request.getAttachments()) {
                // S3 경로 업데이트 (tmp -> 실제 경로)
                String finalS3Path = updateS3Path(attachmentReq.getS3Path(), schoolId, savedBoard.getId());

                Attachment attachment = new Attachment(
                        RelatedType.BOARD,
                        savedBoard.getId(),
                        attachmentReq.getFileName(),
                        finalS3Path,
                        attachmentReq.getFileType()
                );
                savedAttachments.add(attachmentRepository.save(attachment));
            }
            log.info("✅ 첨부파일 저장 완료: {}개", savedAttachments.size());
        }

        // 5. NEW_MENU 카테고리인 경우 비동기로 분석 요청
        if (category == CategoryType.NEW_MENU) {
            newMenuService.requestAnalysisAsync(savedBoard);
            log.info("🔄 신메뉴 분석 비동기 요청 전송: boardId={}", savedBoard.getId());
        }

        // 6. 응답 생성
        List<BoardCreateResponse.AttachmentResponse> attachmentResponses = savedAttachments.stream()
                .map(this::toAttachmentResponse)
                .collect(Collectors.toList());

        return BoardCreateResponse.builder()
                .id(savedBoard.getId())
                .schoolId(savedBoard.getSchoolId())
                .category(savedBoard.getCategory().name())
                .title(savedBoard.getTitle())
                .content(savedBoard.getContent())
                .authorId(savedBoard.getAuthorId())
                .authorName(savedBoard.getAuthorName())
                .authorType(savedBoard.getAuthorType().name())
                .viewCount(savedBoard.getViewCount())
                .attachments(attachmentResponses)
                .createdAt(savedBoard.getCreatedAt())
                .updatedAt(savedBoard.getUpdatedAt())
                .build();
    }

    /**
     * 게시글 등록 + 파일 업로드 (한 번에 처리)
     */
    @Transactional
    public BoardCreateResponse createBoardWithFiles(
            BoardCreateRequest request,
            List<MultipartFile> files,
            Long schoolId,
            Long authorId,
            String role
    ) {
        log.info("📝 게시글 등록 요청 (파일 포함): category={}, title={}, authorId={}, role={}, 파일 수={}",
                request.getCategory(), request.getTitle(), authorId, role,
                files != null ? files.size() : 0);

        // 1. 파일 검증
        if (files != null && !files.isEmpty()) {
            if (files.size() > MAX_FILE_COUNT) {
                throw new IllegalArgumentException(
                        String.format("파일은 최대 %d개까지 첨부 가능합니다.", MAX_FILE_COUNT));
            }
            for (MultipartFile file : files) {
                if (file.getSize() > MAX_FILE_SIZE) {
                    throw new IllegalArgumentException("파일 용량이 제한(10MB)을 초과했습니다: " + file.getOriginalFilename());
                }
            }
        }

        // 2. 카테고리 파싱
        CategoryType category;
        try {
            category = CategoryType.valueOf(request.getCategory().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("유효하지 않은 카테고리입니다: " + request.getCategory());
        }

        // 3. 작성자 타입 결정
        AuthorType authorType;
        if ("ROLE_DIETITIAN".equals(role)) {
            authorType = AuthorType.DIETITIAN;
        } else if ("ROLE_STUDENT".equals(role)) {
            authorType = AuthorType.STUDENT;
        } else {
            throw new IllegalArgumentException("유효하지 않은 사용자 권한입니다: " + role);
        }

        // 4. 작성자 이름 조회
        String authorName = resolveAuthorName(authorId, authorType);

        // 5. 게시글 저장
        Board board = new Board(
                schoolId,
                category,
                request.getTitle(),
                request.getContent(),
                authorId,
                authorName,
                authorType
        );
        Board savedBoard = boardRepository.save(board);
        log.info("✅ 게시글 저장 완료: id={}", savedBoard.getId());

        // 6. 파일 업로드 및 첨부파일 저장
        List<Attachment> savedAttachments = new ArrayList<>();
        if (files != null && !files.isEmpty()) {
            String directory = String.format("schools/%d/boards/%d", schoolId, savedBoard.getId());

            for (MultipartFile file : files) {
                if (file != null && !file.isEmpty()) {
                    // S3 업로드
                    String s3Key = s3Uploader.upload(file, directory);

                    // Attachment 저장
                    Attachment attachment = new Attachment(
                            RelatedType.BOARD,
                            savedBoard.getId(),
                            file.getOriginalFilename(),
                            s3Key,
                            file.getContentType(),
                            file.getSize()
                    );
                    savedAttachments.add(attachmentRepository.save(attachment));
                }
            }
            log.info("✅ 파일 업로드 및 첨부파일 저장 완료: {}개", savedAttachments.size());
        }

        // 7. NEW_MENU 카테고리인 경우 비동기로 분석 요청
        if (category == CategoryType.NEW_MENU) {
            newMenuService.requestAnalysisAsync(savedBoard);
            log.info("🔄 신메뉴 분석 비동기 요청 전송: boardId={}", savedBoard.getId());
        }

        // 8. 응답 생성
        List<BoardCreateResponse.AttachmentResponse> attachmentResponses = savedAttachments.stream()
                .map(this::toAttachmentResponse)
                .collect(Collectors.toList());

        return BoardCreateResponse.builder()
                .id(savedBoard.getId())
                .schoolId(savedBoard.getSchoolId())
                .category(savedBoard.getCategory().name())
                .title(savedBoard.getTitle())
                .content(savedBoard.getContent())
                .authorId(savedBoard.getAuthorId())
                .authorName(savedBoard.getAuthorName())
                .authorType(savedBoard.getAuthorType().name())
                .viewCount(savedBoard.getViewCount())
                .attachments(attachmentResponses)
                .createdAt(savedBoard.getCreatedAt())
                .updatedAt(savedBoard.getUpdatedAt())
                .build();
    }

    private BoardCreateResponse.AttachmentResponse toAttachmentResponse(Attachment attachment) {
        return BoardCreateResponse.AttachmentResponse.builder()
                .id(attachment.getId())
                .relatedType(attachment.getRelatedType().name())
                .relatedId(attachment.getRelatedId())
                .fileName(attachment.getFileName())
                .s3Path(attachment.getS3Path())
                .fileType(attachment.getFileType())
                .createdAt(attachment.getCreatedAt())
                .build();
    }

    /**
     * 임시 S3 경로를 실제 경로로 변경
     * 예: schools/1/boards/tmp/9f2a.../menu.png -> schools/1/boards/101/menu.png
     */
    private String updateS3Path(String tmpPath, Long schoolId, Long boardId) {
        if (tmpPath == null || !tmpPath.contains("/tmp/")) {
            return tmpPath;
        }
        // tmp 경로에서 파일명 추출
        String fileName = tmpPath.substring(tmpPath.lastIndexOf("/") + 1);
        return String.format("schools/%d/boards/%d/%s", schoolId, boardId, fileName);
    }

    @Transactional(readOnly = true)
    public BoardListResponse getBoardList(Long schoolId, String category, String keyword, int page, int size) {
        CategoryType categoryType = null;
        if (category != null && !category.isBlank()) {
            try {
                categoryType = CategoryType.valueOf(category.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("유효하지 않은 카테고리입니다: " + category);
            }
        }

        Page<Board> boardPage = boardRepository.findByFilters(
                schoolId,
                categoryType,
                keyword,
                PageRequest.of(page, size)
        );

        List<BoardListResponse.BoardItem> items = boardPage.getContent().stream()
                .map(this::toBoardItem)
                .collect(Collectors.toList());

        return BoardListResponse.builder()
                .currentPage(page + 1)
                .pageSize(size)
                .totalPages(boardPage.getTotalPages())
                .totalItems(boardPage.getTotalElements())
                .items(items)
                .build();
    }

    private BoardListResponse.BoardItem toBoardItem(Board board) {
        boolean hasAttachment = attachmentRepository.existsByRelatedTypeAndRelatedId(
                RelatedType.BOARD, board.getId()
        );

        return BoardListResponse.BoardItem.builder()
                .id(board.getId())
                .schoolId(board.getSchoolId())
                .category(board.getCategory().name())
                .title(board.getTitle())
                .authorId(board.getAuthorId())
                .authorName(board.getAuthorName())
                .authorType(board.getAuthorType().name())
                .viewCount(board.getViewCount())
                .createdAt(board.getCreatedAt())
                .updatedAt(board.getUpdatedAt())
                .hasAttachment(hasAttachment)
                .build();
    }

    @Transactional
    public BoardDetailResponse getBoardDetail(Long boardId, Long schoolId, Long currentUserId) {
        log.info("📖 게시글 상세 조회: boardId={}, schoolId={}", boardId, schoolId);

        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new BoardNotFoundException("해당 게시글을 찾을 수 없습니다."));

        if (board.getDeleted()) {
            throw new BoardDeletedException("삭제된 게시글입니다.");
        }

        // school_id 필터링: 본인 학교 게시글만 조회 가능
        if (!board.getSchoolId().equals(schoolId)) {
            throw new BoardNotFoundException("해당 게시글을 찾을 수 없습니다.");
        }

        // 조회수 증가
        board.incrementViewCount();
        boardRepository.save(board);

        // 첨부파일 조회
        List<Attachment> attachments = attachmentRepository.findByRelatedTypeAndRelatedId(
                RelatedType.BOARD, boardId
        );

        List<BoardDetailResponse.AttachmentInfo> attachmentInfos = attachments.stream()
                .map(this::toAttachmentInfo)
                .collect(Collectors.toList());

        // 본인 게시글 여부 확인
        boolean isMine = board.getAuthorId().equals(currentUserId);

        return BoardDetailResponse.builder()
                .id(board.getId())
                .schoolId(board.getSchoolId())
                .category(board.getCategory().name())
                .title(board.getTitle())
                .content(board.getContent())
                .authorId(board.getAuthorId())
                .authorType(board.getAuthorType().name())
                .authorName(board.getAuthorName())
                .viewCount(board.getViewCount())
                .createdAt(board.getCreatedAt())
                .updatedAt(board.getUpdatedAt())
                .attachments(attachmentInfos)
                .isMine(isMine)
                .isEditable(isMine)
                .build();
    }

    private BoardDetailResponse.AttachmentInfo toAttachmentInfo(Attachment attachment) {
        return BoardDetailResponse.AttachmentInfo.builder()
                .fileId(attachment.getId())
                .fileName(attachment.getFileName())
                .fileUrl(attachment.getS3Path())
                .fileSize(attachment.getFileSize())
                .build();
    }

    @Transactional
    public BoardCreateResponse updateBoard(Long boardId, Long schoolId, BoardUpdateRequest request, Long currentUserId) {
        log.info("✏️ 게시글 수정 요청: boardId={}, schoolId={}", boardId, schoolId);

        // 1. 게시글 조회
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new BoardNotFoundException("존재하지 않는 게시글입니다."));

        if (board.getDeleted()) {
            throw new BoardDeletedException("삭제된 게시글입니다.");
        }

        // 2. school_id 검증 (본인 학교 게시글만 수정 가능)
        if (!board.getSchoolId().equals(schoolId)) {
            throw new BoardNotFoundException("존재하지 않는 게시글입니다.");
        }

        // 3. 권한 확인 (작성자만 수정 가능)
        if (!board.getAuthorId().equals(currentUserId)) {
            throw new BoardForbiddenException("게시글 수정 권한이 없습니다.");
        }

        // 3. 카테고리 파싱 (있는 경우만)
        CategoryType category = null;
        if (request.getCategory() != null && !request.getCategory().isBlank()) {
            try {
                category = CategoryType.valueOf(request.getCategory().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("유효하지 않은 카테고리입니다: " + request.getCategory());
            }
        }

        // 4. 게시글 수정
        board.update(category, request.getTitle(), request.getContent());
        Board savedBoard = boardRepository.save(board);
        log.info("✅ 게시글 수정 완료: id={}", savedBoard.getId());

        // 5. 새 첨부파일 추가 (기존 첨부파일 유지)
        if (request.getAttachments() != null && !request.getAttachments().isEmpty()) {
            for (BoardUpdateRequest.AttachmentRequest attachmentReq : request.getAttachments()) {
                String finalS3Path = updateS3Path(attachmentReq.getS3Path(), board.getSchoolId(), boardId);

                Attachment attachment = new Attachment(
                        RelatedType.BOARD,
                        boardId,
                        attachmentReq.getFileName(),
                        finalS3Path,
                        attachmentReq.getFileType()
                );
                attachmentRepository.save(attachment);
            }
            log.info("✅ 새 첨부파일 추가 완료: {}개", request.getAttachments().size());
        }

        // 6. 전체 첨부파일 조회 (기존 + 신규)
        List<Attachment> allAttachments = attachmentRepository.findByRelatedTypeAndRelatedId(
                RelatedType.BOARD, boardId
        );

        List<BoardCreateResponse.AttachmentResponse> attachmentResponses = allAttachments.stream()
                .map(this::toAttachmentResponse)
                .collect(Collectors.toList());

        return BoardCreateResponse.builder()
                .id(savedBoard.getId())
                .schoolId(savedBoard.getSchoolId())
                .category(savedBoard.getCategory().name())
                .title(savedBoard.getTitle())
                .content(savedBoard.getContent())
                .authorId(savedBoard.getAuthorId())
                .authorName(savedBoard.getAuthorName())
                .authorType(savedBoard.getAuthorType().name())
                .viewCount(savedBoard.getViewCount())
                .attachments(attachmentResponses)
                .createdAt(savedBoard.getCreatedAt())
                .updatedAt(savedBoard.getUpdatedAt())
                .build();
    }

    /**
     * 게시글 수정 + 파일 수정 (한 번에 처리)
     * - 기존 파일 삭제 (deleteFileIds)
     * - 새 파일 업로드 (files)
     * - 게시글 내용 수정 (category, title, content)
     */
    @Transactional
    public BoardCreateResponse updateBoardWithFiles(
            Long boardId,
            Long schoolId,
            Long currentUserId,
            String category,
            String title,
            String content,
            List<Long> deleteFileIds,
            List<MultipartFile> files
    ) {
        log.info("✏️ 게시글 수정 요청 (파일 포함): boardId={}, schoolId={}, 삭제 파일={}, 추가 파일={}",
                boardId, schoolId,
                deleteFileIds != null ? deleteFileIds.size() : 0,
                files != null ? files.size() : 0);

        // 1. 게시글 조회
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new BoardNotFoundException("존재하지 않는 게시글입니다."));

        if (board.getDeleted()) {
            throw new BoardDeletedException("삭제된 게시글입니다.");
        }

        // 2. school_id 검증 (본인 학교 게시글만 수정 가능)
        if (!board.getSchoolId().equals(schoolId)) {
            throw new BoardNotFoundException("존재하지 않는 게시글입니다.");
        }

        // 3. 권한 확인 (작성자만 수정 가능)
        if (!board.getAuthorId().equals(currentUserId)) {
            throw new BoardForbiddenException("게시글 수정 권한이 없습니다.");
        }

        // 4. 파일 삭제 처리
        if (deleteFileIds != null && !deleteFileIds.isEmpty()) {
            for (Long fileId : deleteFileIds) {
                attachmentRepository.findById(fileId).ifPresent(attachment -> {
                    // 해당 파일이 이 게시글의 첨부파일인지 확인
                    if (attachment.getRelatedType() == RelatedType.BOARD
                            && attachment.getRelatedId().equals(boardId)) {
                        // S3에서 파일 삭제
                        try {
                            s3Uploader.delete(attachment.getS3Path());
                            log.info("S3 파일 삭제 완료: {}", attachment.getS3Path());
                        } catch (Exception e) {
                            log.warn("S3 파일 삭제 실패 (무시하고 계속 진행): {}", attachment.getS3Path(), e);
                        }
                        // DB에서 Attachment 삭제
                        attachmentRepository.delete(attachment);
                        log.info("첨부파일 DB 삭제 완료: id={}", fileId);
                    } else {
                        log.warn("첨부파일이 해당 게시글에 속하지 않음: fileId={}, boardId={}", fileId, boardId);
                    }
                });
            }
        }

        // 5. 새 파일 업로드 검증 및 처리
        List<Attachment> newAttachments = new ArrayList<>();
        if (files != null && !files.isEmpty()) {
            // 현재 첨부파일 개수 조회
            List<Attachment> currentAttachments = attachmentRepository.findByRelatedTypeAndRelatedId(
                    RelatedType.BOARD, boardId
            );
            int currentCount = currentAttachments.size();

            // 파일 개수 검증 (현재 파일 + 새 파일이 MAX_FILE_COUNT 이하인지)
            if (currentCount + files.size() > MAX_FILE_COUNT) {
                throw new IllegalArgumentException(
                        String.format("첨부파일은 최대 %d개까지 가능합니다. 현재: %d개, 추가 요청: %d개",
                                MAX_FILE_COUNT, currentCount, files.size()));
            }

            // 파일 크기 검증
            for (MultipartFile file : files) {
                if (file.getSize() > MAX_FILE_SIZE) {
                    throw new IllegalArgumentException("파일 용량이 제한(10MB)을 초과했습니다: " + file.getOriginalFilename());
                }
            }

            // 파일 업로드
            String directory = String.format("schools/%d/boards/%d", schoolId, boardId);
            for (MultipartFile file : files) {
                if (file != null && !file.isEmpty()) {
                    // S3 업로드
                    String s3Key = s3Uploader.upload(file, directory);

                    // Attachment 저장
                    Attachment attachment = new Attachment(
                            RelatedType.BOARD,
                            boardId,
                            file.getOriginalFilename(),
                            s3Key,
                            file.getContentType(),
                            file.getSize()
                    );
                    newAttachments.add(attachmentRepository.save(attachment));
                }
            }
            log.info("✅ 새 파일 업로드 완료: {}개", newAttachments.size());
        }

        // 6. 카테고리 파싱 (있는 경우만)
        CategoryType categoryType = null;
        if (category != null && !category.isBlank()) {
            try {
                categoryType = CategoryType.valueOf(category.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("유효하지 않은 카테고리입니다: " + category);
            }
        }

        // 7. 게시글 수정
        board.update(categoryType, title, content);
        Board savedBoard = boardRepository.save(board);
        log.info("✅ 게시글 수정 완료: id={}", savedBoard.getId());

        // 8. 전체 첨부파일 조회 (기존 + 신규)
        List<Attachment> allAttachments = attachmentRepository.findByRelatedTypeAndRelatedId(
                RelatedType.BOARD, boardId
        );

        List<BoardCreateResponse.AttachmentResponse> attachmentResponses = allAttachments.stream()
                .map(this::toAttachmentResponse)
                .collect(Collectors.toList());

        return BoardCreateResponse.builder()
                .id(savedBoard.getId())
                .schoolId(savedBoard.getSchoolId())
                .category(savedBoard.getCategory().name())
                .title(savedBoard.getTitle())
                .content(savedBoard.getContent())
                .authorId(savedBoard.getAuthorId())
                .authorName(savedBoard.getAuthorName())
                .authorType(savedBoard.getAuthorType().name())
                .viewCount(savedBoard.getViewCount())
                .attachments(attachmentResponses)
                .createdAt(savedBoard.getCreatedAt())
                .updatedAt(savedBoard.getUpdatedAt())
                .build();
    }

    @Transactional
    public BoardDeleteResponse deleteBoard(Long boardId, Long schoolId, Long currentUserId) {
        log.info("🗑️ 게시글 삭제 요청: boardId={}, schoolId={}, userId={}", boardId, schoolId, currentUserId);

        // 1. 게시글 조회
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new BoardNotFoundException("해당 게시글을 찾을 수 없습니다."));

        // 2. 이미 삭제된 게시글인지 확인
        if (board.getDeleted()) {
            throw new BoardNotFoundException("해당 게시글을 찾을 수 없습니다.");
        }

        // 3. school_id 검증 (본인 학교 게시글만 삭제 가능)
        if (!board.getSchoolId().equals(schoolId)) {
            throw new BoardNotFoundException("해당 게시글을 찾을 수 없습니다.");
        }

        // 4. 권한 확인 (작성자만 삭제 가능)
        if (!board.getAuthorId().equals(currentUserId)) {
            throw new BoardForbiddenException("게시글 삭제 권한이 없습니다.");
        }

        // 5. 첨부파일 삭제 (S3 + DB)
        List<Attachment> attachments = attachmentRepository.findByRelatedTypeAndRelatedId(
                RelatedType.BOARD, boardId
        );

        // S3 파일 먼저 모두 삭제 (하나라도 실패하면 전체 롤백)
        for (Attachment attachment : attachments) {
            try {
                s3Uploader.delete(attachment.getS3Path());
                log.info("S3 파일 삭제 완료: {}", attachment.getS3Path());
            } catch (Exception e) {
                log.error("S3 파일 삭제 실패: {}", attachment.getS3Path(), e);
                throw new BoardDeleteException("파일 삭제 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.");
            }
        }

        // S3 삭제 성공 후 DB에서 Attachment 삭제
        attachmentRepository.deleteAll(attachments);
        log.info("✅ 첨부파일 삭제 완료: {}개", attachments.size());

        // 6. Soft Delete 수행
        board.softDelete();
        boardRepository.save(board);
        log.info("✅ 게시글 삭제 완료 (Soft Delete): boardId={}", boardId);

        return BoardDeleteResponse.builder()
                .boardId(boardId)
                .deleted(true)
                .deleteType("SOFT")
                .deletedAt(board.getDeletedAt())
                .build();
    }

    private String resolveAuthorName(Long authorId, AuthorType authorType) {
        if (authorType == AuthorType.DIETITIAN) {
            Dietitian dietitian = dietitianRepository.findById(authorId)
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 영양사 ID입니다: " + authorId));
            return dietitian.getName();
        } else if (authorType == AuthorType.STUDENT) {
            Student student = studentRepository.findById(authorId)
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 학생 ID입니다: " + authorId));
            return student.getName();
        }
        throw new IllegalArgumentException("알 수 없는 작성자 유형입니다: " + authorType);
    }

    public static class BoardNotFoundException extends RuntimeException {
        public BoardNotFoundException(String message) {
            super(message);
        }
    }

    public static class BoardDeletedException extends RuntimeException {
        public BoardDeletedException(String message) {
            super(message);
        }
    }

    public static class BoardForbiddenException extends RuntimeException {
        public BoardForbiddenException(String message) {
            super(message);
        }
    }

    public static class BoardDeleteException extends RuntimeException {
        public BoardDeleteException(String message) {
            super(message);
        }
    }
}
