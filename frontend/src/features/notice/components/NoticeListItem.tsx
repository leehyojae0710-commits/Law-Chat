import type { Notice } from "../types";
import { useState } from "react";

interface NoticeListItemProps {
  notice: Notice;
}

export const NoticeListItem = ({ notice }: NoticeListItemProps) => {
  const [open, setNoticeOpen] = useState(false);
  return (
    <div className="items-center justify-between border rounded-lg p-4">
      <div className="flex items-center gap-2">
        {notice.pinned && <span className="text-xs bg-purple-600 text-white px-2 py-1 rounded">고정</span>}
        <span className="text-xs bg-gray-100 px-2 py-1 rounded">{notice.category}</span>
        <p className="font-medium">{notice.title}</p>
      </div>
      <span className="text-xs text-gray-400">{notice.date}</span>
      <span>
        <button
          onClick={() => setNoticeOpen(!open)}
          className="w-full flex justify-between items-center text-left font-medium">
          {open ? "-" : "+"}
        </button>
      </span>
      {open && notice.detail ? <p className="mt-3 text-sm text-gray-600 border-t pt-3">{notice.detail}</p> : <p></p>}
    </div>
  );
};
