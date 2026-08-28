import { useEffect, useState } from "react";
import { getNotices } from "../../../api/notice";
import type { NoticeListItem } from "../types";
import { formatNoticeDate } from "../types";

export const RecentNoticePanel = () => {
  const [notices, setNotices] = useState<NoticeListItem[]>([]);

  useEffect(() => {
    getNotices(undefined, 0, 5)
      .then((res) => setNotices(res.content))
      .catch(() => setNotices([]));
  }, []);

  return (
    <div className="border rounded-xl p-4 self-start">
      <p className="font-semibold mb-3">최근 공지</p>

      <div className="space-y-3">
        {notices.map((n) => (
          <div key={n.noticeId} className="border-b pb-3">
            <p className="text-sm mb-1">{n.title}</p>
            <span className="text-xs text-gray-400">
              {formatNoticeDate(n.createdAt)}
            </span>
          </div>
        ))}
      </div>
    </div>
  );
};