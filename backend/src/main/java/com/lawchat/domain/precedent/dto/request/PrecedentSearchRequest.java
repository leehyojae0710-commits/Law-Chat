package com.lawchat.domain.precedent.dto.request;

/**
 * GET /api/precedents 쿼리 파라미터.
 * SearchBar(query) + CategoryFilter(category) + AiSimilaritySwitch(aiSimilarity) 값을 함께 받는다.
 * category="전체"는 서비스단에서 null(=조건 없음)로 취급한다.
 * aiSimilarity는 지금은 유사어 확장 검색 로직이 없어 사용하지 않지만, 프론트 스위치 상태를
 * 그대로 받아두어 추후 legal_chatbot_ai 쪽 유사어 확장과 연동할 여지를 남겨둔다.
 */
public record PrecedentSearchRequest(
        String query,
        String category,
        Boolean aiSimilarity,
        Integer page,
        Integer size
) {
}