package com.lawchat.domain.user.service;

import com.lawchat.domain.user.entity.User;
import com.lawchat.domain.user.entity.UserStatus;
import com.lawchat.domain.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 보존 기간이 지난 탈퇴 회원의 개인정보를 익명화(파기)하는 서비스.
 *
 * ★ 이 클래스가 존재하는 이유 — "상담 기록은 남기고, 개인정보만 파기한다"
 *
 *   개인정보보호법은 수집 목적을 달성한 개인정보를 지체 없이 파기하도록 하고 있다.
 *   그런데 users row 를 통째로 지우면 chat_sessions 의 ON DELETE CASCADE 때문에
 *   상담 기록까지 함께 사라진다.
 *
 *   그래서 row 는 남기되 그 회원을 특정할 수 있는 정보(이메일, 비밀번호, 소셜 ID,
 *   프로필 사진, 닉네임)만 제거한다. 결과적으로
 *     - 상담 기록  : "누가 했는지 알 수 없는 익명 대화 데이터" 로 계속 보존됨
 *     - 개인정보   : 완전히 사라짐 → 파기 의무 충족
 *   이 되는 구조다.
 *
 * ★ 왜 탈퇴 즉시가 아니라 기간을 두는가
 *   탈퇴 직후에는 결제 분쟁, 문의 재확인, 부정 이용 조사 등으로 원본 정보가 필요할 수 있다.
 *   그래서 보존 기간 동안은 원본을 유지하고, 기간이 지나야 파기한다.
 *   기간은 application.yml 에서 조절한다 (user.withdrawal.retention-days).
 *
 * ★ 배치(스케줄러)와 분리한 이유
 *   스케줄러는 "언제 실행할지"만 담당하고, 실제 로직은 이 서비스가 갖는다.
 *   그래야 어드민 화면에서 수동 실행하거나 테스트에서 직접 호출하기 쉽다.
 */
@Service
public class UserAnonymizationService {

    private static final Logger log = LoggerFactory.getLogger(UserAnonymizationService.class);

    private final UserRepository userRepository;

    /**
     * 개인정보 보존 기간(일).
     * 180 = 약 6개월, 365 = 1년. application.yml 에서 조절한다.
     * 값을 지정하지 않으면 180 일이 기본값이다.
     */
    private final int retentionDays;

    public UserAnonymizationService(
            UserRepository userRepository,
            @Value("${user.withdrawal.retention-days:180}") int retentionDays) {
        this.userRepository = userRepository;
        this.retentionDays = retentionDays;
    }

    /**
     * 보존 기간이 지난 탈퇴 회원을 찾아 개인정보를 익명화한다.
     *
     * @return 이번에 익명화된 회원 수 (로그·모니터링용)
     *
     * 동작 순서
     *  1) 기준 시각(cutoff) 계산 : 오늘 - 보존일수
     *     예) 오늘이 8/24 이고 보존 180일이면 cutoff = 2/25.
     *         2/25 이전에 탈퇴한 회원이 대상이 된다.
     *  2) 대상 조회 : status=DELETED, deletedAt <= cutoff, 아직 익명화 안 된 회원
     *  3) 각 회원에 anonymize() 호출
     *
     * ★ save() 를 호출하지 않는 이유
     *   조회해 온 엔티티는 영속 상태라, 필드를 바꾸면 트랜잭션 커밋 시점에
     *   Hibernate 가 변경 감지로 UPDATE 를 자동 생성한다.
     *
     * ★ @Transactional 이 필수인 이유
     *   이게 없으면 변경 감지가 동작하지 않아 아무것도 DB 에 반영되지 않는다.
     */
    @Transactional
    public int anonymizeExpiredUsers() {

        LocalDateTime cutoff = LocalDateTime.now().minusDays(retentionDays);

        List<User> targets =
                userRepository.findAnonymizationTargets(UserStatus.DELETED, cutoff);

        if (targets.isEmpty()) {
            log.debug("익명화 대상 없음 (기준일: {})", cutoff);
            return 0;
        }

        for (User user : targets) {
            // 개인정보를 로그에 남기면 그 자체가 유출이므로 userId 만 기록한다
            log.info("탈퇴 회원 개인정보 익명화 - userId={}, 탈퇴일={}",
                    user.getUserId(), user.getDeletedAt());
            user.anonymize();
        }

        log.info("익명화 완료 - {}건 (보존기간 {}일, 기준일 {})",
                targets.size(), retentionDays, cutoff);

        return targets.size();
    }
}
