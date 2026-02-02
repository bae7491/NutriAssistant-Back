package com.nutriassistant.nutriassistant_back.domain.metrics.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nutriassistant.nutriassistant_back.domain.metrics.dto.LeftoverDto;
import com.nutriassistant.nutriassistant_back.domain.metrics.dto.SatisfactionDto;
import com.nutriassistant.nutriassistant_back.domain.metrics.dto.SkipMealDto;
import com.nutriassistant.nutriassistant_back.domain.metrics.entity.Leftover;
import com.nutriassistant.nutriassistant_back.domain.metrics.entity.SkipMeal;
import com.nutriassistant.nutriassistant_back.domain.metrics.repository.LeftoverRepository;
import com.nutriassistant.nutriassistant_back.domain.metrics.repository.SkipMealRepository;

// [수정 1] 새로 만든 패키지의 Entity, Repository, DTO import
import com.nutriassistant.nutriassistant_back.domain.reviewanalysis.entity.ReviewAnalysis;
import com.nutriassistant.nutriassistant_back.domain.reviewanalysis.repository.ReviewAnalysisRepository;
import com.nutriassistant.nutriassistant_back.domain.reviewanalysis.dto.FastApiDto;

// [추가] 리뷰 원본을 가져오기 위해 필요
import com.nutriassistant.nutriassistant_back.domain.review.repository.ReviewRepository;
import com.nutriassistant.nutriassistant_back.domain.review.entity.Review;

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
public class MetricsService {

    private final SkipMealRepository skipMealRepository;
    private final LeftoverRepository leftoverRepository;

    // [수정] 새로 만든 Repository 사용
    private final ReviewAnalysisRepository reviewAnalysisRepository;

    // [추가] 분석할 리뷰 원본 데이터를 가져오기 위해 필요
    private final ReviewRepository reviewRepository;

    private final RestClient restClient;
    private final ObjectMapper objectMapper;


    // =================================================================================
    // 1. 결식률 (Skip Meal) 로직
    // =================================================================================
    // (기존 코드 유지)
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
    // (기존 코드 유지)
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
    // 3. 만족도 (Satisfaction) 로직 - [전면 수정]
    // =================================================================================

    // [수정] DB 구조가 '일일 요약(Counts)' 저장 방식으로 바뀌었으므로,
    // 기간 조회 시 각 날짜의 positiveCount, negativeCount를 합산해야 합니다.
    public SatisfactionDto.CountResponse getSatisfactionCount(Long schoolId, int days) {
        LocalDateTime end = LocalDateTime.now();
        LocalDateTime start = end.minusDays(days);

        // TODO: Repository에 날짜 범위로 List<ReviewAnalysis>를 가져오는 메서드가 필요합니다.
        // 현재는 임시로 findAll을 쓰거나, Repository에 'findBySchoolIdAndTargetYmBetween' 같은 메서드를 만들어야 정확합니다.
        // 여기서는 기존 Repository 코드를 최대한 활용하여 구현합니다.

        // 1. 해당 학교의 전체 데이터를 가져와서 기간 필터링 (Repository 메서드가 부족할 경우의 차선책)
        // (가장 좋은 건 Repository에 findBySchoolIdAndTargetYmBetween을 만드는 것입니다)
        List<ReviewAnalysis> analysisList = reviewAnalysisRepository.findBySchoolId(schoolId);

        long positive = 0;
        long negative = 0;

        // 문자열 날짜 비교를 위해 변환
        String startYm = start.toLocalDate().toString();
        String endYm = end.toLocalDate().toString();

        for (ReviewAnalysis analysis : analysisList) {
            String targetYm = analysis.getTargetYm();
            // 날짜 범위 체크 (문자열 비교)
            if (targetYm.compareTo(startYm) >= 0 && targetYm.compareTo(endYm) <= 0) {
                // Null 체크 후 합산
                positive += (analysis.getPositiveCount() != null) ? analysis.getPositiveCount() : 0;
                negative += (analysis.getNegativeCount() != null) ? analysis.getNegativeCount() : 0;
            }
        }

        long total = positive + negative;

        return SatisfactionDto.CountResponse.builder()
                .period(SatisfactionDto.Period.builder().start_date(start.toLocalDate()).end_date(end.toLocalDate()).build())
                .school_id(schoolId)
                .total_count(total)
                .positive_count(positive)
                .negative_count(negative)
                .neutral_count(0L) // 현재 구조상 중립은 계산하지 않음 (필요 시 로직 추가)
                .build();
    }

    // [수정] 일일 분석 실행 로직 (FastAPI 연동 핵심)
    @Transactional
    public void executeDailySatisfactionAnalysis(Long schoolId, LocalDate targetDate) {
        log.info("일일 만족도 분석 시작 - School: {}, Date: {}", schoolId, targetDate);

        // 1. 분석할 리뷰 데이터 조회 (ReviewRepository 필요)
        LocalDateTime startOfDay = targetDate.atStartOfDay();
        LocalDateTime endOfDay = targetDate.plusDays(1).atStartOfDay();

        List<Review> reviews = reviewRepository.findBySchoolIdAndCreatedAtBetween(schoolId, startOfDay, endOfDay);

        if (reviews.isEmpty()) {
            log.info("분석할 리뷰가 없습니다.");
            return;
        }

        // 2. FastAPI 요청 데이터 생성
        List<String> texts = reviews.stream()
                .map(Review::getContent)
                .collect(Collectors.toList());

        FastApiDto.Request requestDto = FastApiDto.Request.builder()
                .schoolId(schoolId)
                .targetDate(targetDate.toString())
                .reviewTexts(texts)
                .build();

        try {
            // 3. FastAPI 호출 (RestClient 사용)
            FastApiDto.Response response = restClient.post()
                    .uri("/api/analyze/daily") // [주의] FastAPI 엔드포인트 경로 일치 확인
                    .body(requestDto)
                    .retrieve()
                    .body(FastApiDto.Response.class);

            if (response == null) {
                log.error("FastAPI 응답이 비어있습니다.");
                return;
            }

            // 4. 결과 저장 (1건의 요약 데이터로 저장)
            ReviewAnalysis analysis = ReviewAnalysis.builder()
                    .schoolId(schoolId)
                    .targetYm(targetDate.toString())
                    .sentimentLabel(response.getSentimentLabel())
                    .sentimentScore(response.getSentimentScore())
                    .sentimentConf(response.getSentimentConf())
                    // [중요] 개수 저장
                    .positiveCount(response.getPositiveCount())
                    .negativeCount(response.getNegativeCount())
                    // 리스트 -> 문자열 변환
                    .aspectTags(String.join(",", response.getAspectTags()))
                    .evidencePhrases(String.join("|", response.getEvidencePhrases()))
                    .issueFlags(response.getIssueFlags())
                    .build();

            // saveAll()이 아니라 save() 사용 (하루치 1건 저장)
            reviewAnalysisRepository.save(analysis);
            log.info("일일 분석 결과 저장 완료. 긍정: {}, 부정: {}", response.getPositiveCount(), response.getNegativeCount());

        } catch (Exception e) {
            log.error("FastAPI 분석 요청 실패: {}", e.getMessage());
            // 필요한 경우 예외를 다시 던지거나, 실패 상태를 DB에 기록
        }
    }
}