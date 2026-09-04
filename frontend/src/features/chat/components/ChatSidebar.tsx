import { Link, NavLink, useParams } from "react-router-dom";
import { useConversations } from "../hooks/useConversations";
import { useUserStore } from "../../../store/userStore";
import { ProfileEditModal } from "../components/ProflieEditModal";
import { useEffect, useState } from "react";

export const ChatSidebar = () => {
  const { userNickName, userProfileImg, fetchUserProfile } = useUserStore();
   const [isProfileModalOpen, setIsProfileModalOpen] = useState(false);
  useEffect(() => {
    fetchUserProfile();
  }, []);
  const nickname = userNickName;
  const { conversationId } = useParams();
  const { conversations } = useConversations();
  const recentConversation = conversations[0];



  const linkClass = (isActive: boolean) =>
    `rounded-lg px-3 py-2.5 text-sm transition-colors ${isActive
      ? "bg-violet-50 font-medium text-violet-700 dark:bg-violet-900/30 dark:text-violet-300"
      : "text-slate-600 hover:bg-slate-50 dark:text-slate-400 dark:hover:bg-slate-800"
    }`;

  return (
    <aside className="flex h-full w-64 shrink-0 flex-col justify-between border-r border-slate-200 bg-white px-4 py-5 dark:border-slate-700 dark:bg-slate-900">
      <div>
        <Link to="/">
          <div className="mb-8 flex items-center gap-2 px-2">
            <div className="flex h-7 w-7 items-center justify-center rounded-full bg-violet-600 text-xs font-bold text-white">
              L
            </div>
            <span className="text-lg font-semibold text-slate-900 dark:text-slate-100">LawChat</span>
          </div>
        </Link>

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
            <span className="cursor-default rounded-lg px-3 py-2.5 text-sm text-slate-300 dark:text-slate-600">최근 채팅</span>
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
        <div className="mb-3 cursor-pointer rounded-lg px-3 py-2.5 text-sm text-slate-400 hover:text-slate-600 dark:hover:text-slate-200"
          onClick={() => setIsProfileModalOpen(true)}>
          설정
        </div>

        <div className="flex items-center gap-2.5 cursor-pointer rounded-lg border border-slate-200 px-3 py-2.5 dark:border-slate-700" onClick={() => setIsProfileModalOpen(true)}>
          <div className="flex h-8 w-8 shrink-0 items-center justify-center overflow-hidden rounded-full bg-slate-200 text-xs font-medium text-slate-600 dark:bg-slate-700 dark:text-slate-300">
            {userProfileImg ? (
              <img
                src={userProfileImg}
                alt={`${nickname} 프로필`}
                className="h-full w-full object-cover"
              />
            ) : (
              nickname.slice(0, 2)
            )}
          </div>
          <div className="leading-tight">
            <p className="text-sm font-medium text-slate-900 dark:text-slate-100">{nickname} 님</p>
            <p className="text-xs text-slate-400">일반 사용자</p>
          </div>
        </div>
      </div>
      {isProfileModalOpen && (
        <ProfileEditModal onClose={() => setIsProfileModalOpen(false)} />
      )}
    </aside>
  );
};