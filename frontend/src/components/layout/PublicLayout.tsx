import { useState } from "react";
import { Outlet } from "react-router-dom";
import { Header } from "./Header";
import { NoticePopup } from "../../features/notice/components/NoticePopup";
import { useActivePopupNotices } from "../../features/notice/hooks/useActivePopupNotices";

export const PublicLayout = () => {
  const activeNotices = useActivePopupNotices();
  const [showPopup, setShowPopup] = useState(true);

  return (
    <div className="min-h-screen flex flex-col">
      <Header />
      <main className="flex-1">
        <Outlet />
      </main>
      {showPopup && activeNotices.length > 0 && (
        <NoticePopup notices={activeNotices} onCloseAll={() => setShowPopup(false)} />
      )}
    </div>
  );
};