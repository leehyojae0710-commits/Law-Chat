package com.lawchat.infra.lawapi.client;

import com.lawchat.infra.lawapi.dto.NationalLawApiResponse.PrecedentDetail;
import com.lawchat.infra.lawapi.dto.NationalLawApiResponse.PrecedentSummary;
import com.lawchat.infra.lawapi.dto.NationalLawApiResponse.SearchResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Slf4j
@Component
@RequiredArgsConstructor
public class NationalLawApiClient {

    private static final String SEARCH_PATH = "/lawSearch.do";
    private static final String DETAIL_PATH = "/lawService.do";
    private static final String TARGET = "prec";

    // db_loader.py의 _xtext()와 동일한 방식으로 <br/> 등 잔여 HTML 태그를 정제
    private static final Pattern BR_TAG = Pattern.compile("<br\\s*/?>", Pattern.CASE_INSENSITIVE);
    private static final Pattern ANY_TAG = Pattern.compile("<[^>]+>");
    private static final Pattern MULTI_NEWLINE = Pattern.compile("\\n{3,}");

    private final RestClient lawOpenApiRestClient;

    @Value("${law-api.oc}")
    private String oc;

    /**
     * 판례 목록 조회.
     *
     * @param query 검색어. null/blank면 미적용.
     * @param date  등록일자 필터 (yyyyMMdd). null/blank면 미적용.
     * @param page  1부터 시작.
     * @param display 페이지당 건수 (최대 100).
     */
    public SearchResult search(String query, String date, int page, int display) {
        String xml;
        try {
            xml = lawOpenApiRestClient.get()
                    .uri(uriBuilder -> {
                        uriBuilder.path(SEARCH_PATH)
                                .queryParam("OC", oc)
                                .queryParam("target", TARGET)
                                .queryParam("type", "XML")
                                .queryParam("page", page)
                                .queryParam("display", display);
                        if (query != null && !query.isBlank()) {
                            uriBuilder.queryParam("query", query);
                        }
                        if (date != null && !date.isBlank()) {
                            uriBuilder.queryParam("date", date);
                        }
                        return uriBuilder.build();
                    })
                    .retrieve()
                    .body(String.class);
        } catch (RestClientResponseException e) {
            log.error("판례 목록 조회 실패. status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new NationalLawApiClientException("판례 목록 조회 API 응답 오류: " + e.getStatusCode(), e);
        } catch (Exception e) {
            log.error("판례 목록 조회 연결 실패. query={}, date={}, page={}", query, date, page, e);
            throw new NationalLawApiClientException("판례 목록 조회 API에 연결할 수 없습니다.", e);
        }

        if (xml == null || xml.isBlank()) {
            log.warn("판례 목록 API 응답이 비어있습니다.");
            return new SearchResult(0, page, List.of());
        }

        return parseSearchResult(xml);
    }

    /**
     * 판례 본문 조회.
     *
     * @param serialNumber 판례일련번호 (search()로 얻은 PrecedentSummary.serialNumber())
     */
    public PrecedentDetail fetchDetail(String serialNumber) {
        String xml;
        try {
            xml = lawOpenApiRestClient.get()
                    .uri(uriBuilder -> uriBuilder.path(DETAIL_PATH)
                            .queryParam("OC", oc)
                            .queryParam("target", TARGET)
                            .queryParam("type", "XML")
                            .queryParam("ID", serialNumber)
                            .build())
                    .retrieve()
                    .body(String.class);
        } catch (RestClientResponseException e) {
            log.error("판례 본문 조회 실패. serialNumber={}, status={}, body={}", serialNumber, e.getStatusCode(), e.getResponseBodyAsString());
            throw new NationalLawApiClientException("판례 본문 조회 API 응답 오류: " + e.getStatusCode(), e);
        } catch (Exception e) {
            log.error("판례 본문 조회 연결 실패. serialNumber={}", serialNumber, e);
            throw new NationalLawApiClientException("판례 본문 조회 API에 연결할 수 없습니다.", e);
        }

        return parseDetail(xml);
    }

    // ===== XML 파싱 =====

    private SearchResult parseSearchResult(String xml) {
        Element root = parseRoot(xml);

        // API 오류 메시지가 반환된 경우 처리
        String message = textOf(root, "message");
        String resultMsg = textOf(root, "resultMsg");
        if (message != null || resultMsg != null) {
            log.warn("법령 API 응답 메시지: message={}, resultMsg={}", message, resultMsg);
        }

        int totalCount = parseIntOrZero(textOf(root, "totalCnt"));
        int page = parseIntOrZero(textOf(root, "page"));

        List<Element> itemElements = new ArrayList<>();
        
        // 1. prec, precInfo, law 태그 탐색
        NodeList byTag = root.getElementsByTagName("prec");
        if (byTag.getLength() == 0) {
            byTag = root.getElementsByTagName("precInfo");
        }
        if (byTag.getLength() == 0) {
            byTag = root.getElementsByTagName("law");
        }

        if (byTag.getLength() > 0) {
            for (int i = 0; i < byTag.getLength(); i++) {
                itemElements.add((Element) byTag.item(i));
            }
        } else {
            // 2. 판례일련번호 태그를 가진 모든 부모 요소 탐색
            NodeList serialNodes = root.getElementsByTagName("판례일련번호");
            for (int i = 0; i < serialNodes.getLength(); i++) {
                Node parent = serialNodes.item(i).getParentNode();
                if (parent instanceof Element && !itemElements.contains(parent)) {
                    itemElements.add((Element) parent);
                }
            }
        }

        List<PrecedentSummary> items = new ArrayList<>();
        for (Element item : itemElements) {
            String serialNumber = textOf(item, "판례일련번호");
            if (serialNumber == null || serialNumber.isBlank()) {
                continue;
            }

            items.add(new PrecedentSummary(
                    serialNumber,
                    textOf(item, "사건명"),
                    textOf(item, "사건번호"),
                    textOf(item, "선고일자"),
                    textOf(item, "법원명"),
                    textOf(item, "사건종류명"),
                    textOf(item, "사건종류코드"),
                    textOf(item, "판결유형"),
                    textOf(item, "선고"),
                    textOf(item, "판례상세링크")
            ));
        }

        log.info("판례 목록 검색 완료: totalCnt={}, items={}", totalCount, items.size());
        return new SearchResult(totalCount, page, items);
    }

    private PrecedentDetail parseDetail(String xml) {
        Element root = parseRoot(xml);

        return new PrecedentDetail(
                textOf(root, "판례일련번호"),
                textOf(root, "사건명"),
                textOf(root, "사건번호"),
                textOf(root, "선고일자"),
                textOf(root, "선고"),
                textOf(root, "법원명"),
                textOf(root, "법원종류코드"),
                textOf(root, "사건종류명"),
                textOf(root, "판결유형"),
                textOf(root, "판시사항"),
                textOf(root, "판결요지"),
                textOf(root, "참조조문"),
                textOf(root, "참조판례"),
                textOf(root, "판례내용")
        );
    }

    private Element parseRoot(String xml) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
            return document.getDocumentElement();
        } catch (Exception e) {
            log.error("XML 파싱 실패 원본: {}", xml);
            throw new NationalLawApiClientException("판례 API 응답(XML) 파싱에 실패했습니다.", e);
        }
    }

    /** 부모 엘리먼트 내 특정 태그의 텍스트 값을 추출 (직계/하위 포함), <br/> 등 잔여 태그 정제 */
    private String textOf(Element parent, String tagName) {
        if (parent == null) {
            return null;
        }
        NodeList list = parent.getElementsByTagName(tagName);
        if (list.getLength() > 0 && list.item(0) != null) {
            String text = list.item(0).getTextContent();
            if (text == null || text.isBlank()) {
                return null;
            }
            String cleaned = cleanText(text);
            return cleaned.isBlank() ? null : cleaned;
        }
        return null;
    }

    /**
     * API 응답 텍스트에 섞여 들어오는 &lt;br/&gt; 등 잔여 HTML 태그를 정제한다.
     * db_loader.py의 _xtext() 정제 로직과 동일하게:
     *   1) &lt;br/&gt; -&gt; 줄바꿈
     *   2) 그 외 모든 태그 제거
     *   3) 3줄 이상 연속 줄바꿈은 2줄로 축소
     */
    private String cleanText(String text) {
        String cleaned = BR_TAG.matcher(text).replaceAll("\n");
        cleaned = ANY_TAG.matcher(cleaned).replaceAll("");
        cleaned = MULTI_NEWLINE.matcher(cleaned).replaceAll("\n\n");
        return cleaned.trim();
    }

    private int parseIntOrZero(String value) {
        if (value == null) {
            return 0;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    // ===== 호출 실패 처리 =====

    public static class NationalLawApiClientException extends RuntimeException {
        public NationalLawApiClientException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}