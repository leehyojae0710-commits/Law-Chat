package com.lawchat.domain.precedent.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DB: precedents
 * law.go.kr(국가법령정보 공동활용) 판례 Open API로 수집한 판례 원문을 저장한다.
 * case_number(사건번호)가 UNIQUE 이므로, 동일 판례를 다시 동기화하면 새로 만들지 않고
 * {@link #updateFrom(Precedent)}로 기존 row를 갱신한다 (PrecedentSyncService 참고).
 *
 * decidedDate(선고일자): law.go.kr API의 judgmentDate(yyyyMMdd 문자열)를 파싱해서 저장한다.
 * DB 컬럼 decided_date는 별도 ALTER TABLE로 추가되어 있어야 한다 (nullable).
 */
@Entity
@Table(name = "precedents")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Precedent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "precedent_id")
    private Long precedentId;

    @Column(name = "case_number", nullable = false, unique = true, length = 100)
    private String caseNumber; // 사건번호

    @Column(name = "case_name", nullable = false)
    private String caseName; // 사건명

    @Column(name = "court_name", nullable = false, length = 100)
    private String courtName; // 법원명

    @Column(name = "court_type_code", length = 50)
    private String courtTypeCode; // 법원종류코드

    @Column(name = "decided_date")
    private LocalDate decidedDate; // 선고일자

    @Column(name = "case_type_name", length = 50)
    private String caseTypeName; // 사건종류명

    @Lob
    @Column(name = "holding", columnDefinition = "LONGTEXT")
    private String holding; // 판시사항

    @Lob
    @Column(name = "summary", columnDefinition = "LONGTEXT")
    private String summary; // 판결요지

    @Lob
    @Column(name = "referenced_articles", columnDefinition = "LONGTEXT")
    private String referencedArticles; // 참조조문

    @Lob
    @Column(name = "referenced_cases", columnDefinition = "LONGTEXT")
    private String referencedCases; // 참조판례

    @Lob
    @Column(name = "full_text", columnDefinition = "LONGTEXT", nullable = false)
    private String fullText; // 판례내용(전문)

    @Column(name = "synced_at", nullable = false)
    private LocalDateTime syncedAt;

    @Builder
    private Precedent(String caseNumber, String caseName, String courtName, String courtTypeCode,
                      LocalDate decidedDate, String caseTypeName, String holding, String summary,
                      String referencedArticles, String referencedCases, String fullText) {
        this.caseNumber = caseNumber;
        this.caseName = caseName;
        this.courtName = courtName;
        this.courtTypeCode = courtTypeCode;
        this.decidedDate = decidedDate;
        this.caseTypeName = caseTypeName;
        this.holding = holding;
        this.summary = summary;
        this.referencedArticles = referencedArticles;
        this.referencedCases = referencedCases;
        this.fullText = fullText;
        this.syncedAt = LocalDateTime.now();
    }

    public static Precedent create(String caseNumber, String caseName, String courtName, String courtTypeCode,
                                   LocalDate decidedDate, String caseTypeName, String holding, String summary,
                                   String referencedArticles, String referencedCases, String fullText) {
        return Precedent.builder()
                .caseNumber(caseNumber)
                .caseName(caseName)
                .courtName(courtName)
                .courtTypeCode(courtTypeCode)
                .decidedDate(decidedDate)
                .caseTypeName(caseTypeName)
                .holding(holding)
                .summary(summary)
                .referencedArticles(referencedArticles)
                .referencedCases(referencedCases)
                .fullText(fullText)
                .build();
    }

    /**
     * 같은 사건번호로 재동기화될 때 내용을 최신화한다 (영속 상태이므로 dirty checking으로 UPDATE된다).
     */
    public void updateFrom(Precedent fresh) {
        this.caseName = fresh.caseName;
        this.courtName = fresh.courtName;
        this.courtTypeCode = fresh.courtTypeCode;
        this.decidedDate = fresh.decidedDate;
        this.caseTypeName = fresh.caseTypeName;
        this.holding = fresh.holding;
        this.summary = fresh.summary;
        this.referencedArticles = fresh.referencedArticles;
        this.referencedCases = fresh.referencedCases;
        this.fullText = fresh.fullText;
        this.syncedAt = LocalDateTime.now();
    }
}
