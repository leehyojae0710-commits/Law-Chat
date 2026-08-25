import { Outlet } from "react-router-dom";
import { ChatSidebar } from "../../features/chat/components/ChatSidebar";

// /chat 하위 라우트 전체를 감싸는 레이아웃입니다.
// 왼쪽 사이드바(ChatSidebar) + 오른쪽 콘텐츠(Outlet)로 구성됩니다.
export const ChatLayout = () => {
  return (
    <div className="flex h-screen overflow-hidden bg-white">
      <ChatSidebar />
      <main className="flex-1 overflow-y-auto">
        <Outlet />
      </main>
    </div>
  );
};
