package com.lawchat.infra.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * legal_chatbot_ai(main.py) POST /chat/auto 응답 바디.
 * FastAPI가 snake_case(Pydantic 필드명 그대로)로 내려주므로,
 * 전역 ObjectMapper 네이밍 전략에 의존하지 않도록 필드마다 @JsonProperty로 명시 매핑한다.
 *
 *   class AutoChatResponse(BaseModel):
 *       text: str
 *       detected_domains: list[DomainAnswer]
 *       unavailable_domains: list[str]
 *       answer: str
 */
public record LegalChatbotAiResponse(

        @JsonProperty("text")
        String text,

        @JsonProperty("detected_domains")
        List<DomainAnswer> detectedDomains,

        @JsonProperty("unavailable_domains")
        List<String> unavailableDomains,

        @JsonProperty("answer")
        String answer
) {

    /**
     *   class DomainAnswer(BaseModel):
     *       legal_type: str
     *       legal_type_ko: str
     *       score: float
     *       adapter_used: str
     *       model_group: str
     *       answer: str
     *       sources: list[SourceDoc] = []
     */
    public record DomainAnswer(

            @JsonProperty("legal_type")
            String legalType,

            @JsonProperty("legal_type_ko")
            String legalTypeKo,

            @JsonProperty("score")
            Double score,

            @JsonProperty("adapter_used")
            String adapterUsed,

            @JsonProperty("model_group")
            String modelGroup,

            @JsonProperty("answer")
            String answer,

            @JsonProperty("sources")
            List<SourceDoc> sources
    ) {
    }

    /**
     *   class SourceDoc(BaseModel):
     *       rank: int
     *       law_name: str
     *       article_no: str
     *       docu_type: str
     *       case_num: str
     *       url: str = ""     # rag_chain.build_source_url() 로 우리 쪽 요청으로 추가된 필드
     */
    public record SourceDoc(

            @JsonProperty("rank")
            Integer rank,

            @JsonProperty("law_name")
            String lawName,

            @JsonProperty("article_no")
            String articleNo,

            @JsonProperty("docu_type")
            String docuType,

            @JsonProperty("case_num")
            String caseNum,

            @JsonProperty("url")
            String url
    ) {
    }
}
