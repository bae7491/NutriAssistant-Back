package com.nutriassistant.nutriassistant_back.domain.metrics.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nutriassistant.nutriassistant_back.domain.metrics.dto.LeftoverDto;
import com.nutriassistant.nutriassistant_back.domain.metrics.dto.SatisfactionDto;
import com.nutriassistant.nutriassistant_back.domain.metrics.dto.SkipMealDto;
import com.nutriassistant.nutriassistant_back.domain.metrics.entity.Leftover;
import com.nutriassistant.nutriassistant_back.domain.metrics.entity.SkipMeal;
import com.nutriassistant.nutriassistant_back.domain.metrics.repository.LeftoverRepository;
import com.nutriassistant.nutriassistant_back.domain.metrics.repository.SkipMealRepository;

// [리뷰 & 분석 관련 Import]
import com.nutriassistant.nutriassistant_back.domain.reviewanalysis.entity.ReviewAnalysis;
import com.nutriassistant.nutriassistant_back.domain.reviewanalysis.repository.ReviewAnalysisRepository;
import com.nutriassistant.nutriassistant_back.domain.reviewanalysis.dto.FastApiDto;
import com.nutriassistant.nutriassistant_back.domain.review.repository.ReviewRepository;
import com.nutriassistant.nutriassistant_back.domain.review.entity.Review;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MetricsService {

    private final SkipMealRepository skipMealRepository;
    private final LeftoverRepository leftoverRepository;

    // [추가] 분석된 데이터(통계) 접근용
    private final ReviewAnalysisRepository reviewAnalysisRepository;

    // [추가] 원본 리뷰 데이터 접근용
    private final ReviewRepository reviewRepository;

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    // =================================================================================
    // 1. Skip Meal Logic (결식률 로직)
    // =================================================================================

    // 결식 데이터 등록
    @Transactional
    public SkipMealDto.Response registerSkipMeal(SkipMealDto.RegisterRequest request) {
        if (skipMealRepository.existsBySchoolIdAndDateAndMealType(
                request.getSchool_id(), request.getDate(), request.getMeal_type())) {
            throw new IllegalArgumentException("해당 날짜에 이미 결식 데이터가 존재합니다.");
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

    // 결식 데이터 수정
    @Transactional
    public SkipMealDto.Response updateSkipMeal(SkipMealDto.UpdateRequest request) {
        // 1. 학교ID + 날짜 + 식사타입으로 기존 데이터를 찾습니다.
        SkipMeal skipMeal = skipMealRepository.findBySchoolIdAndDateAndMealType(
                        request.getSchool_id(), request.getDate(), request.getMeal_type())
                .orElseThrow(() -> new IllegalArgumentException("해당 날짜의 데이터를 찾을 수 없습니다. (등록 먼저 해주세요)"));

        // 2. 찾은 데이터를 업데이트합니다. (JPA 변경 감지 활용)
        // Builder로 새로 만드는 대신, 엔티티에 update 메서드를 만드는 게 더 객체지향적이지만,
        // 여기서는 기존 방식대로 Setter나 Builder 패턴을 활용해 값을 덮어씁니다.

        // *팁: JPA 영속성 컨텍스트 때문에 값을 바꾸기만 해도 트랜잭션 종료 시 자동 저장됩니다.
        // 하지만 명시적으로 save를 호출해서 DTO 변환을 합니다.
        SkipMeal updated = SkipMeal.builder()
                .id(skipMeal.getId()) // 기존 PK 유지
                .schoolId(skipMeal.getSchoolId())
                .date(skipMeal.getDate())
                .mealType(skipMeal.getMealType())
                .createdAt(skipMeal.getCreatedAt())
                .skippedCount(request.getSkipped_count())   // 변경된 값
                .totalStudents(request.getTotal_students()) // 변경된 값
                .build();

        return mapToSkipMealResponse(skipMealRepository.save(updated));
    }

    // 일별 결식 조회
    public SkipMealDto.Response getDailySkipMeal(Long schoolId, String mealType, LocalDate date) {
        SkipMeal skipMeal = skipMealRepository.findBySchoolIdAndDateAndMealType(schoolId, date, mealType)
                .orElse(null);

        if (skipMeal == null) return null;
        return mapToSkipMealResponse(skipMeal);
    }

    // 기간별 결식 통계
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
    // 2. Leftover Logic (잔반률 로직)
    // =================================================================================

    // 잔반 데이터 등록
    @Transactional
    public LeftoverDto.Response registerLeftover(LeftoverDto.RegisterRequest request) {
        if (leftoverRepository.existsBySchoolIdAndDateAndMealType(
                request.getSchool_id(), request.getDate(), request.getMeal_type())) {
            throw new IllegalArgumentException("해당 날짜에 이미 잔반 데이터가 존재합니다.");
        }

        Leftover leftover = Leftover.builder()
                .schoolId(request.getSchool_id())
                .date(request.getDate())
                .mealType(request.getMeal_type())
                .amountKg(request.getAmount_kg())
                .build();

        return mapToLeftoverResponse(leftoverRepository.save(leftover));
    }

    // 잔반 데이터 수정
    @Transactional
    public LeftoverDto.Response updateLeftover(LeftoverDto.UpdateRequest request) {
        // 1. 학교ID + 날짜 + 식사타입으로 조회
        Leftover leftover = leftoverRepository.findBySchoolIdAndDateAndMealType(
                        request.getSchool_id(), request.getDate(), request.getMeal_type())
                .orElseThrow(() -> new IllegalArgumentException("해당 날짜의 데이터를 찾을 수 없습니다."));

        // 2. 값 업데이트 (Double -> Float 변환)
        leftover.update(request.getAmount_kg().floatValue());
        // 없다면 아래처럼 save
        /* leftover.setAmountKg(request.getAmount_kg());
        leftoverRepository.save(leftover)
        */

        return mapToLeftoverResponse(leftover);
    }

    // 일별 잔반 조회
    public LeftoverDto.Response getDailyLeftover(Long schoolId, String mealType, LocalDate date) {
        Leftover leftover = leftoverRepository.findBySchoolIdAndDateAndMealType(schoolId, date, mealType)
                .orElse(null);

        if (leftover == null) return null;
        return mapToLeftoverResponse(leftover);
    }

    // 기간별 잔반 통계
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
    // 3. Satisfaction Logic (만족도 분석 로직)
    // =================================================================================

    // 3-1. 만족도 카운트 요약 (최근 N일)
    // ReviewAnalysis 테이블에서 긍정/부정 횟수를 합산합니다.
    public SatisfactionDto.CountResponse getSatisfactionCount(Long schoolId, int days) {
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(days);

        List<ReviewAnalysis> list = getAnalysisListByPeriod(schoolId, start, end);

        long pos = list.stream().mapToLong(a -> a.getPositiveCount() == null ? 0 : a.getPositiveCount()).sum();
        long neg = list.stream().mapToLong(a -> a.getNegativeCount() == null ? 0 : a.getNegativeCount()).sum();
        long total = pos + neg;

        return SatisfactionDto.CountResponse.builder()
                .period(SatisfactionDto.Period.builder().start_date(start).end_date(end).build())
                .school_id(schoolId)
                .total_count(total)
                .positive_count(pos)
                .negative_count(neg)
                .neutral_count(0L) // 중립은 현재 로직에서 사용 안 함
                .build();
    }

    // 3-2. 분석 배포 목록 (최근 N일, 페이징 포함)
    // 일별 분석 결과(ReviewAnalysis)를 리스트로 반환합니다.
    public SatisfactionDto.BatchListResponse getSatisfactionBatchList(Long schoolId, int days, int page, int size) {
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(days);

        // 실제로는 Repository에서 Page로 가져오는 게 좋지만, 로직 유지를 위해 리스트 조회 후 메모리 페이징 처리
        List<ReviewAnalysis> list = getAnalysisListByPeriod(schoolId, start, end);

        int totalItems = list.size();
        int totalPages = (int) Math.ceil((double) totalItems / size);
        int fromIndex = (page - 1) * size;
        int toIndex = Math.min(fromIndex + size, totalItems);

        List<ReviewAnalysis> pagedList = Collections.emptyList();
        if (fromIndex < totalItems && fromIndex >= 0) {
            pagedList = list.subList(fromIndex, toIndex);
        }

        List<SatisfactionDto.BatchInfo> batches = pagedList.stream()
                .map(a -> SatisfactionDto.BatchInfo.builder()
                        .batch_id("batch-" + a.getId()) // ID를 배치 ID로 활용
                        .date(LocalDate.parse(a.getTargetYm()))
                        .generated_at(a.getCreatedAt())
                        .model_version("sent-v1.2.0-lightml") // 고정값 예시
                        .total_reviews((int) (safeLong(Long.valueOf(a.getPositiveCount())) + safeLong(Long.valueOf(a.getNegativeCount()))))
                        .positive_count(Math.toIntExact(safeLong(Long.valueOf(a.getPositiveCount()))))
                        .negative_count(Math.toIntExact(safeLong(Long.valueOf(a.getNegativeCount()))))
                        .average_rating(4.2) // 평점은 별도 집계 필요 (현재는 더미 값)
                        .build())
                .collect(Collectors.toList());

        return SatisfactionDto.BatchListResponse.builder()
                .period(SatisfactionDto.Period.builder().start_date(start).end_date(end).build())
                .school_id(schoolId)
                .batches(batches)
                .pagination(SatisfactionDto.Pagination.builder()
                        .current_page(page)
                        .total_pages(totalPages)
                        .total_items((long) totalItems)
                        .page_size(size)
                        .build())
                .build();
    }

    // 3-3 & 3-4. 감성별 카운트 (긍정/부정)
    public SatisfactionDto.LabelCountResponse getSentimentCount(Long schoolId, String sentiment, LocalDate start, LocalDate end) {
        List<ReviewAnalysis> list = getAnalysisListByPeriod(schoolId, start, end);
        long count = 0;

        if ("POSITIVE".equalsIgnoreCase(sentiment)) {
            count = list.stream().mapToLong(a -> safeLong(Long.valueOf(a.getPositiveCount()))).sum();
        } else {
            count = list.stream().mapToLong(a -> safeLong(Long.valueOf(a.getNegativeCount()))).sum();
        }

        return SatisfactionDto.LabelCountResponse.builder()
                .school_id(schoolId)
                .sentiment_label(sentiment)
                .count(count)
                .period(SatisfactionDto.Period.builder().start_date(start).end_date(end).build())
                .build();
    }

    // 3-5. 리뷰 목록 상세 조회 (페이징) [404 에러 해결 핵심]
    // Review 테이블에서 실제 리뷰 텍스트를 페이징하여 가져옵니다.
    public SatisfactionDto.ReviewListResponse getSatisfactionReviews(Long schoolId, String batchId, LocalDate start, LocalDate end, String sentiment, int page, int size) {

        // 1. Pageable 객체 생성 (0부터 시작하므로 page-1)
        Pageable pageable = PageRequest.of(Math.max(0, page - 1), size, Sort.by("createdAt").descending());

        // 2. DB 조회 (ReviewRepository에 findBySchoolId 메서드가 있어야 함)
        Page<Review> reviewPage = reviewRepository.findBySchoolId(schoolId, pageable);

        // 3. DTO 매핑
        List<SatisfactionDto.ReviewDetail> details = reviewPage.getContent().stream()
                .map(r -> SatisfactionDto.ReviewDetail.builder()
                        .review_id("R-" + r.getId())
                        .batch_id(batchId != null ? batchId : "batch-latest")
                        .school_id(r.getSchoolId())
                        .meal_type("LUNCH")
                        .date(r.getCreatedAt().toLocalDate())
                        .rating_5(r.getRating() != null ? r.getRating().doubleValue() : 0.0)

                        // 현재 Review 엔티티에 감성분석 결과 컬럼이 없으므로, 기본값 또는 추후 조인 필요
                        // 일단 원본 텍스트(content)를 evidence로 보여주는 것이 중요
                        .sentiment_label("POSITIVE")
                        .sentiment_score(0.85)
                        .aspect_tags(Arrays.asList("맛", "양"))
                        .evidence_phrases(Collections.singletonList(r.getContent())) // 실제 리뷰 내용
                        .issue_flags(new ArrayList<>())
                        .build())
                .collect(Collectors.toList());

        // 4. 응답 생성
        return SatisfactionDto.ReviewListResponse.builder()
                .reviews(details)
                .pagination(SatisfactionDto.Pagination.builder()
                        .current_page(page)
                        .total_pages(reviewPage.getTotalPages())
                        .total_items(reviewPage.getTotalElements())
                        .page_size(size)
                        .build())
                .build();
    }

    // [Helper] 일간 만족도 분석 실행 (FastAPI 호출)
    @Transactional
    public void executeDailySatisfactionAnalysis(Long schoolId, LocalDate targetDate) {
        log.info("일간 만족도 분석 시작 - 학교ID: {}, 날짜: {}", schoolId, targetDate);

        // 1. 해당 날짜의 리뷰 데이터 조회
        LocalDateTime startOfDay = targetDate.atStartOfDay();
        LocalDateTime endOfDay = targetDate.plusDays(1).atStartOfDay();

        List<Review> reviews = reviewRepository.findBySchoolIdAndCreatedAtBetween(schoolId, startOfDay, endOfDay);

        if (reviews.isEmpty()) {
            log.info("분석할 리뷰가 없습니다.");
            return;
        }

        List<String> texts = reviews.stream()
                .map(Review::getContent)
                .collect(Collectors.toList());

        // 2. FastAPI 요청 DTO 생성
        FastApiDto.Request requestDto = FastApiDto.Request.builder()
                .schoolId(schoolId)
                .targetDate(targetDate.toString())
                .reviewTexts(texts)
                .build();

        try {
            // 3. FastAPI 호출 (URL은 application.yml이나 RestClient 설정에 따름)
            // 안전을 위해 전체 URL 명시 권장 (localhost:8001)
            FastApiDto.Response response = restClient.post()
                    .uri("http://localhost:8001/api/analyze/daily")
                    .body(requestDto)
                    .retrieve()
                    .body(FastApiDto.Response.class);

            if (response == null) {
                log.error("FastAPI 응답이 비어있습니다.");
                return;
            }

            // 4. 분석 결과 DB 저장 (ReviewAnalysis)
            ReviewAnalysis analysis = ReviewAnalysis.builder()
                    .schoolId(schoolId)
                    .targetYm(targetDate.toString())
                    .sentimentLabel(response.getSentimentLabel())
                    .sentimentScore(response.getSentimentScore())
                    .sentimentConf(response.getSentimentConf())
                    .positiveCount(response.getPositiveCount())
                    .negativeCount(response.getNegativeCount())
                    .aspectTags(String.join(",", response.getAspectTags()))
                    .evidencePhrases(String.join("|", response.getEvidencePhrases()))
                    .issueFlags(response.getIssueFlags())
                    .build();

            reviewAnalysisRepository.save(analysis);
            log.info("분석 완료 및 저장 성공. 긍정: {}, 부정: {}", response.getPositiveCount(), response.getNegativeCount());

        } catch (Exception e) {
            log.error("FastAPI 분석 요청 실패: {}", e.getMessage());
        }
    }

    // [Helper] 기간별 분석 데이터 조회
    private List<ReviewAnalysis> getAnalysisListByPeriod(Long schoolId, LocalDate start, LocalDate end) {
        // 성능 최적화를 위해 Repository에서 Between으로 가져오는 것이 좋으나, 현재 구조 유지를 위해 Stream 필터링 사용
        List<ReviewAnalysis> all = reviewAnalysisRepository.findBySchoolId(schoolId);
        String s = start.toString();
        String e = end.toString();

        return all.stream()
                .filter(a -> a.getTargetYm().compareTo(s) >= 0 && a.getTargetYm().compareTo(e) <= 0)
                .collect(Collectors.toList());
    }

    // [Helper] Null-safe Long 변환
    private long safeLong(Long val) {
        return val == null ? 0L : val;
    }
}