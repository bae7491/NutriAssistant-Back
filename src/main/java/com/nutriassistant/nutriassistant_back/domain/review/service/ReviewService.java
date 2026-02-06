package com.nutriassistant.nutriassistant_back.domain.review.service;

import com.nutriassistant.nutriassistant_back.domain.MealPlan.entity.MealType;
import com.nutriassistant.nutriassistant_back.domain.review.dto.ReviewDto;
import com.nutriassistant.nutriassistant_back.domain.review.entity.Review;
import com.nutriassistant.nutriassistant_back.domain.review.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewService {

    private final ReviewRepository reviewRepository;

    // 리뷰 등록
    @Transactional
    public ReviewDto.Response registerReview(ReviewDto.RegisterRequest request, Long schoolId, Long studentId) {

        // =================================================================
        // [신규 추가] 중복 리뷰 체크 로직
        // =================================================================
        boolean alreadyReviewed = reviewRepository.existsByStudentIdAndDateAndMealType(
                studentId,
                request.getDate(),
                MealType.valueOf(request.getMeal_type())
        );

        if (alreadyReviewed) {
            // 이미 존재하면 에러 발생 (프론트엔드에서 409 Conflict 또는 400 Bad Request로 처리)
            throw new IllegalArgumentException("이미 해당 식단에 대한 평가를 완료했습니다.");
        }

        // 중복이 아니면 리뷰 생성 및 저장 진행
        Review review = Review.builder()
                .studentId(studentId)
                .schoolId(schoolId)
                .date(request.getDate())
                .mealType(MealType.valueOf(request.getMeal_type()))
                .rating(request.getRating())
                .content(request.getContent())
                .build();

        Review savedReview = reviewRepository.save(review);

        return mapToResponse(savedReview);
    }

    // DTO 변환 헬퍼
    private ReviewDto.Response mapToResponse(Review entity) {
        return ReviewDto.Response.builder()
                .id(entity.getId())
                .student_id(entity.getStudentId())
                .school_id(entity.getSchoolId())
                .date(entity.getDate())
                .meal_type(String.valueOf(entity.getMealType()))
                .rating(entity.getRating())
                .content(entity.getContent())
                .created_at(entity.getCreatedAt())
                .build();
    }
}