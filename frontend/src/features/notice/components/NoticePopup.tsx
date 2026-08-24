import { useState } from "react";
import type { Notice } from "../types";

interface NoticePopupProps {
  notice: Notice;
  onClose: () => void;
}

export const NoticePopup = ({ notice, onClose }: NoticePopupProps) => {
  const [hideForWeek, setHideForWeek] = useState(false);

  const handleClose = () => {
    if (hideForWeek) {
      // TODO: 쿠키/로컬스토리지에 7일간 숨김 처리 저장
    }
    onClose();
  };

  return (
    <div className="fixed inset-0 bg-black/40 flex items-center justify-center">
      <div className="bg-white rounded-xl p-6 max-w-md w-full">
        <p className="font-semibold mb-2">{notice.title}</p>
        <p className="text-xs text-gray-400 mb-4">{notice.date} 게시</p>
        <label className="flex items-center gap-2 text-sm mb-4">
          <input
            type="checkbox"
            checked={hideForWeek}
            onChange={(e) => setHideForWeek(e.target.checked)}
          />
          7일간 다시 보지 않기
        </label>
        <button onClick={handleClose} className="w-full py-3 rounded-lg bg-purple-600 text-white">
          닫기
        </button>
      </div>
    </div>
  );
};
