import { useState } from "react";
import { Link } from "react-router-dom";
import { useConversations } from "../hooks/useConversations";

const formatYmd = (iso: string) => {
  const d = new Date(iso);
  return `${d.getFullYear()}.${String(d.getMonth() + 1).padStart(2, "0")}.${String(d.getDate()).padStart(2, "0")}`;
};

export const FavoritesList = () => {
  const [tab, setTab] = useState<"consult" | "precedent">("consult");
  const { conversations, isLoading, toggleFavorite } = useConversations({ favoriteOnly: true });

  return (
    <div className="mx-auto max-w-4xl px-6 py-8">
      <h1 className="mb-6 text-xl font-bold text-slate-900">
        즐겨찾기 <span className="ml-1 text-sm font-normal text-slate-400">총 {conversations.length}건</span>
      </h1>

      <div className="mb-6 flex gap-2">
        <button
          type="button"
          onClick={() => setTab("consult")}
          className={`rounded-lg px-4 py-2 text-sm font-medium ${
            tab === "consult" ? "bg-violet-600 text-white" : "border border-slate-200 text-slate-600"
          }`}
        >
          상담 즐겨찾기
        </button>
        <button
          type="button"
          onClick={() => setTab("precedent")}
          className={`rounded-lg px-4 py-2 text-sm font-medium ${
            tab === "precedent" ? "bg-violet-600 text-white" : "border border-slate-200 text-slate-600"
          }`}
        >
          판례 즐겨찾기
        </button>
      </div>

      {tab === "consult" ? (
        <>
          {isLoading && <p className="py-10 text-center text-sm text-slate-400">불러오는 중이에요…</p>}
          {!isLoading && conversations.length === 0 && (
            <p className="py-10 text-center text-sm text-slate-400">즐겨찾기한 상담이 없어요.</p>
          )}

          <div className="space-y-3">
            {conversations.map((c) => (
              <div
                key={c.id}
                className="flex items-center justify-between rounded-xl border border-slate-200 px-5 py-4"
              >
                <div className="min-w-0">
                  <p className="truncate font-semibold text-slate-900">{c.title}</p>
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
                  <button
                    type="button"
                    onClick={() => toggleFavorite(c.id, false)}
                    aria-label="즐겨찾기 해제"
                    className="text-lg text-violet-600"
                  >
                    ★
                  </button>
                </div>
              </div>
            ))}
          </div>

          <p className="mt-6 rounded-lg bg-slate-50 px-4 py-3 text-sm text-slate-500">
            ★ 아이콘을 다시 누르면 즐겨찾기에서 제거돼요
          </p>
        </>
      ) : (
        <div className="rounded-xl border border-dashed border-slate-200 px-5 py-10 text-center text-sm text-slate-500">
          판례 즐겨찾기는 판례검색 페이지에서 별표를 눌러 저장하고 관리할 수 있어요.
          <div className="mt-3">
            <Link to="/precedents" className="font-medium text-violet-600 hover:underline">
              판례 검색으로 이동 →
            </Link>
          </div>
        </div>
      )}
    </div>
  );
};
