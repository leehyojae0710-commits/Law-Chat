package com.lawchat.domain.inquiry.service;

/**
 * 관리자 화면에서 작성자 이메일만 필요해서, users 도메인과의 결합을 이 인터페이스로 끊어둡니다.
 * 기존 UserRepository 를 주입해 한 줄로 구현하면 됩니다.
 *
 * 예)
 * @Component
 * public class UserEmailLookupImpl implements UserEmailLookup {
 *     private final UserRepository userRepository;
 *     public String findEmail(Long userId) {
 *         if (userId == null) return null;                       // 탈퇴 회원
 *         return userRepository.findById(userId).map(User::getEmail).orElse(null);
 *     }
 * }
 */
public interface UserEmailLookup {
    String findEmail(Long userId);
}
