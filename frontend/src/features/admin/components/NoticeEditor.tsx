import { useEffect, useState } from "react";
import {
  getAdminNotices,
  createNotice,
  deleteNotice,
  toggleNoticePin,
} from "../../../api/admin";
import type { NoticeListItem, NoticeCategory } from "../../notice/types";
import { NOTICE_CATEGORY_LABELS, formatNoticeDate } from "../../notice/types";

export const NoticeEditor = () => {
  const [notices, setNotices] = useState<NoticeListItem[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [title, setTitle] = useState("");
  const [content, setContent] = useState("");
  const [category, setCategory] = useState<NoticeCategory>("GENERAL");

  const refetch = () => {
    setIsLoading(true);
    getAdminNotices()
      .then((res) => setNotices(res.content))
      .finally(() => setIsLoading(false));
  };

  useEffect(() => {
    refetch();
  }, []);

  const handleCreate = async () => {
    if (!title.trim() || !content.trim()) return;
    await createNotice({ title, content, category });
    setTitle("");
    setContent("");
    refetch();
  };

  const handleDelete = async (noticeId: number) => {
    await deleteNotice(noticeId);
    refetch();
  };

  const handleTogglePin = async (noticeId: number) => {
    await toggleNoticePin(noticeId);
    refetch();
  };

  if (isLoading) return <p className="text-sm text-gray-400">불러오는 중...</p>;

  return (
    <div className="space-y-6">
      <div className="border rounded-xl p-4 space-y-3">
        <p className="font-semibold text-sm">새 공지 작성</p>
        <select
          value={category}
          onChange={(e) => setCategory(e.target.value as NoticeCategory)}
          className="w-full border rounded-lg px-3 py-2 text-sm"
        >
          {Object.entries(NOTICE_CATEGORY_LABELS).map(([value, label]) => (
            <option key={value} value={value}>{label}</option>
          ))}
        </select>
        <input
          value={title}
          onChange={(e) => setTitle(e.target.value)}
          placeholder="공지 제목"
          className="w-full border rounded-lg px-3 py-2 text-sm"
        />
        <textarea
          value={content}
          onChange={(e) => setContent(e.target.value)}
          placeholder="공지 내용"
          rows={3}
          className="w-full border rounded-lg px-3 py-2 text-sm"
        />
        <button
          onClick={handleCreate}
          className="px-4 py-2 rounded-lg bg-violet-600 text-white text-sm font-medium"
        >
          등록
        </button>
      </div>

      <div className="border rounded-xl divide-y">
        {notices.map((n) => (
          <div key={n.noticeId} className="flex items-center justify-between p-4">
            <div>
              <div className="flex items-center gap-2">
                {n.isPinned && <span className="text-xs bg-purple-600 text-white px-2 py-0.5 rounded">고정</span>}
                <p className="text-sm font-medium">{n.title}</p>
              </div>
              <span className="text-xs text-gray-400">
                {NOTICE_CATEGORY_LABELS[n.category]} · {formatNoticeDate(n.createdAt)}
              </span>
            </div>
            <div className="flex gap-3">
              <button onClick={() => handleTogglePin(n.noticeId)} className="text-xs text-violet-600">
                {n.isPinned ? "고정 해제" : "고정"}
              </button>
              <button onClick={() => handleDelete(n.noticeId)} className="text-xs text-red-500">
                삭제
              </button>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
};