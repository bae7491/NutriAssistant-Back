package com.nutriassistant.nutriassistant_back.MealPlan.controller;

import com.nutriassistant.nutriassistant_back.MealPlan.DTO.*;
import com.nutriassistant.nutriassistant_back.MealPlan.entity.MealPlan;
import com.nutriassistant.nutriassistant_back.MealPlan.entity.MealPlanMenu;
import com.nutriassistant.nutriassistant_back.MealPlan.entity.MealType;
import com.nutriassistant.nutriassistant_back.MealPlan.service.MealPlanService;
import com.nutriassistant.nutriassistant_back.global.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.util.net.openssl.ciphers.Authentication;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/mealplan")
public class MealPlanController {

    private final MealPlanService mealPlanService;

    public MealPlanController(MealPlanService mealPlanService) {
        this.mealPlanService = mealPlanService;
    }

    /**
     * 월간 식단표 생성
     */
    @PostMapping
    public ResponseEntity<ApiResponse<List<MealPlanGenerateResponse>>> generateMealPlan(
            @Validated @RequestBody MealPlanGenerateRequest request,
            Authentication authentication
    ) {
        try {
            // TODO: JWT에서 schoolId 추출
            Long schoolId = extractSchoolIdFromAuth(authentication);

            log.info("🎯 식단 생성 API 호출: 학교 ID={}, 연도={}, 월={}",
                    schoolId, request.getYear(), request.getMonth());

            MealPlan mealPlan = mealPlanService.generateAndSave(schoolId, request);
            List<MealPlanGenerateResponse> responseData = mealPlanService.toResponseList(mealPlan);

            return ResponseEntity.ok(
                    ApiResponse.success("월간 식단표 생성 성공.", responseData)
            );

        } catch (IllegalArgumentException e) {
            log.error("❌ 잘못된 요청값: ", e);
            return ResponseEntity.badRequest().body(
                    ApiResponse.error("요청값이 올바르지 않습니다.")
            );

        } catch (HttpClientErrorException e) {
            log.error("❌ FastAPI 클라이언트 오류 (4xx): ", e);
            return ResponseEntity.status(e.getStatusCode()).body(
                    ApiResponse.error("외부 서비스 요청이 실패했습니다: " + e.getMessage())
            );

        } catch (HttpServerErrorException e) {
            log.error("❌ FastAPI 서버 오류 (5xx): ", e);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(
                    ApiResponse.error("외부 서비스에서 오류가 발생했습니다.")
            );

        } catch (ResourceAccessException e) {
            log.error("❌ FastAPI 연결 실패: ", e);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(
                    ApiResponse.error("외부 서비스에 연결할 수 없습니다.")
            );

        } catch (Exception e) {
            log.error("❌ 예상치 못한 오류 발생: ", e);
            String errorId = "err-" + UUID.randomUUID().toString().substring(0, 6);

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.error(
                            "서버 내부 오류가 발생했습니다.",
                            new ApiResponse.ErrorDetails(errorId)
                    )
            );
        }
    }

    /**
     * JWT에서 학교 ID 추출 (TODO: 실제 JWT 구현 시 수정 필요)
     */
    private Long extractSchoolIdFromAuth(Authentication authentication) {
        return 1L;
    }

    /**
     * 월간 식단표 조회
     */
    @GetMapping("/monthly/{mealPlanId}")
    public ResponseEntity<ApiResponse<MealPlanMonthlyResponse>> getMealPlanMonthly(
            @PathVariable Long mealPlanId
    ) {
        try {
            log.info("🔍 월간 식단표 조회 API 호출: mealPlanId={}", mealPlanId);

            return mealPlanService.findById(mealPlanId)
                    .map(mealPlan -> {
                        MealPlanMonthlyResponse response = mealPlanService.toMonthlyResponse(mealPlan);
                        return ResponseEntity.ok(
                                ApiResponse.success("월간 식단표 조회 성공", response)
                        );
                    })
                    .orElseGet(() -> {
                        log.warn("⚠️ 월간 식단표를 찾을 수 없음: mealPlanId={}", mealPlanId);
                        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                                ApiResponse.error(
                                        "해당 월간 식단표를 찾을 수 없습니다.",
                                        new ApiResponse.ErrorDetails("mealPlanId", String.valueOf(mealPlanId))
                                )
                        );
                    });

        } catch (Exception e) {
            log.error("❌ 예상치 못한 오류 발생: ", e);
            String errorId = "err-" + UUID.randomUUID().toString().substring(0, 6);

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.error(
                            "서버 내부 오류가 발생했습니다.",
                            new ApiResponse.ErrorDetails(errorId)
                    )
            );
        }
    }

    /**
     * 일간 식단표 상세 조회
     */
    @GetMapping("/menus/{menuDate}/{mealType}")
    public ResponseEntity<ApiResponse<MealPlanDetailResponse>> getMealPlanDetail(
            @PathVariable String menuDate,
            @PathVariable String mealType,
            Authentication authentication
    ) {
        try {
            Long schoolId = extractSchoolIdFromAuth(authentication);
            log.info("🔍 일간 식단표 상세 조회 API 호출: schoolId={}, menuDate={}, mealType={}",
                    schoolId, menuDate, mealType);

            // 날짜 파싱
            LocalDate date;
            try {
                date = LocalDate.parse(menuDate);
            } catch (DateTimeParseException e) {
                log.warn("⚠️ 날짜 형식 오류: menuDate={}", menuDate);
                return ResponseEntity.badRequest().body(
                        ApiResponse.error(
                                "요청 파라미터가 유효하지 않습니다.",
                                new ApiResponse.ErrorDetails("menuDate", "invalid_format")
                        )
                );
            }

            // MealType 파싱
            MealType type;
            try {
                type = MealType.valueOf(mealType.toUpperCase());
            } catch (IllegalArgumentException e) {
                log.warn("⚠️ 식사 유형 오류: mealType={}", mealType);
                return ResponseEntity.badRequest().body(
                        ApiResponse.error(
                                "요청 파라미터가 유효하지 않습니다.",
                                new ApiResponse.ErrorDetails("mealType", "invalid_value")
                        )
                );
            }

            return mealPlanService.findByDateAndMealType(schoolId, date, type)
                    .map(menu -> {
                        MealPlanDetailResponse response = mealPlanService.toDetailResponse(menu);
                        return ResponseEntity.ok(
                                ApiResponse.success("일간 식단표 상세 조회 성공", response)
                        );
                    })
                    .orElseGet(() -> {
                        log.warn("⚠️ 일간 식단표를 찾을 수 없음: menuDate={}, mealType={}", menuDate, mealType);
                        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                                ApiResponse.error(
                                        "해당 날짜와 식사 유형의 식단표가 존재하지 않습니다.",
                                        new ApiResponse.ErrorDetails(menuDate, mealType)
                                )
                        );
                    });

        } catch (Exception e) {
            log.error("❌ 예상치 못한 오류 발생: ", e);
            String errorId = "err-" + UUID.randomUUID().toString().substring(0, 6);

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.error(
                            "서버 내부 오류가 발생했습니다.",
                            new ApiResponse.ErrorDetails(errorId)
                    )
            );
        }
    }

    /**
     * 주간 식단표 조회
     *
     * 사용 방법:
     * 1. GET /mealplan/weekly → 이번 주 (offset=0)
     * 2. GET /mealplan/weekly?offset=-1 → 지난 주
     * 3. GET /mealplan/weekly?offset=1 → 다음 주
     * 4. GET /mealplan/weekly?date=2026-05-15 → 해당 날짜가 포함된 주
     */
    @GetMapping("/weekly")
    public ResponseEntity<ApiResponse<MealPlanWeeklyResponse>> getMealPlanWeekly(
            @RequestParam(required = false) String date,
            @RequestParam(required = false, defaultValue = "0") Integer offset,
            Authentication authentication
    ) {
        try {
            Long schoolId = extractSchoolIdFromAuth(authentication);

            // 기준 날짜 결정
            LocalDate baseDate;
            if (date != null && !date.isBlank()) {
                try {
                    baseDate = LocalDate.parse(date);
                } catch (DateTimeParseException e) {
                    log.warn("⚠️ 날짜 형식 오류: date={}", date);
                    return ResponseEntity.badRequest().body(
                            ApiResponse.error(
                                    "요청 파라미터가 유효하지 않습니다.",
                                    new ApiResponse.ErrorDetails("date", "invalid_format")
                            )
                    );
                }
            } else {
                baseDate = LocalDate.now();
            }

            // 해당 날짜가 포함된 주의 월요일 계산
            LocalDate mondayOfWeek = baseDate.with(java.time.DayOfWeek.MONDAY);

            // offset 적용 (주 단위 이동)
            LocalDate startDate = mondayOfWeek.plusWeeks(offset);
            LocalDate endDate = startDate.plusDays(6);

            // 이번 주 월요일 기준 offset 계산 (응답용)
            LocalDate thisMonday = LocalDate.now().with(java.time.DayOfWeek.MONDAY);
            int currentOffset = (int) java.time.temporal.ChronoUnit.WEEKS.between(thisMonday, startDate);

            log.info("🔍 주간 식단표 조회: schoolId={}, weekStart={}, weekEnd={}, offset={}",
                    schoolId, startDate, endDate, currentOffset);

            List<MealPlanMenu> menus = mealPlanService.findWeeklyMenus(schoolId, startDate, endDate);

            if (menus.isEmpty()) {
                log.warn("⚠️ 주간 식단표 데이터 없음: weekStart={}", startDate);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                        ApiResponse.error(
                                "해당 주간 식단표 데이터가 존재하지 않습니다.",
                                new ApiResponse.ErrorDetails("week_start", startDate.toString())
                        )
                );
            }

            MealPlanWeeklyResponse response = mealPlanService.toWeeklyResponse(
                    schoolId, startDate, endDate, currentOffset, menus
            );
            return ResponseEntity.ok(ApiResponse.success("주간 식단표 조회 성공", response));

        } catch (Exception e) {
            log.error("❌ 예상치 못한 오류 발생: ", e);
            String errorId = "err-" + UUID.randomUUID().toString().substring(0, 6);

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.error(
                            "서버 내부 오류가 발생했습니다.",
                            new ApiResponse.ErrorDetails(errorId)
                    )
            );
        }
    }

    /**
     * AI 자동 대체
     */
    @PutMapping("/ai-replace")
    public ResponseEntity<ApiResponse<MealPlanAIReplaceResponse>> replaceMenuWithAi(
            @Validated @RequestBody MealPlanAIReplaceRequest request,
            Authentication authentication
    ) {
        try {
            Long schoolId = extractSchoolIdFromAuth(authentication);

            // 날짜 파싱
            LocalDate date;
            try {
                date = LocalDate.parse(request.getDate());
            } catch (DateTimeParseException e) {
                log.warn("⚠️ 날짜 형식 오류: date={}", request.getDate());
                return ResponseEntity.badRequest().body(
                        ApiResponse.error(
                                "요청 값이 올바르지 않습니다.",
                                new ApiResponse.ErrorDetails("date", "invalid_format")
                        )
                );
            }

            // MealType 파싱
            MealType mealType;
            try {
                mealType = MealType.valueOf(request.getMealType().toUpperCase());
            } catch (IllegalArgumentException e) {
                log.warn("⚠️ 식사 유형 오류: mealType={}", request.getMealType());
                return ResponseEntity.badRequest().body(
                        ApiResponse.error(
                                "요청 값이 올바르지 않습니다.",
                                new ApiResponse.ErrorDetails("mealType", "invalid_value")
                        )
                );
            }

            log.info("🤖 AI 자동 대체 API 호출: schoolId={}, date={}, mealType={}",
                    schoolId, date, mealType);

            MealPlanAIReplaceResponse response = mealPlanService.replaceMenuWithAi(schoolId, date, mealType);
            return ResponseEntity.ok(ApiResponse.success("AI replaced successfully", response));

        } catch (IllegalArgumentException e) {
            log.warn("⚠️ 대상 없음: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ApiResponse.error(e.getMessage())
            );

        } catch (Exception e) {
            log.error("❌ AI 대체 처리 중 오류 발생: ", e);
            String errorId = "err-" + UUID.randomUUID().toString().substring(0, 6);

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.error(
                            "AI 대체 처리 중 오류가 발생했습니다.",
                            new ApiResponse.ErrorDetails(errorId)
                    )
            );
        }
    }

    /**
     * 식단표 수동 수정
     */
    @PatchMapping("/{mealPlanId}/menus/{menuId}")
    public ResponseEntity<ApiResponse<MealPlanManualUpdateResponse>> updateMenuManually(
            @PathVariable Long mealPlanId,
            @PathVariable Long menuId,
            @Validated @RequestBody MealPlanManualUpdateRequest request,
            Authentication authentication
    ) {
        try {
            Long schoolId = extractSchoolIdFromAuth(authentication);

            log.info("✏️ 식단표 수동 수정 API 호출: schoolId={}, mealPlanId={}, menuId={}",
                    schoolId, mealPlanId, menuId);

            MealPlanManualUpdateResponse response = mealPlanService.updateMenuManually(
                    mealPlanId, menuId, request.getMenus(), request.getReason()
            );

            return ResponseEntity.ok(ApiResponse.success("식단표 수동 수정이 완료되었습니다.", response));

        } catch (IllegalArgumentException e) {
            log.warn("⚠️ 대상 없음: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ApiResponse.error("해당 날짜와 식사 유형의 식단표를 찾을 수 없습니다.")
            );

        } catch (Exception e) {
            log.error("❌ 수동 수정 처리 중 오류 발생: ", e);
            String errorId = "err-" + UUID.randomUUID().toString().substring(0, 6);

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.error(
                            "서버 내부 오류가 발생했습니다.",
                            new ApiResponse.ErrorDetails(errorId)
                    )
            );
        }
    }

    /**
     * 식단표 수정 히스토리 조회
     *
     * Query Parameters:
     * - startDate: 조회 시작 날짜 (예: 2026-05-01)
     * - endDate: 조회 종료 날짜 (예: 2026-05-31)
     * - actionType: ALL (전체), MANUAL_UPDATE (수동), AI_AUTO_REPLACE (AI 자동)
     * - page: 페이지 번호 (기본값 0)
     * - size: 페이지 크기 (기본값 20)
     */
    @GetMapping("/histories")
    public ResponseEntity<ApiResponse<MealPlanHistoryResponse>> getHistories(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String actionType,
            @RequestParam(required = false, defaultValue = "0") Integer page,
            @RequestParam(required = false, defaultValue = "20") Integer size,
            Authentication authentication
    ) {
        try {
            log.info("📜 히스토리 조회 API 호출: startDate={}, endDate={}, actionType={}, page={}, size={}",
                    startDate, endDate, actionType, page, size);

            MealPlanHistoryResponse response = mealPlanService.getHistories(startDate, endDate, actionType, page, size);

            if (response.getItems().isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                        ApiResponse.error("해당 조건의 히스토리가 존재하지 않습니다.")
                );
            }

            return ResponseEntity.ok(ApiResponse.success("식단표 수정 히스토리 조회 성공", response));

        } catch (Exception e) {
            log.error("❌ 히스토리 조회 중 오류 발생: ", e);
            String errorId = "err-" + UUID.randomUUID().toString().substring(0, 6);

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.error(
                            "서버 내부 오류가 발생했습니다.",
                            new ApiResponse.ErrorDetails(errorId)
                    )
            );
        }
    }

    /**
     * Validation 예외 처리
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationExceptions(
            MethodArgumentNotValidException ex
    ) {
        FieldError fieldError = ex.getBindingResult().getFieldError();

        if (fieldError != null) {
            log.warn("⚠️ 검증 실패: 필드={}, 메시지={}",
                    fieldError.getField(), fieldError.getDefaultMessage());

            return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                            "필수 파라미터가 누락되었습니다.",
                            new ApiResponse.ErrorDetails(
                                    fieldError.getField(),
                                    "required"
                            )
                    )
            );
        }

        return ResponseEntity.badRequest().body(
                ApiResponse.error("요청값 검증에 실패했습니다.")
        );
    }
}