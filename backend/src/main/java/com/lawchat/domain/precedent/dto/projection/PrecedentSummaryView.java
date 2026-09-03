package com.lawchat.domain.precedent.dto.projection;

import java.time.LocalDate;

/**
 * 판례 목록/검색(GET /api/precedents)용 프로젝션.
 *
 * 기존에는 PrecedentRepository#search가 "SELECT p FROM Precedent p ..."로 엔티티 전체를
 * 가져왔는데, Precedent에는 holding/summary/referencedArticles/referencedCases/fullText 같은
 * LONGTEXT 컬럼(특히 fullText=판례 전문)이 붙어있어서 목록 카드가 실제로 쓰지도 않는
 * 대용량 텍스트를 페이지마다(검색이든 단순 페이지 이동이든) 통째로 읽어오고 있었다.
 *
 * 목록 카드(PrecedentResultCard)가 실제로 쓰는 필드만 인터페이스 프로젝션으로 선언하면
 * Hibernate가 SELECT 절에 이 필드들만 넣어서 쿼리하므로 LOB 컬럼을 아예 건드리지 않는다
 * (검색 키워드가 없는 일반 페이지 이동에서 특히 효과가 크다).
 */
public interface PrecedentSummaryView {

    Long getPrecedentId();

    String getCaseNumber();

    String getCaseName();

    String getCourtName();

    LocalDate getDecidedDate();

    String getSummary();

    String getCaseTypeName();
}
