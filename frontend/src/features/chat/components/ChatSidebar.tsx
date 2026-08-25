import { NavLink, useParams } from "react-router-dom";
import { useAuthStore } from "../../../store/authStore";
import { useConversations } from "../hooks/useConversations";

export const ChatSidebar = () => {
  const user = useAuthStore((s) => s.user);
  const { conversationId } = useParams();
  const { conversations } = useConversations();
  const recentConversation = conversations[0];

  const linkClass = (isActive: boolean) =>
    `rounded-lg px-3 py-2.5 text-sm transition-colors ${
      isActive ? "bg-violet-50 font-medium text-violet-700" : "text-slate-600 hover:bg-slate-50"
    }`;

  return (
    <aside className="flex h-full w-64 shrink-0 flex-col justify-between border-r border-slate-200 bg-white px-4 py-5">
      <div>
        <div className="mb-8 flex items-center gap-2 px-2">
          <div className="flex h-7 w-7 items-center justify-center rounded-full bg-violet-600 text-xs font-bold text-white">
            L
          </div>
          <span className="text-lg font-semibold text-slate-900">LawChat</span>
        </div>

        <nav className="flex flex-col gap-1">
          <NavLink to="/chat" end className={({ isActive }) => linkClass(isActive)}>
            새 상담 시작
          </NavLink>

          {recentConversation ? (
            <NavLink
              to={`/chat/${recentConversation.id}`}
              className={() => linkClass(!!conversationId && conversationId === recentConversation.id)}
            >
              최근 채팅
            </NavLink>
          ) : (
            <span className="cursor-default rounded-lg px-3 py-2.5 text-sm text-slate-300">최근 채팅</span>
          )}

          <NavLink to="/chat/history" className={({ isActive }) => linkClass(isActive)}>
            상담 히스토리
          </NavLink>
          <NavLink to="/chat/favorites" className={({ isActive }) => linkClass(isActive)}>
            즐겨찾기
          </NavLink>
        </nav>
      </div>

      <div>
        <div className="mb-3 rounded-lg px-3 py-2.5 text-sm text-slate-400">설정</div>

        <div className="flex items-center gap-2.5 rounded-lg border border-slate-200 px-3 py-2.5">
          <div className="flex h-8 w-8 items-center justify-center rounded-full bg-slate-200 text-xs font-medium text-slate-600">
            {(user?.name ?? "이용자").slice(0, 2)}
          </div>
          <div className="leading-tight">
            <p className="text-sm font-medium text-slate-900">{user?.name ?? "이용자"} 님</p>
            <p className="text-xs text-slate-400">일반 사용자</p>
          </div>
        </div>
      </div>
    </aside>
  );
};
