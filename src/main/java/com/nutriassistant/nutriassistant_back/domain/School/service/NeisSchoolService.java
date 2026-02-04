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

/**
 * [NEIS 학교 검색 서비스]
 *
 * 역할:
 * - 교육부 나이스(NEIS) 오픈 API를 호출하여 학교 정보를 검색합니다.
 *
 * 주요 로직:
 * 1. 사용자가 입력한 검색어를 공백 기준으로 나눕니다. (예: "부산 소프트" -> "부산", "소프트")
 * 2. 첫 번째 단어("부산")로 나이스 API를 페이지네이션하며 호출합니다.
 * 3. 받아온 결과 리스트에서 나머지 단어("소프트")가 포함된 학교만 필터링합니다. (Java Stream 활용)
 * 4. 최종 결과를 리스트로 반환합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NeisSchoolService {

    private final RestClient restClient;

    // application.yml에 설정된 나이스 API 인증키
    @Value("${neis.api-key}")
    private String neisApiKey;

    // 나이스 학교 정보 API 기본 URL
    private static final String NEIS_BASE_URL = "https://open.neis.go.kr/hub/schoolInfo";

    // 한 페이지당 가져올 학교 수 (최대 1000까지 가능하나 안정성을 위해 100 사용)
    private static final int PAGE_SIZE = 100;

    // 최대 검색 페이지 수 (무한 루프 방지용 안전장치)
    private static final int MAX_PAGES = 30;

    /**
     * [학교 검색 메인 메서드]
     *
     * @param schoolName 사용자 검색어 (예: "부산소프트")
     * @param normalizedSchoolKind (선택) 학교 급 필터링용 (사용 안 할 경우 null)
     * @return 검색된 학교 리스트
     */
    public List<NeisSchoolResponse.SchoolRow> searchSchools(String schoolName, String normalizedSchoolKind) {
        log.info("나이스 API 학교 검색 진입: {}", schoolName);

        // 1. 입력값 검증 (Null 또는 공백 체크)
        if (schoolName == null) return Collections.emptyList();
        String keyword = schoolName.trim();
        if (keyword.isEmpty()) return Collections.emptyList();

        // 2. 최소 글자수 제한 (1글자 검색 시 결과가 너무 많아 서버 부하 발생 방지)
        // 테스트 시 1글자도 허용하고 싶다면 이 조건을 주석 처리하세요.
        if (keyword.length() < 2) {
            log.warn("검색어가 너무 짧습니다 (2글자 미만): {}", keyword);
            return Collections.emptyList();
        }

        // 3. 단어별 검색 준비 ("부산 소프트" -> tokens=["부산", "소프트"])
        String[] tokens = keyword.split("\\s+");
        String firstToken = tokens[0]; // 실제 API 호출에 사용할 첫 번째 단어
        List<String> extraTokens = (tokens.length > 1)
                ? Arrays.asList(tokens).subList(1, tokens.length)
                : List.of();

        List<NeisSchoolResponse.SchoolRow> aggregated = new ArrayList<>();
        Set<String> seenSchoolCodes = new HashSet<>(); // 중복 제거용 Set

        // 4. 페이지네이션 반복 호출 (최대 MAX_PAGES 만큼)
        for (int pIndex = 1; pIndex <= MAX_PAGES; pIndex++) {

            // 4-1. 나이스 API 호출 (첫 번째 단어로 검색)
            NeisSchoolResponse response = callNeis(firstToken, pIndex, PAGE_SIZE);

            // 4-2. 응답 데이터 파싱
            List<NeisSchoolResponse.SchoolRow> rawRows = extractRows(response);

            // 더 이상 데이터가 없으면 검색 종료
            if (rawRows.isEmpty()) {
                break;
            }

            // 4-3. 추가 단어 필터링 (메모리 상에서 수행)
            // 첫 단어로 가져온 결과 중, 나머지 단어들이 학교 이름에 모두 포함되어 있는지 확인
            List<NeisSchoolResponse.SchoolRow> filteredRows = rawRows;
            if (!extraTokens.isEmpty()) {
                filteredRows = rawRows.stream()
                        .filter(row -> {
                            String name = row.getSchoolName() == null ? "" : row.getSchoolName();
                            // 모든 추가 토큰이 이름에 포함되어야 함 (AND 조건)
                            for (String t : extraTokens) {
                                if (t != null && !name.contains(t)) return false;
                            }
                            return true;
                        })
                        .toList();
            }

            // 4-4. 결과 누적 (중복된 학교 코드는 제외)
            for (NeisSchoolResponse.SchoolRow row : filteredRows) {
                String code = row.getSchoolCode();
                if (code != null && seenSchoolCodes.add(code)) {
                    aggregated.add(row);
                }
            }

            // 4-5. 마지막 페이지인지 확인
            // 가져온 데이터 개수가 페이지 크기보다 작으면 마지막 페이지임
            if (rawRows.size() < PAGE_SIZE) {
                break;
            }
        }

        log.info("검색 완료. 총 {}건 발견", aggregated.size());
        return aggregated;
    }

    /**
     * [나이스 API 호출 내부 메서드]
     * - RestClient를 사용하여 실제 HTTP 요청을 보냅니다.
     * - ★ 수정됨: 한글 인코딩 문제 해결 (.encode().build())
     */
    private NeisSchoolResponse callNeis(String schoolName, int pIndex, int pSize) {

        // URI 생성 (여기서 한글 처리가 중요합니다)
        URI uri = UriComponentsBuilder.fromHttpUrl(NEIS_BASE_URL)
                .queryParam("KEY", neisApiKey)      // API 인증키
                .queryParam("Type", "json")         // JSON 포맷 요청
                .queryParam("pIndex", pIndex)       // 페이지 번호
                .queryParam("pSize", pSize)         // 페이지 크기
                .queryParam("SCHUL_NM", schoolName) // 검색어

                // [핵심 수정] build(true) 대신 encode().build() 사용
                .encode() // UTF-8로 한글 파라미터 인코딩 (필수)
                .build()
                .toUri();

        // 디버깅용 로그 (콘솔에서 요청 URL 확인 가능)
        log.info("나이스 API 요청 URL: {}", uri);

        try {
            return restClient.get()
                    .uri(uri)
                    .retrieve()
                    .body(NeisSchoolResponse.class);
        } catch (Exception e) {
            log.error("나이스 API 호출 실패: {}", e.getMessage());
            // 에러 발생 시 멈추지 않고 빈 결과 반환 (다음 페이지 시도 혹은 종료)
            return null;
        }
    }

    /**
     * [응답 파싱 헬퍼 메서드]
     * - 나이스 API의 복잡한 JSON 구조(schoolInfo -> head/row 혼합)에서 row만 추출
     */
    private List<NeisSchoolResponse.SchoolRow> extractRows(NeisSchoolResponse response) {
        // 응답이 없거나 비정상적이면 빈 리스트 반환
        if (response == null || response.getSchoolInfo() == null) {
            return Collections.emptyList();
        }

        // schoolInfo 리스트 중 row 필드를 가진 객체를 찾아 반환
        return response.getSchoolInfo().stream()
                .filter(wrapper -> wrapper.getRow() != null)
                .findFirst()
                .map(NeisSchoolResponse.SchoolInfoWrapper::getRow)
                .orElse(Collections.emptyList());
    }
}