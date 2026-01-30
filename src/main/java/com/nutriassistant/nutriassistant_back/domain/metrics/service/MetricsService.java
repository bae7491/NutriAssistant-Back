package com.nutriassistant.nutriassistant_back.domain.metrics.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nutriassistant.nutriassistant_back.domain.metrics.dto.LeftoverDto;
import com.nutriassistant.nutriassistant_back.domain.metrics.dto.SatisfactionDto;
import com.nutriassistant.nutriassistant_back.domain.metrics.dto.SkipMealDto;
import com.nutriassistant.nutriassistant_back.domain.metrics.entity.Leftover;
import com.nutriassistant.nutriassistant_back.domain.metrics.entity.ReviewAnalysis;
import com.nutriassistant.nutriassistant_back.domain.metrics.entity.SkipMeal;
import com.nutriassistant.nutriassistant_back.domain.metrics.repository.LeftoverRepository;
import com.nutriassistant.nutriassistant_back.domain.metrics.repository.ReviewAnalysisRepository;
import com.nutriassistant.nutriassistant_back.domain.metrics.repository.SkipMealRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MetricsService {

    private final SkipMealRepository skipMealRepository;
    private final LeftoverRepository leftoverRepository;
    private final ReviewAnalysisRepository reviewAnalysisRepository;

    // [변경] WebClient -> RestClient (RestClientConfig에서 설정된 빈 주입)
    private final RestClient restClient;
    private final ObjectMapper objectMapper;


    // =================================================================================
    // 1. 결식률 (Skip Meal) 로직
    // =================================================================================

    @Transactional
    public SkipMealDto.Response registerSkipMeal(SkipMealDto.RegisterRequest request) {
        if (skipMealRepository.existsBySchoolIdAndDateAndMealType(
                request.getSchool_id(), request.getDate(), request.getMeal_type())) {
            throw new IllegalArgumentException("이미 해당 날짜의 결식 데이터가 존재합니다.");
        }

        SkipMeal skipMeal = SkipMeal.builder()
                .schoolId(request.getSchool_id())
                .date(request.getDate())
                .mealType(request.getMeal_type())
                .skippedCount(request.getSkipped_count())
                .totalStudents(request.getTotal_students())
                .build();

        return mapToSkipMealResponse(skipMealRepository.save(skipMeal));
    }

    @Transactional
    public SkipMealDto.Response updateSkipMeal(SkipMealDto.UpdateRequest request) {
        SkipMeal skipMeal = skipMealRepository.findById(request.getId())
                .orElseThrow(() -> new IllegalArgumentException("데이터를 찾을 수 없습니다."));

        SkipMeal updated = SkipMeal.builder()
                .id(skipMeal.getId())
                .schoolId(skipMeal.getSchoolId())
                .date(skipMeal.getDate())
                .mealType(skipMeal.getMealType())
                .createdAt(skipMeal.getCreatedAt())
                .skippedCount(request.getSkipped_count())
                .totalStudents(request.getTotal_students())
                .build();

        return mapToSkipMealResponse(skipMealRepository.save(updated));
    }

    public SkipMealDto.Response getDailySkipMeal(Long schoolId, String mealType, LocalDate date) {
        SkipMeal skipMeal = skipMealRepository.findBySchoolIdAndDateAndMealType(schoolId, date, mealType)
                .orElse(null);

        if (skipMeal == null) return null;
        return mapToSkipMealResponse(skipMeal);
    }

    public SkipMealDto.PeriodResponse getSkipMealStats(Long schoolId, String mealType, LocalDate start, LocalDate end) {
        List<SkipMeal> list = skipMealRepository.findBySchoolIdAndMealTypeAndDateBetweenOrderByDateAsc(
                schoolId, mealType, start, end);

        List<SkipMealDto.Response> dailyData = list.stream()
                .map(this::mapToSkipMealResponse)
                .collect(Collectors.toList());

        double avgRate = dailyData.stream()
                .mapToDouble(SkipMealDto.Response::getSkip_rate)
                .average().orElse(0.0);

        return SkipMealDto.PeriodResponse.builder()
                .period(SkipMealDto.Period.builder().start_date(start).end_date(end).build())
                .school_id(schoolId)
                .meal_type(mealType)
                .average_skip_rate(Math.round(avgRate * 10) / 10.0)
                .daily_data(dailyData)
                .build();
    }

    private SkipMealDto.Response mapToSkipMealResponse(SkipMeal entity) {
        double rate = (entity.getTotalStudents() == 0) ? 0 :
                (double) entity.getSkippedCount() / entity.getTotalStudents() * 100;

        return SkipMealDto.Response.builder()
                .id(entity.getId())
                .school_id(entity.getSchoolId())
                .date(entity.getDate())
                .meal_type(entity.getMealType())
                .skipped_count(entity.getSkippedCount())
                .total_students(entity.getTotalStudents())
                .skip_rate(Math.round(rate * 10) / 10.0)
                .build();
    }


    // =================================================================================
    // 2. 잔반률 (Leftover) 로직
    // =================================================================================

    @Transactional
    public LeftoverDto.Response registerLeftover(LeftoverDto.RegisterRequest request) {
        if (leftoverRepository.existsBySchoolIdAndDateAndMealType(
                request.getSchool_id(), request.getDate(), request.getMeal_type())) {
            throw new IllegalArgumentException("이미 해당 날짜의 잔반 데이터가 존재합니다.");
        }

        Leftover leftover = Leftover.builder()
                .schoolId(request.getSchool_id())
                .date(request.getDate())
                .mealType(request.getMeal_type())
                .amountKg(request.getAmount_kg())
                .build();

        return mapToLeftoverResponse(leftoverRepository.save(leftover));
    }

    @Transactional
    public LeftoverDto.Response updateLeftover(LeftoverDto.UpdateRequest request) {
        Leftover leftover = leftoverRepository.findById(request.getId())
                .orElseThrow(() -> new IllegalArgumentException("데이터를 찾을 수 없습니다."));

        leftover.update(request.getAmount_kg());

        return mapToLeftoverResponse(leftover);
    }

    public LeftoverDto.Response getDailyLeftover(Long schoolId, String mealType, LocalDate date) {
        Leftover leftover = leftoverRepository.findBySchoolIdAndDateAndMealType(schoolId, date, mealType)
                .orElse(null);

        if (leftover == null) return null;
        return mapToLeftoverResponse(leftover);
    }

    public LeftoverDto.PeriodResponse getLeftoverStats(Long schoolId, String mealType, LocalDate start, LocalDate end) {
        List<Leftover> list = leftoverRepository.findBySchoolIdAndMealTypeAndDateBetweenOrderByDateAsc(
                schoolId, mealType, start, end);

        List<LeftoverDto.Response> dailyData = list.stream()
                .map(this::mapToLeftoverResponse)
                .collect(Collectors.toList());

        double avgAmount = dailyData.stream()
                .mapToDouble(LeftoverDto.Response::getAmount_kg)
                .average().orElse(0.0);

        return LeftoverDto.PeriodResponse.builder()
                .period(LeftoverDto.Period.builder().start_date(start).end_date(end).build())
                .school_id(schoolId)
                .meal_type(mealType)
                .average_amount_kg(Math.round(avgAmount * 10) / 10.0)
                .daily_data(dailyData)
                .build();
    }

    private LeftoverDto.Response mapToLeftoverResponse(Leftover entity) {
        return LeftoverDto.Response.builder()
                .id(entity.getId())
                .school_id(entity.getSchoolId())
                .date(entity.getDate())
                .meal_type(entity.getMealType())
                .amount_kg(entity.getAmountKg())
                .build();
    }


    // =================================================================================
    // 3. 만족도 (Satisfaction) 로직 - 조회 및 일일 분석
    // =================================================================================

    public SatisfactionDto.CountResponse getSatisfactionCount(Long schoolId, int days) {
        LocalDateTime end = LocalDateTime.now();
        LocalDateTime start = end.minusDays(days);

        long total = reviewAnalysisRepository.countBySchoolIdAndDateRange(schoolId, start, end);
        long positive = reviewAnalysisRepository.countBySchoolIdAndLabelAndDateRange(schoolId, "POSITIVE", start, end);
        long negative = reviewAnalysisRepository.countBySchoolIdAndLabelAndDateRange(schoolId, "NEGATIVE", start, end);

        long neutral = total - positive - negative;

        return SatisfactionDto.CountResponse.builder()
                .period(SatisfactionDto.Period.builder().start_date(start.toLocalDate()).end_date(end.toLocalDate()).build())
                .school_id(schoolId)
                .total_count(total)
                .positive_count(positive)
                .negative_count(negative)
                .neutral_count(neutral)
                .build();
    }

    // [수정] RestClient를 사용하여 FastAPI 일일 분석 요청
    @Transactional
    public void executeDailySatisfactionAnalysis(Long schoolId, LocalDate targetDate) {
        log.info("일일 만족도 분석 시작 - School: {}, Date: {}", schoolId, targetDate);

        List<Map<String, Object>> analysisResults;
        try {
            // [변경] RestClient 동기 요청 방식 (깔끔하고 직관적임)
            analysisResults = restClient.post()
                    .uri("/analyze/daily") // BaseURL은 Config에 설정됨 (http://localhost:8001)
                    .body(Map.of(
                            "school_id", schoolId,
                            "date", targetDate.toString()
                    ))
                    .retrieve()
                    // List<Map> 타입으로 안전하게 변환
                    .body(new ParameterizedTypeReference<List<Map<String, Object>>>() {});

        } catch (Exception e) {
            log.error("FastAPI 분석 요청 실패", e);
            throw new RuntimeException("AI 분석 서버 오류");
        }

        if (analysisResults == null || analysisResults.isEmpty()) {
            log.info("해당 날짜에 분석된 리뷰 데이터가 없습니다.");
            return;
        }

        // 결과 저장 로직 (기존 동일)
        List<ReviewAnalysis> entities = analysisResults.stream()
                .map(result -> {
                    String label = (String) result.get("sentiment_label");
                    Double score = (Double) result.get("sentiment_score");

                    String aspectTags = null;
                    String evidence = null;
                    try {
                        if (result.get("aspect_tags") != null) {
                            aspectTags = objectMapper.writeValueAsString(result.get("aspect_tags"));
                        }
                        if (result.get("evidence_phrases") != null) {
                            evidence = objectMapper.writeValueAsString(result.get("evidence_phrases"));
                        }
                    } catch (Exception e) {
                        log.warn("JSON 변환 중 경고: {}", e.getMessage());
                    }

                    return ReviewAnalysis.builder()
                            .schoolId(schoolId)
                            .targetYm(targetDate.toString())
                            .sentimentLabel(label)
                            .sentimentScore(score != null ? score.floatValue() : 0.0f)
                            .sentimentConf(0.0f)
                            .aspectTags(aspectTags)
                            .evidencePhrases(evidence)
                            .issueFlags(false)
                            .createdAt(LocalDateTime.now())
                            .build();
                })
                .collect(Collectors.toList());

        reviewAnalysisRepository.saveAll(entities);
        log.info("{}건의 리뷰 분석 데이터 저장 완료", entities.size());
    }
}