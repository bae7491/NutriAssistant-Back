package com.nutriassistant.nutriassistant_back.domain.reviewanalysis.scheduler;

import com.nutriassistant.nutriassistant_back.domain.reviewanalysis.service.ReviewAnalysisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AnalysisScheduler {

    private final ReviewAnalysisService reviewAnalysisService;

    // 매일 자정(00:00:00) 실행
    @Scheduled(cron = "0 0 0 * * *")
    public void scheduleDailyAnalysis() {
        log.info("일일 감성 분석 스케줄러 시작");

        LocalDate yesterday = LocalDate.now().minusDays(1);

        // TODO: 실제 학교 ID 리스트를 DB에서 조회하도록 수정 필요
        List<Long> schoolIds = List.of(1L, 2L);

        for (Long schoolId : schoolIds) {
            reviewAnalysisService.runDailyAnalysis(schoolId, yesterday);
        }
    }
}