package com.nutriassistant.nutriassistant_back.domain.reviewanalysis.service;

import com.nutriassistant.nutriassistant_back.domain.review.entity.Review;
import com.nutriassistant.nutriassistant_back.domain.review.repository.ReviewRepository;
import com.nutriassistant.nutriassistant_back.domain.reviewanalysis.dto.FastApiDto;
import com.nutriassistant.nutriassistant_back.domain.reviewanalysis.entity.ReviewAnalysis;
import com.nutriassistant.nutriassistant_back.domain.reviewanalysis.repository.ReviewAnalysisRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

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

    // [변경] RestTemplate + @Value 삭제 -> RestClient 사용
    // RestClientConfig에서 이미 URL 설정이 완료된 상태로 주입됩니다.
    private final RestClient restClient;

    // [1] 가장 최근 분석 결과 1건 조회 (Controller용)
    public ReviewAnalysis getLatestAnalysis(Long schoolId) {
        return reviewAnalysisRepository.findTopBySchoolIdOrderByTargetYmDesc(schoolId);
    }

    // [2] 분석 결과 리스트 조회 (Controller용)
    public List<ReviewAnalysis> getAnalysisList(Long schoolId) {
        return reviewAnalysisRepository.findBySchoolId(schoolId);
    }

    // [3] 일일 분석 실행 로직
    @Transactional
    public void runDailyAnalysis(Long schoolId, LocalDate targetDate) {
        log.info("일일 감성 분석 시작 - School: {}, Date: {}", schoolId, targetDate);

        LocalDateTime startOfDay = targetDate.atStartOfDay();
        LocalDateTime endOfDay = targetDate.plusDays(1).atStartOfDay();

        List<Review> reviews = reviewRepository.findBySchoolIdAndCreatedAtBetween(schoolId, startOfDay, endOfDay);

        if (reviews.isEmpty()) {
            log.info("분석할 리뷰가 없습니다. SchoolId: {}, Date: {}", schoolId, targetDate);
            return;
        }

        // FastAPI 요청 데이터 생성
        List<String> texts = reviews.stream()
                .map(Review::getContent)
                .collect(Collectors.toList());

        FastApiDto.Request requestDto = FastApiDto.Request.builder()
                .schoolId(schoolId)
                .targetDate(targetDate.toString())
                .reviewTexts(texts)
                .build();

        try {
            // [변경] RestClient를 사용한 깔끔한 호출
            FastApiDto.Response response = restClient.post()
                    .uri("/api/analyze/daily") // Base URL은 이미 설정됨
                    .body(requestDto)
                    .retrieve()
                    .body(FastApiDto.Response.class);

            if (response != null) {
                ReviewAnalysis analysis = ReviewAnalysis.builder()
                        .schoolId(schoolId)
                        .targetYm(targetDate.toString())
                        .sentimentLabel(response.getSentimentLabel())
                        .sentimentScore(response.getSentimentScore())
                        .sentimentConf(response.getSentimentConf())
                        .positiveCount(response.getPositiveCount()) // 개수 저장
                        .negativeCount(response.getNegativeCount()) // 개수 저장
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
}