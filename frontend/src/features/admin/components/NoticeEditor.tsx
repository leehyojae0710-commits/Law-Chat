import { useEffect, useRef, useState } from "react";
import {
  getAdminNotices,
  createNotice,
  deleteNotice,
  toggleNoticePin,
  uploadNoticeFile,
  createPopup,
} from "../../../api/admin";
import type { NoticeListItem, NoticeCategory } from "../../notice/types";
import { NOTICE_CATEGORY_LABELS, formatNoticeDate } from "../../notice/types";

const PAGE_SIZE = 10;

// 오늘/일주일 뒤 날짜를 <input type="date"> 형식(YYYY-MM-DD)으로
const toDateInputValue = (d: Date) => {
  const yyyy = d.getFullYear();
  const mm = String(d.getMonth() + 1).padStart(2, "0");
  const dd = String(d.getDate()).padStart(2, "0");
  return `${yyyy}-${mm}-${dd}`;
};

// 날짜 문자열(YYYY-MM-DD)을 하루의 시작/끝 시각을 포함한 ISO 문자열로 변환
const toIsoStart = (dateStr: string) => new Date(`${dateStr}T00:00:00`).toISOString();
const toIsoEnd = (dateStr: string) => new Date(`${dateStr}T23:59:59`).toISOString();

export const NoticeEditor = () => {
  const [notices, setNotices] = useState<NoticeListItem[]>([]);
  const [totalPages, setTotalPages] = useState(1);
  const [page, setPage] = useState(0); // 0-indexed
  const [isLoading, setIsLoading] = useState(true);
  const [title, setTitle] = useState("");
  const [content, setContent] = useState("");
  const [category, setCategory] = useState<NoticeCategory>("GENERAL");

  // 이미지 업로드
  // fileName: 등록 요청(fileUrl 필드)에 넣을 값 / previewUrl: 미리보기 전용
  const [fileName, setFileName] = useState<string | undefined>(undefined);
  const [previewUrl, setPreviewUrl] = useState<string | undefined>(undefined);
  const [isUploading, setIsUploading] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);

  // 팝업 노출 옵션
  const [showAsPopup, setShowAsPopup] = useState(false);
  const [popupStart, setPopupStart] = useState(() => toDateInputValue(new Date()));
  const [popupEnd, setPopupEnd] = useState(() => {
    const d = new Date();
    d.setDate(d.getDate() + 7);
    return toDateInputValue(d);
  });

  const [isSubmitting, setIsSubmitting] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);

  const refetch = (targetPage = page) => {
    setIsLoading(true);
    getAdminNotices(targetPage, PAGE_SIZE)
      .then((res) => {
        setNotices(res.content);
        setTotalPages(res.totalPages);
      })
      .catch((err) => console.error("공지 목록 조회 실패:", err))
      .finally(() => setIsLoading(false));
  };

  useEffect(() => {
    refetch(page);
  }, [page]);

  const handleFileChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    setIsUploading(true);
    setFormError(null);
    try {
      const uploaded = await uploadNoticeFile(file);
      setFileName(uploaded.fileName);
      setPreviewUrl(uploaded.fileUrl);
    } catch (err) {
      console.error("이미지 업로드 실패:", err);
      setFormError("이미지 업로드에 실패했습니다. 다시 시도해주세요.");
    } finally {
      setIsUploading(false);
    }
  };

  const handleRemoveImage = () => {
    setFileName(undefined);
    setPreviewUrl(undefined);
    if (fileInputRef.current) fileInputRef.current.value = "";
  };

  const resetForm = () => {
    setTitle("");
    setContent("");
    setFileName(undefined);
    setPreviewUrl(undefined);
    setShowAsPopup(false);
    if (fileInputRef.current) fileInputRef.current.value = "";
  };

  const handleCreate = async () => {
    if (!title.trim() || !content.trim()) return;
    if (isUploading) {
      setFormError("이미지 업로드가 끝난 뒤 등록해주세요.");
      return;
    }
    if (showAsPopup && !fileName) {
      setFormError("팝업으로 노출하려면 이미지를 먼저 업로드해주세요.");
      return;
    }
    if (showAsPopup && popupStart > popupEnd) {
      setFormError("팝업 종료일은 시작일보다 이후여야 합니다.");
      return;
    }

    setFormError(null);
    setIsSubmitting(true);
    try {
      await createNotice({ title, content, category, fileUrl: fileName });

      if (showAsPopup && fileName) {
        await createPopup({
          title,
          fileUrl: fileName,
          altText: title,
          startDate: toIsoStart(popupStart),
          endDate: toIsoEnd(popupEnd),
        });
      }

      resetForm();
      if (page === 0) {
        refetch(0);
      } else {
        setPage(0); // useEffect가 알아서 refetch
      }
    } catch (err) {
      console.error("공지 등록 실패:", err);
      setFormError("공지 등록에 실패했습니다. 다시 시도해주세요.");
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleDelete = async (noticeId: number) => {
    await deleteNotice(noticeId);
    refetch(page);
  };

  const handleTogglePin = async (noticeId: number) => {
    await toggleNoticePin(noticeId);
    refetch(page);
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

        {/* 이미지 업로드 */}
        <div className="space-y-2">
          <label className="text-xs font-medium text-gray-500">이미지 (선택)</label>
          {previewUrl ? (
            <div className="relative inline-block">
              <img src={previewUrl} alt="첨부 이미지 미리보기" className="max-h-40 rounded-lg border" />
              <button
                onClick={handleRemoveImage}
                className="absolute -top-2 -right-2 w-6 h-6 rounded-full bg-white border text-xs text-red-500 shadow"
                aria-label="이미지 제거"
              >
                ✕
              </button>
            </div>
          ) : (
            <input
              ref={fileInputRef}
              type="file"
              accept="image/*"
              onChange={handleFileChange}
              className="w-full text-sm"
            />
          )}
          {isUploading && <p className="text-xs text-gray-400">업로드 중...</p>}
        </div>

        {/* 팝업 노출 옵션 */}
        <div className="border rounded-lg p-3 space-y-2 bg-gray-50">
          <label className="flex items-center gap-2 text-sm font-medium">
            <input
              type="checkbox"
              checked={showAsPopup}
              onChange={(e) => setShowAsPopup(e.target.checked)}
            />
            이 공지를 팝업으로도 노출하기
          </label>
          {showAsPopup && (
            <div className="flex items-center gap-2 text-sm pl-6">
              <input
                type="date"
                value={popupStart}
                onChange={(e) => setPopupStart(e.target.value)}
                className="border rounded-lg px-2 py-1.5 text-sm"
              />
              <span className="text-gray-400">~</span>
              <input
                type="date"
                value={popupEnd}
                onChange={(e) => setPopupEnd(e.target.value)}
                className="border rounded-lg px-2 py-1.5 text-sm"
              />
              {!fileName && (
                <span className="text-xs text-red-500">팝업 노출에는 이미지가 필요해요</span>
              )}
            </div>
          )}
        </div>

        {formError && <p className="text-xs text-red-500">{formError}</p>}

        <button
          onClick={handleCreate}
          disabled={isSubmitting || isUploading}
          className="px-4 py-2 rounded-lg bg-violet-600 text-white text-sm font-medium disabled:opacity-50"
        >
          {isSubmitting ? "등록 중..." : "등록"}
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
        {notices.length === 0 && (
          <p className="text-sm text-gray-400 p-4">등록된 공지가 없습니다.</p>
        )}
      </div>

      {/* 페이지네이션 바 */}
      {totalPages > 1 && (
        <div className="flex items-center justify-center gap-1">
          <button
            onClick={() => setPage((p) => Math.max(0, p - 1))}
            disabled={page === 0}
            className="px-2 py-1 text-sm text-gray-500 disabled:opacity-30"
          >
            ‹
          </button>
          {Array.from({ length: totalPages }, (_, i) => (
            <button
              key={i}
              onClick={() => setPage(i)}
              className={`w-7 h-7 rounded-lg text-sm ${
                i === page
                  ? "bg-violet-600 text-white font-medium"
                  : "text-gray-500 hover:bg-violet-50"
              }`}
            >
              {i + 1}
            </button>
          ))}
          <button
            onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))}
            disabled={page === totalPages - 1}
            className="px-2 py-1 text-sm text-gray-500 disabled:opacity-30"
          >
            ›
          </button>
        </div>
      )}
    </div>
  );
};