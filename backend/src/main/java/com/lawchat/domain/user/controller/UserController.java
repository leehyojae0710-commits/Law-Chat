package com.lawchat.domain.user.controller;

import com.lawchat.domain.user.dto.request.UpdateProfileRequest;
import com.lawchat.domain.user.dto.response.UserProfileResponse;
import com.lawchat.domain.user.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * 회원 정보 조회/수정/탈퇴 담당.
 *
 * @Validated : 메서드 파라미터(@RequestParam 등)에 붙은 검증 애노테이션을 동작시키려면 필요하다.
 *              (@Valid 는 @RequestBody 객체에만 적용된다)
 */
@RestController
@RequestMapping("/api/users")
@Validated
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * 내 정보 조회
     * GET /api/users/me
     *
     * URL 에 userId 를 넣지 않고 토큰에서 꺼내는 이유:
     * /api/users/{userId} 로 만들면 다른 사람 번호를 넣어 조회를 시도할 수 있어
     * 매번 "본인인지" 검사 코드를 넣어야 한다. me 방식이 안전하고 단순하다.
     */
    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getMyProfile(@AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(userService.getMyProfile(userId));
    }

    /**
     * 프로필 수정
     * PATCH /api/users/me
     *
     * PUT 이 아니라 PATCH 인 이유: 전달된 필드만 부분 수정하기 때문.
     *
     * ★ phone 필드가 추가됐지만 프론트 수정은 필요 없다.
     *   기존 프론트가 { nickname, profileImg } 만 보내면 phone 은 JSON 에 없는 필드라
     *   자동으로 null 이 되고, null 이면 서비스가 전화번호를 건드리지 않는다.
     *   전화번호 수정 UI 가 생기면 그때 body 에 phone 만 추가로 실어 보내면 된다.
     */
    @PatchMapping("/me")
    public ResponseEntity<UserProfileResponse> updateProfile(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody UpdateProfileRequest request) {

        return ResponseEntity.ok(
                userService.updateProfile(userId, request.nickname(), request.profileImg(), request.phone()));
    }

    /**
     * 프로필 이미지 변경
     * POST /api/users/me/profile-image
     *
     * Content-Type: multipart/form-data
     * 파트 이름: file
     *
     * ★ PATCH /me 와 나눈 이유
     *   PATCH /me 는 JSON 을 받는다. 이미지 파일은 JSON 에 담을 수 없어
     *   multipart 로 받아야 하므로 엔드포인트를 분리했다.
     *   대신 "업로드 후 다시 PATCH 호출" 같은 2단계를 요구하지 않고,
     *   이 요청 하나로 저장 + 반영까지 끝낸다.
     *
     * ★ 응답이 PATCH /me 와 동일한 UserProfileResponse 인 이유
     *   프론트가 업로드 후 프로필을 다시 조회할 필요 없이
     *   이 응답만으로 화면(프로필 사진, 닉네임 등)을 갱신할 수 있게 하기 위함이다.
     *   응답의 profileImg 는 브라우저가 바로 <img src> 에 넣을 수 있는 절대 URL 이다.
     *
     * 제약: PNG/JPG/GIF/WEBP, 최대 5MB
     */
    @PostMapping("/me/profile-image")
    public ResponseEntity<UserProfileResponse> updateProfileImage(
            @AuthenticationPrincipal Long userId,
            @RequestParam("file") MultipartFile file) {

        return ResponseEntity.ok(userService.updateProfileImage(userId, file));
    }

    /**
     * 비밀번호 변경
     * PATCH /api/users/me/password
     */
    @PatchMapping("/me/password")
    public ResponseEntity<Void> changePassword(
            @AuthenticationPrincipal Long userId,
            @RequestBody ChangePasswordRequest request) {

        userService.changePassword(userId, request.currentPassword(), request.newPassword());
        return ResponseEntity.noContent().build();
    }

    /**
     * 회원 탈퇴
     * DELETE /api/users/me
     *
     * 실제로 row 를 삭제하지 않고 status=DELETED, deleted_at 기록만 남긴다(soft delete).
     * 대화 기록/문의 내역 등이 FK 로 물려 있어 하드 삭제하면 연쇄 삭제가 일어나기 때문.
     */
    @DeleteMapping("/me")
    public ResponseEntity<Void> withdraw(@AuthenticationPrincipal Long userId) {
        userService.withdraw(userId);
        return ResponseEntity.noContent().build();
    }

    /**
     * 이메일 중복 확인 (회원가입 화면에서 호출, 비로그인 허용)
     * GET /api/users/check-email?email=test@example.com
     */
    @GetMapping("/check-email")
    public ResponseEntity<Map<String, Boolean>> checkEmail(
            @RequestParam @NotBlank @Email String email) {
        return ResponseEntity.ok(Map.of("available", userService.isEmailAvailable(email)));
    }

    /**
     * 닉네임 중복 확인
     * GET /api/users/check-nickname?nickname=승철
     */
    @GetMapping("/check-nickname")
    public ResponseEntity<Map<String, Boolean>> checkNickname(
            @RequestParam @NotBlank String nickname) {
        return ResponseEntity.ok(Map.of("available", userService.isNicknameAvailable(nickname)));
    }

    public record ChangePasswordRequest(String currentPassword, String newPassword) {}
}
