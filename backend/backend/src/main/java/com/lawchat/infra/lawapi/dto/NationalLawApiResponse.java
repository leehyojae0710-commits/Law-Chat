package com.lawchat.infra.lawapi.dto;

import java.util.List;

/**
 * 국가법령정보 공동활용(law.go.kr) "판례" Open API 응답을 담는 DTO 모음.
 *
 * 이 API는 XML 응답만 안정적으로 문서화되어 있어(JSON 지원 여부가 target별로 불확실),
 * NationalLawApiClient에서 XML을 JDK 내장 DOM 파서로 직접 파싱해 아래 record로 변환한다.
 * (Jackson과 무관 - 별도 라이브러리 의존성 추가 없음)
 *
 * 참고:
 *   - 목록 조회: GET /DRF/lawSearch.do?target=prec
 *   - 본문 조회: GET /DRF/lawService.do?target=prec&ID={판례일련번호}
 */
public class NationalLawApiResponse {

    private NationalLawApiResponse() {
    }

    /** 목록 조회(lawSearch.do) 결과 한 페이지. */
    public record SearchResult(
            int totalCount,
            int page,
            List<PrecedentSummary> items
    ) {
    }

    /** 목록 조회에 포함된 판례 1건의 요약 정보 (본문은 포함하지 않음 - fetchDetail로 별도 조회). */
    public record PrecedentSummary(
            String serialNumber,   // 판례일련번호 - 본문 조회 시 ID로 사용
            String caseName,       // 사건명
            String caseNumber,     // 사건번호
            String judgmentDate,   // 선고일자 (yyyyMMdd)
            String courtName,      // 법원명
            String caseTypeName,   // 사건종류명
            String caseTypeCode,   // 사건종류코드
            String judgmentType,   // 판결유형
            String verdict,        // 선고 (예: 파기환송)
            String detailLink      // 판례상세링크
    ) {
    }

    /** 본문 조회(lawService.do) 결과. */
    public record PrecedentDetail(
            String serialNumber,       // 판례일련번호
            String caseName,           // 사건명
            String caseNumber,         // 사건번호
            String judgmentDate,       // 선고일자
            String verdict,            // 선고
            String courtName,          // 법원명
            String courtTypeCode,      // 법원종류코드
            String caseTypeName,       // 사건종류명
            String judgmentType,       // 판결유형
            String holdingIssues,      // 판시사항
            String summary,            // 판결요지
            String referencedArticles, // 참조조문
            String referencedCases,    // 참조판례
            String fullText            // 판례내용(전문)
    ) {
    }
}
