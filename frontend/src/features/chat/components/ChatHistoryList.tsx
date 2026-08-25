import { useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { useConversations } from "../hooks/useConversations";

const PAGE_SIZE = 5;

const formatYmd = (iso: string) => {
  const d = new Date(iso);
  return `${d.getFullYear()}.${String(d.getMonth() + 1).padStart(2, "0")}.${String(d.getDate()).padStart(2, "0")}`;
};

export const ChatHistoryList = () => {
  const { conversations, keyword, setKeyword, isLoading, toggleFavorite, removeConversation } = useConversations();
  const [page, setPage] = useState(1);
  const [openMenuId, setOpenMenuId] = useState<string | null>(null);

  const totalPages = Math.max(1, Math.ceil(conversations.length / PAGE_SIZE));
  const pageItems = useMemo(
    () => conversations.slice((page - 1) * PAGE_SIZE, page * PAGE_SIZE),
    [conversations, page]
  );

  return (
    <div className="mx-auto max-w-4xl px-6 py-8">
      <div className="mb-6 flex items-center justify-between">
        <h1 className="text-xl font-bold text-slate-900">
          상담 히스토리 <span className="ml-1 text-sm font-normal text-slate-400">총 {conversations.length}건</span>
        </h1>
        <Link
          to="/chat"
          className="rounded-lg bg-violet-600 px-4 py-2 text-sm font-medium text-white hover:bg-violet-700"
        >
          새 상담
        </Link>
      </div>

      <input
        value={keyword}
        onChange={(e) => {
          setKeyword(e.target.value);
          setPage(1);
        }}
        placeholder="제목이나 내용으로 검색"
        className="mb-6 w-full rounded-xl border border-slate-200 px-4 py-2.5 text-sm outline-none focus:border-violet-400"
      />

      {isLoading && <p className="py-10 text-center text-sm text-slate-400">불러오는 중이에요…</p>}

      {!isLoading && conversations.length === 0 && (
        <p className="py-10 text-center text-sm text-slate-400">아직 상담 기록이 없어요.</p>
      )}

      <div className="space-y-3">
        {pageItems.map((c) => (
          <div
            key={c.id}
            className="flex items-center justify-between rounded-xl border border-slate-200 px-5 py-4 hover:border-slate-300"
          >
            <div className="min-w-0">
              <div className="flex items-center gap-2">
                <p className="truncate font-semibold text-slate-900">{c.title}</p>
                {c.hasSummary && (
                  <span className="shrink-0 rounded-full bg-violet-50 px-2 py-0.5 text-xs font-medium text-violet-600">
                    요약서
                  </span>
                )}
              </div>
              <p className="mt-0.5 truncate text-sm text-slate-500">{c.preview}</p>
            </div>

            <div className="flex shrink-0 items-center gap-3 pl-4">
              <span className="text-xs text-slate-400">{formatYmd(c.updatedAt)}</span>
              <Link
                to={`/chat/${c.id}`}
                className="rounded-lg bg-violet-50 px-3 py-1.5 text-sm font-medium text-violet-700 hover:bg-violet-100"
              >
                이어서 상담
              </Link>

              <div className="relative">
                <button
                  type="button"
                  onClick={() => setOpenMenuId(openMenuId === c.id ? null : c.id)}
                  className="rounded-lg border border-slate-200 px-2.5 py-1.5 text-sm text-slate-500 hover:bg-slate-50"
                >
                  ···
                </button>
                {openMenuId === c.id && (
                  <div className="absolute right-0 top-9 z-10 w-40 rounded-lg border border-slate-200 bg-white py-1 shadow-lg">
                    <button
                      type="button"
                      onClick={() => {
                        toggleFavorite(c.id, !c.isFavorite);
                        setOpenMenuId(null);
                      }}
                      className="flex w-full items-center gap-2 px-3 py-2 text-left text-sm text-violet-600 hover:bg-slate-50"
                    >
                      ★ {c.isFavorite ? "즐겨찾기 해제" : "즐겨찾기 추가"}
                    </button>
                    <button
                      type="button"
                      onClick={() => {
                        removeConversation(c.id);
                        setOpenMenuId(null);
                      }}
                      className="flex w-full items-center gap-2 px-3 py-2 text-left text-sm text-red-500 hover:bg-slate-50"
                    >
                      ✕ 채팅 삭제
                    </button>
                  </div>
                )}
              </div>
            </div>
          </div>
        ))}
      </div>

      {totalPages > 1 && (
        <div className="mt-8 flex justify-center gap-2">
          {Array.from({ length: totalPages }, (_, i) => i + 1).map((p) => (
            <button
              key={p}
              type="button"
              onClick={() => setPage(p)}
              className={`h-8 w-8 rounded-full text-sm ${
                p === page ? "bg-violet-600 text-white" : "text-slate-500 hover:bg-slate-100"
              }`}
            >
              {p}
            </button>
          ))}
        </div>
      )}
    </div>
  );
};
