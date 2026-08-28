import { useState } from "react";
import type { NoticeListItem as NoticeListItemType } from "../types";
import { NOTICE_CATEGORY_LABELS, formatNoticeDate } from "../types";
import { getNotice } from "../../../api/notice";
import { resolveFileUrl } from "../hooks/useActivePopupNotices";

interface NoticeListItemProps {
  notice: NoticeListItemType;
}

export const NoticeListItem = ({ notice }: NoticeListItemProps) => {
  const [open, setOpen] = useState(false);
  const [content, setContent] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [fileUrl, setFileURL] = useState<string | undefined>(undefined);

  const handleToggle = async () => {
    const next = !open;
    setOpen(next);

    if (next && content === null) {
      setLoading(true);
      try {
        const detail = await getNotice(notice.noticeId);
        setContent(detail.content);
        setFileURL(detail.fileUrl);
      } catch {
        setContent("내용을 불러오지 못했습니다.");
      } finally {
        setLoading(false);
      }
    }
  };

  return (
    <div className="items-center justify-between border rounded-lg p-4">
      <div className="flex items-center gap-2">
        {notice.isPinned && <span className="text-xs bg-purple-600 text-white px-2 py-1 rounded">고정</span>}
        <span className="text-xs bg-gray-100 px-2 py-1 rounded">{NOTICE_CATEGORY_LABELS[notice.category]}</span>
        <p className="font-medium">{notice.title}</p>
      </div>
      <span className="text-xs text-gray-400">{formatNoticeDate(notice.createdAt)}</span>
      <span>
        <button
          onClick={handleToggle}
          className="w-full flex justify-between items-center text-left font-medium">
          {open ? "-" : "+"}
        </button>
      </span>
      {open && (
        <p className="mt-3 text-sm text-gray-600 border-t pt-3">
          {loading ? "불러오는 중..." : content}
          {loading ? "불러오는 중..." : fileUrl ? <img src={fileUrl}/> : ""}
        </p>
      )}
    </div>
  );
};