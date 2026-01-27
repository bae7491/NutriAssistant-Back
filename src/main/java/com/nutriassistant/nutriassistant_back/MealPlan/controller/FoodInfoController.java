package com.nutriassistant.nutriassistant_back.MealPlan.controller;

import com.nutriassistant.nutriassistant_back.MealPlan.entity.FoodInfo;
import com.nutriassistant.nutriassistant_back.MealPlan.repository.FoodInfoRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;

@RestController
@RequestMapping("/internal/food")
public class FoodInfoController {

    private final FoodInfoRepository repo;

    @Value("${internal.token:}")
    private String internalToken;

    public FoodInfoController(FoodInfoRepository repo) {
        this.repo = repo;
    }

    @GetMapping
    public Page<FoodInfo> list(
            @RequestHeader(value="X-INTERNAL-TOKEN", required=false) String token,
            @RequestParam(required=false) Instant since,
            @RequestParam(defaultValue="0") int page,
            @RequestParam(defaultValue="2000") int size
    ) {
        if (internalToken != null && !internalToken.isBlank() && !internalToken.equals(token)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "unauthorized");
        }
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").ascending());
        return (since == null) ? repo.findAll(pageable) : repo.findByUpdatedAtAfter(since, pageable);
    }
}
