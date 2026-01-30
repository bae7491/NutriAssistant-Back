package com.nutriassistant.nutriassistant_back.domain.review.service;

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
    public ReviewDto.Response registerReview(ReviewDto.RegisterRequest request) {

        Review review = Review.builder()
                .studentId(request.getStudent_id())
                .schoolId(request.getSchool_id())
                .date(request.getDate())
                .mealType(request.getMeal_type())
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
                .meal_type(entity.getMealType())
                .rating(entity.getRating())
                .content(entity.getContent())
                .created_at(entity.getCreatedAt())
                .build();
    }
}