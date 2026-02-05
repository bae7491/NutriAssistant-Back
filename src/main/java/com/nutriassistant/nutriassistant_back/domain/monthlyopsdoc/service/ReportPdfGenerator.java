package com.nutriassistant.nutriassistant_back.domain.monthlyopsdoc.service;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfWriter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 월간 운영자료 JSON 데이터를 PDF로 변환하는 서비스
 */
@Slf4j
@Component
public class ReportPdfGenerator {

    private Font titleFont;
    private Font sectionFont;
    private Font normalFont;
    private boolean fontsInitialized = false;

    // PDF에 포함할 필드와 한글 제목 매핑
    private static final LinkedHashMap<String, String> INCLUDE_FIELDS = new LinkedHashMap<>();
    static {
        INCLUDE_FIELDS.put("summary", "종합 요약");
        INCLUDE_FIELDS.put("leftover", "잔반 분석");
        INCLUDE_FIELDS.put("satisfaction", "만족도 분석");
        INCLUDE_FIELDS.put("issues", "주요 이슈");
        INCLUDE_FIELDS.put("trendAnalysis", "트렌드 분석");
        INCLUDE_FIELDS.put("nutritionQuality", "영양 품질");
        INCLUDE_FIELDS.put("opStrategies", "운영 전략");
    }

    /**
     * 폰트 초기화 (lazy initialization)
     */
    private synchronized void initializeFonts() {
        if (fontsInitialized) {
            return;
        }

        try {
            BaseFont koreanFont = BaseFont.createFont(
                    "HYGoThic-Medium",
                    "UniKS-UCS2-H",
                    BaseFont.NOT_EMBEDDED
            );
            titleFont = new Font(koreanFont, 20, Font.BOLD);
            sectionFont = new Font(koreanFont, 14, Font.BOLD);
            normalFont = new Font(koreanFont, 11, Font.NORMAL);
            log.info("CJK 폰트 로드 성공");
        } catch (Exception e) {
            log.warn("CJK 폰트 로드 실패, 기본 폰트 사용: {}", e.getMessage());
            titleFont = new Font(Font.HELVETICA, 20, Font.BOLD);
            sectionFont = new Font(Font.HELVETICA, 14, Font.BOLD);
            normalFont = new Font(Font.HELVETICA, 11, Font.NORMAL);
        }

        fontsInitialized = true;
    }

    /**
     * 월간 운영자료 데이터를 PDF 바이트 배열로 변환
     */
    @SuppressWarnings("unchecked")
    public byte[] generatePdf(Map<String, Object> reportData, int year, int month, String title) {
        initializeFonts();

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 50, 50, 50, 50);
            PdfWriter.getInstance(document, baos);

            document.open();

            // 제목
            Paragraph titlePara = new Paragraph(title, titleFont);
            titlePara.setAlignment(Element.ALIGN_CENTER);
            titlePara.setSpacingAfter(40);
            document.add(titlePara);

            // doc 내부 데이터 추출
            Map<String, Object> docData = extractDocData(reportData);
            log.info("PDF 생성 - docData 키: {}", docData.keySet());

            // 각 섹션 렌더링
            for (Map.Entry<String, String> field : INCLUDE_FIELDS.entrySet()) {
                String key = field.getKey();
                String sectionTitle = field.getValue();

                Object value = docData.get(key);
                if (value != null && !value.toString().isBlank()) {
                    addSection(document, sectionTitle, value.toString());
                }
            }

            document.close();

            log.info("PDF 생성 완료: {}년 {}월", year, month);
            return baos.toByteArray();

        } catch (Exception e) {
            log.error("PDF 생성 실패", e);
            throw new RuntimeException("PDF 생성 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * reportData에서 doc 내부 데이터 추출
     * 다양한 구조 지원: { doc: {...} }, { data: { doc: {...} } }, 또는 직접 필드
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> extractDocData(Map<String, Object> reportData) {
        // 1. reportData에 직접 summary 등이 있는 경우
        if (reportData.containsKey("summary")) {
            return reportData;
        }

        // 2. reportData.doc에 있는 경우
        if (reportData.containsKey("doc")) {
            Object docObj = reportData.get("doc");
            if (docObj instanceof Map) {
                return (Map<String, Object>) docObj;
            }
        }

        // 3. reportData.data.doc에 있는 경우
        if (reportData.containsKey("data")) {
            Object dataObj = reportData.get("data");
            if (dataObj instanceof Map) {
                Map<String, Object> dataMap = (Map<String, Object>) dataObj;
                if (dataMap.containsKey("doc")) {
                    Object docObj = dataMap.get("doc");
                    if (docObj instanceof Map) {
                        return (Map<String, Object>) docObj;
                    }
                }
                // data에 직접 summary가 있는 경우
                if (dataMap.containsKey("summary")) {
                    return dataMap;
                }
            }
        }

        // 4. 그 외 - 원본 반환
        return reportData;
    }

    /**
     * 섹션 추가
     */
    private void addSection(Document document, String sectionTitle, String content) throws Exception {
        // 섹션 제목
        Paragraph header = new Paragraph("■ " + sectionTitle, sectionFont);
        header.setSpacingBefore(20);
        header.setSpacingAfter(10);
        document.add(header);

        // 섹션 내용
        Paragraph para = new Paragraph(content, normalFont);
        para.setLeading(18);
        para.setSpacingAfter(15);
        document.add(para);
    }
}
