package com.nutriassistant.nutriassistant_back.domain.reviewanalysis.service;

import com.nutriassistant.nutriassistant_back.domain.reviewanalysis.dto.FastApiDto;
import com.nutriassistant.nutriassistant_back.domain.reviewanalysis.entity.ReviewAnalysis;
import com.nutriassistant.nutriassistant_back.domain.reviewanalysis.repository.ReviewAnalysisRepository;
// 아래 import 경로는 실제 프로젝트의 review 패키지 위치와 일치해야 합니다.
import com.nutriassistant.nutriassistant_back.domain.review.repository.ReviewRepository;
import com.nutriassistant.nutriassistant_back.domain.review.entity.Review;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewAnalysisService {

    private final ReviewAnalysisRepository reviewAnalysisRepository;
    private final ReviewRepository reviewRepository;
    private final RestTemplate restTemplate;

    @Value("${fastapi.url}")
    private String fastApiUrl;

    @Transactional
    public void runDailyAnalysis(Long schoolId, LocalDate targetDate) {

        LocalDateTime startOfDay = targetDate.atStartOfDay();
        LocalDateTime endOfDay = targetDate.plusDays(1).atStartOfDay();

        // ReviewRepository에 해당 메소드가 없다면 추가가 필요합니다.
        List<Review> reviews = reviewRepository.findBySchoolIdAndCreatedAtBetween(schoolId, startOfDay, endOfDay);

        if (reviews.isEmpty()) {
            log.info("분석할 리뷰가 없습니다. SchoolId: {}, Date: {}", schoolId, targetDate);
            return;
        }

        List<String> texts = reviews.stream()
                .map(Review::getContent)
                .collect(Collectors.toList());

        FastApiDto.Request requestDto = FastApiDto.Request.builder()
                .schoolId(schoolId)
                .targetDate(targetDate.toString())
                .reviewTexts(texts)
                .build();

        try {
            FastApiDto.Response response = restTemplate.postForObject(
                    fastApiUrl + "/api/analyze/daily",
                    requestDto,
                    FastApiDto.Response.class
            );

            if (response != null) {
                ReviewAnalysis analysis = ReviewAnalysis.builder()
                        .schoolId(schoolId)
                        .targetYm(targetDate.toString())
                        .sentimentLabel(response.getSentimentLabel())
                        .sentimentScore(response.getSentimentScore())
                        .sentimentConf(response.getSentimentConf())
                        .aspectTags(String.join(",", response.getAspectTags()))
                        .evidencePhrases(String.join("|", response.getEvidencePhrases()))
                        .issueFlags(response.getIssueFlags())
                        .build();

                reviewAnalysisRepository.save(analysis);
                log.info("분석 완료 및 저장. SchoolId: {}", schoolId);
            }
        } catch (Exception e) {
            log.error("FastAPI 통신 중 오류 발생: {}", e.getMessage());
        }
    }

    public ReviewAnalysis getLatestAnalysis(Long schoolId) {
        // Repository에서 만들어서 호출해야 함.
        // 예: findTopBySchoolIdOrderByTargetYmDesc(schoolId)
        return reviewAnalysisRepository.findTopBySchoolIdOrderByTargetYmDesc(schoolId);
    }
}