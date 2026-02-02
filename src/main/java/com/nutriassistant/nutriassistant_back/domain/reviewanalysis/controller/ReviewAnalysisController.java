package com.nutriassistant.nutriassistant_back.domain.reviewanalysis.controller;

import com.nutriassistant.nutriassistant_back.domain.reviewanalysis.entity.ReviewAnalysis;
import com.nutriassistant.nutriassistant_back.domain.reviewanalysis.service.ReviewAnalysisService;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/review-analysis")
@RequiredArgsConstructor
public class ReviewAnalysisController {

    private final ReviewAnalysisService reviewAnalysisService;

    // "개수만 보여줘" 요청을 처리하는 API
    // GET /api/review-analysis/counts/1 (1번 학교의 개수 데이터 줘)
    @GetMapping("/counts/{schoolId}")
    public ResponseEntity<CountResponse> getCounts(@PathVariable Long schoolId) {

        // 1. DB에서 해당 학교의 가장 최근 분석 결과 1개를 가져옵니다.
        // (Service에 getLatestAnalysis 메서드가 필요합니다)
        ReviewAnalysis analysis = reviewAnalysisService.getLatestAnalysis(schoolId);

        if (analysis == null) {
            // 데이터가 없으면 0개로 리턴
            return ResponseEntity.ok(new CountResponse(0, 0));
        }

        // 2. 긍정/부정 개수만 쏙 뽑아서 보냅니다.
        return ResponseEntity.ok(new CountResponse(
                analysis.getPositiveCount(),
                analysis.getNegativeCount()
        ));
    }

    // 개수 전달용 작은 가방 (DTO)
    @Getter
    @AllArgsConstructor
    static class CountResponse {
        private Integer positiveCount;
        private Integer negativeCount;
    }
}