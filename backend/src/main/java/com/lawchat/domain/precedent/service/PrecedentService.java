package com.lawchat.domain.precedent.service;

import com.lawchat.domain.precedent.dto.response.PrecedentAiSummaryResponse;
import com.lawchat.domain.precedent.dto.response.PrecedentBookmarkResponse;
import com.lawchat.domain.precedent.dto.response.PrecedentDetailResponse;
import com.lawchat.domain.precedent.entity.Precedent;
import com.lawchat.domain.precedent.entity.PrecedentBookmark;
import com.lawchat.domain.precedent.repository.PrecedentBookmarkRepository;
import com.lawchat.domain.precedent.repository.PrecedentRepository;
import com.lawchat.domain.user.entity.User;
import com.lawchat.domain.user.repository.UserRepository;
import com.lawchat.global.exception.BusinessException;
import com.lawchat.global.exception.ErrorCode;
import com.lawchat.infra.ai.client.PrecedentSummaryClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 판례 상세 조회 + 북마크(저장) 처리 + AI 요약.
 * PrecedentResultCard 클릭 시 상세, SavedPrecedentPanel의 저장/목록에 사용한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PrecedentService {

    private final PrecedentRepository precedentRepository;
    private final PrecedentBookmarkRepository precedentBookmarkRepository;
    private final UserRepository userRepository;
    private final PrecedentSummaryClient precedentSummaryClient;

    /**
     * 상세 조회. GET /api/precedents/**는 비로그인도 열람 가능(SecurityConfig)이라
     * userId가 null일 수 있고, 그 경우 isBookmarked는 항상 false로 내려간다.
     */
    public PrecedentDetailResponse getDetail(Long userId, Long precedentId) {
        Precedent precedent = findPrecedentOrThrow(precedentId);
        boolean isBookmarked = userId != null
                && precedentBookmarkRepository.existsByUser_UserIdAndPrecedent_PrecedentId(userId, precedentId);
        return PrecedentDetailResponse.of(precedent, isBookmarked);
    }

    /**
     * AI(KoBART) 판례요약. DB에 저장하지 않고 매 요청마다 legal_chatbot_ai를 호출해 실시간으로 만든다.
     * AI 서버가 콜드스타트/kobart 미로딩 등으로 실패하면 502(PRECEDENT_AI_SUMMARY_FAILED)로 변환해 던진다.
     */
    public PrecedentAiSummaryResponse getAiSummary(Long precedentId) {
        Precedent precedent = findPrecedentOrThrow(precedentId);
        try {
            var aiResponse = precedentSummaryClient.summarize(precedent.getFullText());
            return PrecedentAiSummaryResponse.from(aiResponse);
        } catch (PrecedentSummaryClient.PrecedentSummaryClientException e) {
            log.error("AI 판례요약 실패. precedentId={}", precedentId, e);
            throw new BusinessException(ErrorCode.PRECEDENT_AI_SUMMARY_FAILED);
        }
    }

    public List<PrecedentBookmarkResponse> getBookmarks(Long userId) {
        return precedentBookmarkRepository.findByUser_UserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(PrecedentBookmarkResponse::from)
                .toList();
    }

    /**
     * 이미 저장된 판례를 다시 저장 요청하면 조용히 무시한다(버튼 연타/중복 클릭 대비).
     */
    @Transactional
    public void addBookmark(Long userId, Long precedentId) {
        if (precedentBookmarkRepository.existsByUser_UserIdAndPrecedent_PrecedentId(userId, precedentId)) {
            return;
        }
        User user = findUserOrThrow(userId);
        Precedent precedent = findPrecedentOrThrow(precedentId);
        precedentBookmarkRepository.save(PrecedentBookmark.create(user, precedent));
    }

    @Transactional
    public void removeBookmark(Long userId, Long precedentId) {
        precedentBookmarkRepository.findByUser_UserIdAndPrecedent_PrecedentId(userId, precedentId)
                .ifPresent(precedentBookmarkRepository::delete);
    }

    private Precedent findPrecedentOrThrow(Long precedentId) {
        return precedentRepository.findById(precedentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRECEDENT_NOT_FOUND));
    }

    private User findUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }
}