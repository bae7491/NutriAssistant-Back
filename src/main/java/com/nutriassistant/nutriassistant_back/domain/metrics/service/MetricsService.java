package com.nutriassistant.nutriassistant_back.domain.metrics.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nutriassistant.nutriassistant_back.domain.metrics.dto.LeftoverDto;
import com.nutriassistant.nutriassistant_back.domain.metrics.dto.SatisfactionDto;
import com.nutriassistant.nutriassistant_back.domain.metrics.dto.SkipMealDto;
import com.nutriassistant.nutriassistant_back.domain.metrics.entity.Leftover;
import com.nutriassistant.nutriassistant_back.domain.metrics.entity.SkipMeal;
import com.nutriassistant.nutriassistant_back.domain.metrics.repository.LeftoverRepository;
import com.nutriassistant.nutriassistant_back.domain.metrics.repository.SkipMealRepository;

// [Update] Imports for review analysis entities and repositories from the new package 'reviewanalysis'
import com.nutriassistant.nutriassistant_back.domain.reviewanalysis.entity.ReviewAnalysis;
import com.nutriassistant.nutriassistant_back.domain.reviewanalysis.repository.ReviewAnalysisRepository;
import com.nutriassistant.nutriassistant_back.domain.reviewanalysis.dto.FastApiDto;

// [Add] Imports for fetching raw review data from the 'review' domain
import com.nutriassistant.nutriassistant_back.domain.review.repository.ReviewRepository;
import com.nutriassistant.nutriassistant_back.domain.review.entity.Review;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;          // Added for pagination
import org.springframework.data.domain.PageRequest;   // Added for creating page requests
import org.springframework.data.domain.Pageable;      // Added for pagination interface
import org.springframework.data.domain.Sort;          // Added for sorting
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList; // Added
import java.util.Arrays; // Added
import java.util.Collections; // Added
import java.util.List;
import java.util.stream.Collectors;


@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MetricsService {

    private final SkipMealRepository skipMealRepository;
    private final LeftoverRepository leftoverRepository;

    // [Update] Inject repository for accessing analyzed review data (statistics)
    private final ReviewAnalysisRepository reviewAnalysisRepository;

    // [Add] Inject repository for accessing raw review data (lists)
    private final ReviewRepository reviewRepository;

    private final RestClient restClient;
    private final ObjectMapper objectMapper;


    // =================================================================================
    // 1. Skip Meal Logic (결식률 로직)
    // =================================================================================

    // Registers daily skip meal data. Throws exception if data already exists for the date.
    @Transactional
    public SkipMealDto.Response registerSkipMeal(SkipMealDto.RegisterRequest request) {
        if (skipMealRepository.existsBySchoolIdAndDateAndMealType(
                request.getSchool_id(), request.getDate(), request.getMeal_type())) {
            throw new IllegalArgumentException("Skip meal data already exists for this date.");
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

    // Updates existing skip meal data.
    @Transactional
    public SkipMealDto.Response updateSkipMeal(SkipMealDto.UpdateRequest request) {
        SkipMeal skipMeal = skipMealRepository.findById(request.getId())
                .orElseThrow(() -> new IllegalArgumentException("Data not found."));

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

    // Retrieves skip meal data for a specific day.
    public SkipMealDto.Response getDailySkipMeal(Long schoolId, String mealType, LocalDate date) {
        SkipMeal skipMeal = skipMealRepository.findBySchoolIdAndDateAndMealType(schoolId, date, mealType)
                .orElse(null);

        if (skipMeal == null) return null;
        return mapToSkipMealResponse(skipMeal);
    }

    // Retrieves skip meal statistics for a specific period (e.g., last 7 days, last 30 days).
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

    // Helper method to convert SkipMeal entity to DTO.
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

    // Registers daily leftover data.
    @Transactional
    public LeftoverDto.Response registerLeftover(LeftoverDto.RegisterRequest request) {
        if (leftoverRepository.existsBySchoolIdAndDateAndMealType(
                request.getSchool_id(), request.getDate(), request.getMeal_type())) {
            throw new IllegalArgumentException("Leftover data already exists for this date.");
        }

        Leftover leftover = Leftover.builder()
                .schoolId(request.getSchool_id())
                .date(request.getDate())
                .mealType(request.getMeal_type())
                .amountKg(request.getAmount_kg())
                .build();

        return mapToLeftoverResponse(leftoverRepository.save(leftover));
    }

    // Updates existing leftover data.
    @Transactional
    public LeftoverDto.Response updateLeftover(LeftoverDto.UpdateRequest request) {
        Leftover leftover = leftoverRepository.findById(request.getId())
                .orElseThrow(() -> new IllegalArgumentException("Data not found."));

        leftover.update(request.getAmount_kg());

        return mapToLeftoverResponse(leftover);
    }

    // Retrieves leftover data for a specific day.
    public LeftoverDto.Response getDailyLeftover(Long schoolId, String mealType, LocalDate date) {
        Leftover leftover = leftoverRepository.findBySchoolIdAndDateAndMealType(schoolId, date, mealType)
                .orElse(null);

        if (leftover == null) return null;
        return mapToLeftoverResponse(leftover);
    }

    // Retrieves leftover statistics for a specific period.
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

    // Helper method to convert Leftover entity to DTO.
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
    // 3. Satisfaction Logic (만족도 로직) - Updated for 5 endpoints
    // =================================================================================

    // 3-1. Satisfaction Count Summary (Last 30 days)
    // Aggregates positive/negative counts from daily analysis data (ReviewAnalysis).
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
                .neutral_count(0L) // Neutral sentiment is not currently used
                .build();
    }

    // 3-2. Satisfaction Batch List (Last 30 days)
    // Returns a list of daily analysis summaries (batches).
    public SatisfactionDto.BatchListResponse getSatisfactionBatchList(Long schoolId, int days, int page, int size) {
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(days);

        List<ReviewAnalysis> list = getAnalysisListByPeriod(schoolId, start, end);

        // In-memory pagination (since ReviewAnalysis might not support complex DB filtering easily)
        int totalItems = list.size();
        int totalPages = (int) Math.ceil((double) totalItems / size);
        int fromIndex = (page - 1) * size;
        int toIndex = Math.min(fromIndex + size, totalItems);

        List<ReviewAnalysis> pagedList = Collections.emptyList();
        if (fromIndex < totalItems) {
            pagedList = list.subList(fromIndex, toIndex);
        }

        List<SatisfactionDto.BatchInfo> batches = pagedList.stream()
                .map(a -> SatisfactionDto.BatchInfo.builder()
                        .batch_id("batch-" + a.getId())
                        .date(LocalDate.parse(a.getTargetYm()))
                        .generated_at(a.getCreatedAt())
                        .model_version("sent-v1.2.0-lightml") // Example value
                        .total_reviews((a.getPositiveCount() == null ? 0 : a.getPositiveCount()) + (a.getNegativeCount() == null ? 0 : a.getNegativeCount()))
                        .positive_count(a.getPositiveCount())
                        .negative_count(a.getNegativeCount())
                        .average_rating(4.2) // Placeholder value
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

    // 3-3 & 3-4. Sentiment Count (Positive/Negative)
    // Counts reviews with a specific sentiment (POSITIVE or NEGATIVE) within a period.
    public SatisfactionDto.LabelCountResponse getSentimentCount(Long schoolId, String sentiment, LocalDate start, LocalDate end) {
        List<ReviewAnalysis> list = getAnalysisListByPeriod(schoolId, start, end);
        long count = 0;

        if ("POSITIVE".equalsIgnoreCase(sentiment)) {
            count = list.stream().mapToLong(a -> a.getPositiveCount() == null ? 0 : a.getPositiveCount()).sum();
        } else {
            count = list.stream().mapToLong(a -> a.getNegativeCount() == null ? 0 : a.getNegativeCount()).sum();
        }

        return SatisfactionDto.LabelCountResponse.builder()
                .school_id(schoolId)
                .sentiment_label(sentiment)
                .count(count)
                .period(SatisfactionDto.Period.builder().start_date(start).end_date(end).build())
                .build();
    }

    // 3-5. Satisfaction Review List (with Pagination)
    // Retrieves a paginated list of individual reviews. Resolves 404 error.
    public SatisfactionDto.ReviewListResponse getSatisfactionReviews(Long schoolId, String batchId, LocalDate start, LocalDate end, String sentiment, int page, int size) {
        // Create pagination object (0-indexed page, sorted by creation date descending)
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by("createdAt").descending());

        // Fetch paginated reviews from DB
        Page<Review> reviewPage = reviewRepository.findBySchoolId(schoolId, pageable);

        // Map Review entities to DTOs
        List<SatisfactionDto.ReviewDetail> details = reviewPage.getContent().stream()
                .map(r -> SatisfactionDto.ReviewDetail.builder()
                        .review_id("R-" + r.getId())
                        .batch_id(batchId != null ? batchId : "batch-unknown")
                        .school_id(r.getSchoolId())
                        .meal_type("LUNCH")
                        .date(r.getCreatedAt().toLocalDate())
                        .rating_5(r.getRating().doubleValue())
                        .sentiment_label("POSITIVE") // Placeholder, integrate actual sentiment later
                        .sentiment_score(0.752)
                        .aspect_tags(Arrays.asList("Taste", "Rice")) // Placeholder tags
                        .evidence_phrases(Arrays.asList(r.getContent()))
                        .issue_flags(new ArrayList<>())
                        .build())
                .collect(Collectors.toList());

        // Create pagination metadata
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

    // Helper method to execute daily satisfaction analysis (calls FastAPI)
    @Transactional
    public void executeDailySatisfactionAnalysis(Long schoolId, LocalDate targetDate) {
        log.info("Starting daily satisfaction analysis - School: {}, Date: {}", schoolId, targetDate);

        LocalDateTime startOfDay = targetDate.atStartOfDay();
        LocalDateTime endOfDay = targetDate.plusDays(1).atStartOfDay();

        List<Review> reviews = reviewRepository.findBySchoolIdAndCreatedAtBetween(schoolId, startOfDay, endOfDay);

        if (reviews.isEmpty()) {
            log.info("No reviews to analyze.");
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
            FastApiDto.Response response = restClient.post()
                    .uri("/api/analyze/daily")
                    .body(requestDto)
                    .retrieve()
                    .body(FastApiDto.Response.class);

            if (response == null) {
                log.error("FastAPI response is empty.");
                return;
            }

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
            log.info("Daily analysis saved. Positive: {}, Negative: {}", response.getPositiveCount(), response.getNegativeCount());

        } catch (Exception e) {
            log.error("FastAPI analysis request failed: {}", e.getMessage());
        }
    }

    // Helper method to retrieve analysis data for a period
    private List<ReviewAnalysis> getAnalysisListByPeriod(Long schoolId, LocalDate start, LocalDate end) {
        List<ReviewAnalysis> all = reviewAnalysisRepository.findBySchoolId(schoolId);
        String s = start.toString();
        String e = end.toString();
        return all.stream()
                .filter(a -> a.getTargetYm().compareTo(s) >= 0 && a.getTargetYm().compareTo(e) <= 0)
                .collect(Collectors.toList());
    }
}