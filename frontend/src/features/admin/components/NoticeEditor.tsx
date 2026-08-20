import { useEffect, useState } from "react";
import {
  getAdminNotices,
  createNotice,
  deleteNotice,
  type Notice,
} from "../../../api/admin";

export const NoticeEditor = () => {
  const [notices, setNotices] = useState<Notice[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [title, setTitle] = useState("");
  const [content, setContent] = useState("");

  const refetch = () => {
    setIsLoading(true);
    getAdminNotices()
      .then(setNotices)
      .finally(() => setIsLoading(false));
  };

  useEffect(() => {
    refetch();
  }, []);

  const handleCreate = async () => {
    if (!title.trim() || !content.trim()) return;
    await createNotice({ title, content, category: "서비스 업데이트", isVisible: true });
    setTitle("");
    setContent("");
    refetch();
  };

  const handleDelete = async (id: string) => {
    await deleteNotice(id);
    refetch();
  };

  if (isLoading) return <p className="text-sm text-gray-400">불러오는 중...</p>;

  return (
    <div className="space-y-6">
      <div className="border rounded-xl p-4 space-y-3">
        <p className="font-semibold text-sm">새 공지 작성</p>
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
          <div key={n.id} className="flex items-center justify-between p-4">
            <div>
              <p className="text-sm font-medium">{n.title}</p>
              <span className="text-xs text-gray-400">{n.category}</span>
            </div>
            <button onClick={() => handleDelete(n.id)} className="text-xs text-red-500">
              삭제
            </button>
          </div>
        ))}
      </div>
    </div>
  );
};
