import { NAVER_AUTH_URL } from "../../../api/naverLoginApi";

export const NaverLoginButton = () => {
  return (
    <a href={NAVER_AUTH_URL}>
      <button
        type="button"
        className="w-full py-3 rounded-lg bg-[#03C75A] text-white font-medium"
      >
        네이버 로그인
      </button>
    </a>
  );
};