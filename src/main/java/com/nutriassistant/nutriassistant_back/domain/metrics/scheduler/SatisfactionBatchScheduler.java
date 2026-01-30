package com.nutriassistant.nutriassistant_back.domain.metrics.scheduler; // 패키지 위치 변경

import com.nutriassistant.nutriassistant_back.domain.metrics.service.MetricsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Slf4j
@Component
@RequiredArgsConstructor
public class SatisfactionBatchScheduler {

    private final MetricsService metricsService;
    // private final SchoolRepository schoolRepository; // 모든 학교를 조회해야 한다면 필요

    /**
     * [일일 만족도 분석 배치]
     * 매일 밤 00시 00분 00초에 실행되어,
     * '어제' 날짜의 리뷰들을 FastAPI로 분석 요청하고 저장합니다.
     */
    @Scheduled(cron = "0 0 0 * * *")
    public void runDailyAnalysis() {
        log.info(">>> [Batch] 일일 만족도 분석 스케줄러 시작");

        LocalDate targetDate = LocalDate.now().minusDays(1); // 배치 시점(00시)의 '어제' 데이터 분석

        // [실제 운영 환경 로직]
        // 1. 등록된 모든 학교 ID를 가져옵니다.
        // List<School> schools = schoolRepository.findAll();

        // 2. 반복문을 돌며 각 학교별로 분석을 실행합니다.
        // for (School school : schools) {
        //     try {
        //         metricsService.executeDailySatisfactionAnalysis(school.getId(), targetDate);
        //     } catch (Exception e) {
        //         log.error("학교 ID {} 분석 실패: {}", school.getId(), e.getMessage());
        //         // 한 학교가 실패해도 다른 학교는 계속 진행되도록 예외 처리
        //     }
        // }

        // [현재 테스트용 하드코딩] 일단 1번 학교만 테스트
        try {
            metricsService.executeDailySatisfactionAnalysis(1L, targetDate);
        } catch (Exception e) {
            log.error("배치 작업 중 오류 발생", e);
        }

        log.info(">>> [Batch] 일일 만족도 분석 스케줄러 종료");
    }
}