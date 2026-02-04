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

    private static final String NEIS_BASE_URL = "https://open.neis.go.kr/hub/schoolInfo";
    private static final int PAGE_SIZE = 100;
    private static final int MAX_PAGES = 30;

    /**
     * [학교 검색 단순 호출 메서드]
     * - SchoolService에서 호출하기 편하게 만든 오버로딩 메서드입니다.
     * - SchoolKind(학교 종류) 필터 없이 이름으로만 검색합니다.
     */
    public List<NeisSchoolResponse.SchoolRow> searchSchool(String keyword) {
        return searchSchools(keyword, null);
    }

    /**
     * [학교 검색 핵심 로직]
     * 1. 키워드 분리 ("부산 소프트" -> "부산", "소프트")
     * 2. 첫 단어로 API 페이징 호출 (1~30페이지)
     * 3. 나머지 단어로 결과 필터링
     */
    public List<NeisSchoolResponse.SchoolRow> searchSchools(String schoolName, String normalizedSchoolKind) {
        log.info("나이스 API 학교 검색 진입: {}", schoolName);

        if (schoolName == null) return Collections.emptyList();
        String keyword = schoolName.trim();
        if (keyword.isEmpty()) return Collections.emptyList();

        // 2글자 미만 검색 제한 (선택 사항)
        if (keyword.length() < 2) {
            log.warn("검색어가 너무 짧습니다 (2글자 미만): {}", keyword);
            return Collections.emptyList();
        }

        // 공백 기준 단어 분리
        String[] tokens = keyword.split("\\s+");
        String firstToken = tokens[0];
        List<String> extraTokens = (tokens.length > 1)
                ? Arrays.asList(tokens).subList(1, tokens.length)
                : List.of();

        List<NeisSchoolResponse.SchoolRow> aggregated = new ArrayList<>();
        Set<String> seenSchoolCodes = new HashSet<>();

        for (int pIndex = 1; pIndex <= MAX_PAGES; pIndex++) {
            // API 호출
            NeisSchoolResponse response = callNeis(firstToken, pIndex, PAGE_SIZE);
            List<NeisSchoolResponse.SchoolRow> rawRows = extractRows(response);

            if (rawRows.isEmpty()) break;

            // 추가 단어 필터링 (메모리)
            List<NeisSchoolResponse.SchoolRow> filteredRows = rawRows;
            if (!extraTokens.isEmpty()) {
                filteredRows = rawRows.stream()
                        .filter(row -> {
                            String name = row.getSchoolName() == null ? "" : row.getSchoolName();
                            for (String t : extraTokens) {
                                if (!name.contains(t)) return false;
                            }
                            return true;
                        })
                        .toList();
            }

            // 중복 제거 및 결과 추가
            for (NeisSchoolResponse.SchoolRow row : filteredRows) {
                if (row.getSchoolCode() != null && seenSchoolCodes.add(row.getSchoolCode())) {
                    aggregated.add(row);
                }
            }

            // 페이지 끝 도달 시 종료
            if (rawRows.size() < PAGE_SIZE) break;
        }

        log.info("검색 완료. 총 {}건 발견", aggregated.size());
        return aggregated;
    }

    // 나이스 API 실제 호출
    private NeisSchoolResponse callNeis(String schoolName, int pIndex, int pSize) {
        try {
            URI uri = UriComponentsBuilder.fromHttpUrl(NEIS_BASE_URL)
                    .queryParam("KEY", neisApiKey)
                    .queryParam("Type", "json")
                    .queryParam("pIndex", pIndex)
                    .queryParam("pSize", pSize)
                    .queryParam("SCHUL_NM", schoolName)
                    .encode() // 한글 인코딩 처리
                    .build()
                    .toUri();

            return restClient.get()
                    .uri(uri)
                    .retrieve()
                    .body(NeisSchoolResponse.class);
        } catch (Exception e) {
            log.error("나이스 API 호출 중 오류: {}", e.getMessage());
            return null;
        }
    }

    // JSON 구조에서 row 리스트만 추출
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