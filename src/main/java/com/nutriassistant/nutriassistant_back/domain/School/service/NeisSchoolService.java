package com.nutriassistant.nutriassistant_back.domain.School.service;

import com.nutriassistant.nutriassistant_back.domain.School.dto.NeisSchoolResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class NeisSchoolService {

    private final RestClient restClient;

    @Value("${neis.api-key}")
    private String neisApiKey;

    // ★ 정확한 학교 정보 조회 URL 확인
    private static final String NEIS_BASE_URL = "https://open.neis.go.kr/hub/schoolInfo";
    private static final int PAGE_SIZE = 100;
    private static final int MAX_PAGES = 30;

    /**
     * [학교 검색 진입점]
     * SchoolService에서 호출하는 메서드입니다.
     */
    public List<NeisSchoolResponse.SchoolRow> searchSchool(String keyword) {
        return searchSchoolsInternal(keyword);
    }

    /**
     * [내부 검색 로직]
     * 1. API 호출
     * 2. 검색어 필터링 (띄어쓰기 포함)
     * 3. 결과 리스트 반환
     */
    private List<NeisSchoolResponse.SchoolRow> searchSchoolsInternal(String schoolName) {
        if (schoolName == null || schoolName.trim().isEmpty()) {
            return Collections.emptyList();
        }

        String keyword = schoolName.trim();
        log.info("나이스 API 학교 검색 시작: {}", keyword);

        // 검색어 분리 ("부산 소프트" -> "부산", "소프트")
        String[] tokens = keyword.split("\\s+");
        String firstToken = tokens[0];
        List<String> extraTokens = (tokens.length > 1)
                ? Arrays.asList(tokens).subList(1, tokens.length)
                : List.of();

        List<NeisSchoolResponse.SchoolRow> aggregated = new ArrayList<>();
        Set<String> seenSchoolCodes = new HashSet<>();

        // 최대 30페이지까지 검색
        for (int pIndex = 1; pIndex <= MAX_PAGES; pIndex++) {
            NeisSchoolResponse response = callNeisApi(firstToken, pIndex, PAGE_SIZE);
            List<NeisSchoolResponse.SchoolRow> rawRows = extractRows(response);

            if (rawRows.isEmpty()) break;

            // 추가 검색어(extraTokens)가 모두 포함된 학교만 필터링
            for (NeisSchoolResponse.SchoolRow row : rawRows) {
                boolean match = true;
                String name = row.getSchoolName();

                if (name == null) continue;

                for (String token : extraTokens) {
                    if (!name.contains(token)) {
                        match = false;
                        break;
                    }
                }

                // 중복 제거 후 추가
                if (match && seenSchoolCodes.add(row.getSchoolCode())) {
                    aggregated.add(row);
                }
            }

            if (rawRows.size() < PAGE_SIZE) break; // 더 이상 데이터 없음
        }

        log.info("검색 결과: {}건", aggregated.size());
        return aggregated;
    }

    private NeisSchoolResponse callNeisApi(String schoolName, int pIndex, int pSize) {
        try {
            URI uri = UriComponentsBuilder.fromHttpUrl(NEIS_BASE_URL)
                    .queryParam("KEY", neisApiKey)
                    .queryParam("Type", "json")
                    .queryParam("pIndex", pIndex)
                    .queryParam("pSize", pSize)
                    .queryParam("SCHUL_NM", schoolName)
                    .encode()
                    .build()
                    .toUri();

            return restClient.get()
                    .uri(uri)
                    .retrieve()
                    .body(NeisSchoolResponse.class);
        } catch (Exception e) {
            log.error("나이스 API 호출 실패: {}", e.getMessage());
            return null;
        }
    }

    private List<NeisSchoolResponse.SchoolRow> extractRows(NeisSchoolResponse response) {
        if (response == null || response.getSchoolInfo() == null) {
            return Collections.emptyList();
        }
        return response.getSchoolInfo().stream()
                .filter(wrapper -> wrapper.getRow() != null)
                .findFirst()
                .map(NeisSchoolResponse.SchoolInfoWrapper::getRow)
                .orElse(Collections.emptyList());
    }
}