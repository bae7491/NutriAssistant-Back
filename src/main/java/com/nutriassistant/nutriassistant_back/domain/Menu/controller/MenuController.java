package com.nutriassistant.nutriassistant_back.domain.Menu.controller;

import com.nutriassistant.nutriassistant_back.domain.Menu.DTO.FoodInfoDetailResponse;
import com.nutriassistant.nutriassistant_back.domain.Menu.DTO.FoodInfoListResponse;
import com.nutriassistant.nutriassistant_back.domain.Menu.service.MenuService;
import com.nutriassistant.nutriassistant_back.global.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/foodinfo")
public class MenuController {

    private final MenuService menuService;

    public MenuController(MenuService menuService) {
        this.menuService = menuService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<FoodInfoListResponse>> getFoodInfoList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        try {
            if (size > 100) {
                return ResponseEntity.badRequest().body(
                        ApiResponse.error("요청 파라미터가 올바르지 않습니다.",
                                new ApiResponse.ErrorDetails("size", "max 100"))
                );
            }

            if (page < 1) {
                return ResponseEntity.badRequest().body(
                        ApiResponse.error("요청 파라미터가 올바르지 않습니다.",
                                new ApiResponse.ErrorDetails("page", "min 1"))
                );
            }

            log.info("🔍 메뉴 목록 조회: page={}, size={}", page, size);

            FoodInfoListResponse response = menuService.getFoodInfoList(page - 1, size);

            return ResponseEntity.ok(
                    ApiResponse.success("메뉴 목록 조회 성공", response)
            );

        } catch (Exception e) {
            log.error("❌ 메뉴 목록 조회 중 오류 발생: ", e);
            String errorId = "err-" + UUID.randomUUID().toString().substring(0, 6);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.error("서버 내부 오류가 발생했습니다.",
                            new ApiResponse.ErrorDetails(errorId))
            );
        }
    }

    @GetMapping("/{menuId}")
    public ResponseEntity<ApiResponse<FoodInfoDetailResponse>> getFoodInfoDetail(
            @PathVariable String menuId
    ) {
        try {
            if (menuId == null || menuId.isBlank()) {
                return ResponseEntity.badRequest().body(
                        ApiResponse.error("요청 파라미터가 올바르지 않습니다.",
                                new ApiResponse.ErrorDetails("menuId", "invalid format"))
                );
            }

            log.info("🔍 메뉴 상세 조회: menuId={}", menuId);

            Optional<FoodInfoDetailResponse> response = menuService.getFoodInfoDetail(menuId);

            if (response.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                        ApiResponse.error("해당 메뉴를 찾을 수 없습니다.",
                                new ApiResponse.ErrorDetails("menu_id", menuId))
                );
            }

            return ResponseEntity.ok(
                    ApiResponse.success("메뉴 상세 정보 조회 성공", response.get())
            );

        } catch (Exception e) {
            log.error("❌ 메뉴 상세 조회 중 오류 발생: ", e);
            String errorId = "err-" + UUID.randomUUID().toString().substring(0, 6);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.error("서버 내부 오류가 발생했습니다.",
                            new ApiResponse.ErrorDetails(errorId))
            );
        }
    }
}
