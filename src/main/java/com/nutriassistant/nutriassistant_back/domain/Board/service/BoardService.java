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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class BoardService {

    private final BoardRepository boardRepository;
    private final AttachmentRepository attachmentRepository;
    private final NewMenuService newMenuService;
    private final DietitianRepository dietitianRepository;
    private final StudentRepository studentRepository;

    public BoardService(BoardRepository boardRepository,
                        AttachmentRepository attachmentRepository,
                        NewMenuService newMenuService,
                        DietitianRepository dietitianRepository,
                        StudentRepository studentRepository) {
        this.boardRepository = boardRepository;
        this.attachmentRepository = attachmentRepository;
        this.newMenuService = newMenuService;
        this.dietitianRepository = dietitianRepository;
        this.studentRepository = studentRepository;
    }

    @Transactional
    public BoardCreateResponse createBoard(BoardCreateRequest request, Long schoolId) {
        log.info("📝 게시글 등록 요청: category={}, title={}", request.getCategory(), request.getTitle());

        // 1. 카테고리 파싱
        CategoryType category;
        try {
            category = CategoryType.valueOf(request.getCategory().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("유효하지 않은 카테고리입니다: " + request.getCategory());
        }

        // 2. 작성자 타입 파싱
        AuthorType authorType;
        try {
            authorType = AuthorType.valueOf(request.getAuthorType().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("유효하지 않은 작성자 유형입니다: " + request.getAuthorType());
        }

        // 3. 작성자 이름 조회 (authorName이 없는 경우)
        String authorName = request.getAuthorName();
        if (authorName == null || authorName.isBlank()) {
            authorName = resolveAuthorName(request.getAuthorId(), authorType);
        }

        // 4. 게시글 저장
        Board board = new Board(
                schoolId,
                category,
                request.getTitle(),
                request.getContent(),
                request.getAuthorId(),
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
    public BoardListResponse getBoardList(String category, String keyword, int page, int size) {
        CategoryType categoryType = null;
        if (category != null && !category.isBlank()) {
            try {
                categoryType = CategoryType.valueOf(category.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("유효하지 않은 카테고리입니다: " + category);
            }
        }

        Page<Board> boardPage = boardRepository.findByFilters(
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
    public BoardDetailResponse getBoardDetail(Long boardId, Long currentUserId) {
        log.info("📖 게시글 상세 조회: boardId={}", boardId);

        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new BoardNotFoundException("해당 게시글을 찾을 수 없습니다."));

        if (board.getDeleted()) {
            throw new BoardDeletedException("삭제된 게시글입니다.");
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
    public BoardCreateResponse updateBoard(Long boardId, BoardUpdateRequest request, Long currentUserId) {
        log.info("✏️ 게시글 수정 요청: boardId={}", boardId);

        // 1. 게시글 조회
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new BoardNotFoundException("존재하지 않는 게시글입니다."));

        if (board.getDeleted()) {
            throw new BoardDeletedException("삭제된 게시글입니다.");
        }

        // 2. 권한 확인 (작성자만 수정 가능)
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

    @Transactional
    public BoardDeleteResponse deleteBoard(Long boardId, Long currentUserId) {
        log.info("🗑️ 게시글 삭제 요청: boardId={}, userId={}", boardId, currentUserId);

        // 1. 게시글 조회
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new BoardNotFoundException("해당 게시글을 찾을 수 없습니다."));

        // 2. 이미 삭제된 게시글인지 확인
        if (board.getDeleted()) {
            throw new BoardNotFoundException("해당 게시글을 찾을 수 없습니다.");
        }

        // 3. 권한 확인 (작성자만 삭제 가능)
        if (!board.getAuthorId().equals(currentUserId)) {
            throw new BoardForbiddenException("게시글 삭제 권한이 없습니다.");
        }

        // 4. Soft Delete 수행
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
}
