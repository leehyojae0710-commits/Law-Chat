import { getNaverAuthUrl } from "../../../api/naverLoginApi";

export const NaverLoginButton = () => {
  const handleClick = async () => {
    try {
      const url = await getNaverAuthUrl();
      window.location.href = url;
    } catch {
      alert("네이버 로그인을 시작할 수 없습니다. 잠시 후 다시 시도해주세요.");
    }
  };

  return (
    <button
      type="button"
      onClick={handleClick}
      className="w-full py-3 rounded-lg bg-[#03C75A] text-white font-medium"
    >
      네이버 로그인
    </button>
  );
};