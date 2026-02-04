package com.nutriassistant.nutriassistant_back.domain.review.controller;

import com.nutriassistant.nutriassistant_back.domain.review.dto.ReviewDto;
import com.nutriassistant.nutriassistant_back.domain.review.service.ReviewService;
import com.nutriassistant.nutriassistant_back.global.ApiResponse;
import com.nutriassistant.nutriassistant_back.global.auth.CurrentUser;
import com.nutriassistant.nutriassistant_back.global.auth.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    // 학생 리뷰 등록 API
    @PostMapping
    public ApiResponse<ReviewDto.Response> registerReview(
            @CurrentUser UserContext user,
            @RequestBody ReviewDto.RegisterRequest request) {
        // JWT에서 schoolId와 userId를 사용하여 리뷰 등록
        ReviewDto.Response response = reviewService.registerReview(request, user.getSchoolId(), user.getUserId());
        return ApiResponse.success("리뷰가 성공적으로 등록되었습니다.", response);
    }
}