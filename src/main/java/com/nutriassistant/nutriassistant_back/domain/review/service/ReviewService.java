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

        // 1. [변환] String -> Enum 변환을 딱 한 번만 수행
        // (잘못된 값이 들어오면 여기서 바로 IllegalArgumentException이 발생하여 처리가 깔끔해집니다)
        MealType mealType = MealType.valueOf(request.getMeal_type());

        // 2. [검증] 변환된 Enum 변수(mealType) 사용
        boolean alreadyReviewed = reviewRepository.existsByStudentIdAndDateAndMealType(
                studentId,
                request.getDate(),
                mealType
        );

        if (alreadyReviewed) {
            throw new IllegalArgumentException("이미 해당 식단에 대한 평가를 완료했습니다.");
        }

        // 3. [저장] 여기서도 변환된 Enum 변수(mealType) 재사용
        Review review = Review.builder()
                .studentId(studentId)
                .schoolId(schoolId)
                .date(request.getDate())
                .mealType(mealType)
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
                .meal_type(entity.getMealType().name()) // Enum -> String 변환
                .rating(entity.getRating())
                .content(entity.getContent())
                .created_at(entity.getCreatedAt())
                .build();
    }
}