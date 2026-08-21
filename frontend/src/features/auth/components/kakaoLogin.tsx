import { KAKAO_AUTH_URL } from "../../../api/kakaoLoginApi";

export const KakaoLoginButton = () => {
  return (
    <a href={KAKAO_AUTH_URL}>
      <button
        type="button"
        className="w-full py-3 rounded-lg bg-[#FEE500] text-[#191919] font-medium"
      >
        카카오톡 로그인
      </button>
    </a>
  );
};