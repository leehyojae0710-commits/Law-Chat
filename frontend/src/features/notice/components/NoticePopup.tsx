import { useEffect, useState } from "react";
import type { NoticePopup as NoticePopupType } from "../types";
import { dismissPopupForDays } from "../hooks/useActivePopupNotices";
import { useNavigate } from "react-router-dom";

interface NoticePopupProps {
  notices: NoticePopupType[];
  onCloseAll: () => void;
}

const AUTO_ADVANCE_MS = 10000;

export const NoticePopup = ({ notices, onCloseAll }: NoticePopupProps) => {
  const [index, setIndex] = useState(0);
  const [hideForWeek, setHideForWeek] = useState(false);
  const current = notices[index];

  const goPrev = () => setIndex((i) => (i === 0 ? notices.length - 1 : i - 1));
  const goNext = () => setIndex((i) => (i === notices.length - 1 ? 0 : i + 1));
  const goTo = (i: number) => setIndex(i);

  const navigate = useNavigate();

  useEffect(() => {
    if (notices.length <= 1) return;
    const timer = setTimeout(goNext, AUTO_ADVANCE_MS);
    return () => clearTimeout(timer);
  }, [index, notices.length]);

  const handleClose = () => {
    if (hideForWeek) {
      dismissPopupForDays(7);
    }
    onCloseAll();
  };
  
  const handleGoToNotices=() =>{
    handleClose();
    navigate("/notices");
  }

  if (!current) return null;

  return (
    <div className="fixed inset-0 bg-black/40 flex items-center justify-center">
      <div className="relative bg-white rounded-xl p-6 max-w-md w-full">
        <button onClick={handleClose} aria-label="닫기" className="absolute top-4 right-4 text-slate-400 hover:text-slate-600">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <path d="M18 6 6 18M6 6l12 12" strokeLinecap="round" />
          </svg>
        </button>

        <div className="flex items-center gap-3">
          {notices.length > 1 && (
            <button onClick={goPrev} aria-label="이전 공지" className="shrink-0 text-slate-400 hover:text-violet-600">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <path d="M15 18l-6-6 6-6" strokeLinecap="round" strokeLinejoin="round" />
              </svg>
            </button>
          )}

          <div className="flex-1 min-w-0 min-h-[100px] flex flex-col justify-center">
            {current.fileUrl ? (
              <a onClick={handleGoToNotices} style={{ cursor: "pointer" }}>
                <p>{current.altText}</p>
                <img src={current.fileUrl} className="w-full h-auto max-h-72 object-contain rounded-lg" />
                <p>{current.title}</p>
              </a>
            ) : (
              <p className="font-semibold mb-2 whitespace-pre-line line-clamp-2">{current.title}</p>
            )}
          </div>

          {notices.length > 1 && (
            <button onClick={goNext} aria-label="다음 공지" className="shrink-0 text-slate-400 hover:text-violet-600">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <path d="M9 6l6 6-6 6" strokeLinecap="round" strokeLinejoin="round" />
              </svg>
            </button>
          )}
        </div>

        {notices.length > 1 && (
          <div className="flex items-center justify-center gap-2 mb-4">
            {notices.map((n, i) => (
              <button
                key={n.popupId}
                onClick={() => goTo(i)}
                aria-label={`${i + 1}번째 공지로 이동`}
                className={`w-2 h-2 rounded-full transition-colors ${i === index ? "bg-violet-600" : "bg-slate-200 hover:bg-slate-300"}`}
              />
            ))}
          </div>
        )}

        <label className="flex items-center gap-2 text-sm mb-4">
          <input type="checkbox" checked={hideForWeek} onChange={(e) => setHideForWeek(e.target.checked)} />
          7일간 다시 보지 않기
        </label>

        <button onClick={handleClose} className="w-full py-3 rounded-lg bg-purple-600 text-white">
          닫기
        </button>
      </div>
    </div>
  );
};