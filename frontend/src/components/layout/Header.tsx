import { Link, useNavigate } from "react-router-dom";
import { useAuthStore } from "../../store/authStore";
import { useAuth } from "../../features/auth/hooks/useAuth";

const navItems = [
  { label: "서비스 소개", path: "/about" },
  { label: "주요 기능", path: "/features" },
  { label: "FAQ", path: "/faq" },
  { label: "판례검색", path: "/precedents" },
  { label: "고객 센터", path: "/support" },
  { label: "공지 사항", path: "/notices" },
];

export const Header = () => {
  const navigate = useNavigate();
  const { logout } = useAuth();
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated);
  const isAdmin = useAuthStore((s) => s.isAdmin);

  const handleLogout = async () => {
    await logout();
    navigate("/");
  };

  return (
    <header className="sticky top-0 z-50 border-b border-slate-100 bg-white shadow-sm">
      <div className="max-w-7xl mx-auto px-8 h-16 flex items-center justify-between">
        <Link to="/" className="flex items-center gap-2.5">
          <div className="w-8 h-8 rounded-full bg-violet-600 text-white flex items-center justify-center font-bold text-sm">
            L
          </div>
          <div className="leading-tight">
            <p className="font-bold text-[15px] text-slate-900">LawChat</p>
            <p className="text-[10px] text-slate-400">AI 법률 상담 챗봇</p>
          </div>
        </Link>

        <nav className="hidden md:flex items-center gap-8">
          {navItems.map((item) => (
            <Link
              key={item.path}
              to={item.path}
              className="text-sm text-slate-600 hover:text-violet-600 transition-colors"
            >
              {item.label}
            </Link>
          ))}
        </nav>

        <div className="flex items-center">
          {isAuthenticated ? (
            <data className="flex items-center gap-2">
              <button
                type="button"
                onClick={handleLogout}
                className="px-4 py-2 rounded-lg border border-slate-200 text-sm text-slate-700 hover:bg-slate-50 transition-colors"
              >
                로그아웃
              </button>
              <Link
                to={isAdmin ? "/admin" : "/chat"}
                className="px-4 py-2 rounded-lg bg-violet-600 text-white text-sm font-medium hover:bg-violet-700 transition-colors"
              >
                {isAdmin ? "관리자 모드" : "채팅 시작"}
              </Link>
            </data>
          ) : (
            <data className="flex items-center gap-2">
              <Link
                to="/login"
                className="px-4 py-2 rounded-lg border border-slate-200 text-sm text-slate-700 hover:bg-slate-50 transition-colors"
              >
                로그인
              </Link>
              <Link
                to="/Signup"
                className="px-4 py-2 rounded-lg bg-violet-600 text-white text-sm font-medium hover:bg-violet-700 transition-colors"
              >
                회원가입
              </Link>
            </data>
          )}
          </div>
        </div>
    </header>
  );
};
