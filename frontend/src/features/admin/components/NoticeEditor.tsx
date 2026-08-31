import { useEffect, useRef, useState } from "react";
import {
  getAdminNotices,
  createNotice,
  updateNotice,
  deleteNotice,
  toggleNoticePin,
  uploadNoticeFile,
  createPopup,
  getAdminPopups,
  deletePopup,
} from "../../../api/admin";
import { getNotice } from "../../../api/notice";
import type { NoticeListItem, NoticeCategory, NoticePopupAdmin } from "../../notice/types";
import { NOTICE_CATEGORY_LABELS, formatNoticeDate } from "../../notice/types";
import { fa } from "zod/locales";

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


  // 수정 모드: null이면 새 글 작성, 아니면 해당 noticeId를 수정 중
  const [editingId, setEditingId] = useState<number | null>(null);
  const [isLoadingDetail, setIsLoadingDetail] = useState(false);

  // 이미지
  // pendingFile: 사용자가 새로 선택했지만 아직 서버에 업로드하지 않은 파일 — 등록/수정 시점에 업로드됨
  // previewUrl: 미리보기 전용. 새 파일 선택 시 로컬 blob 경로(URL.createObjectURL), 수정 모드 진입 시 서버의 기존 이미지 URL
  const [pendingFile, setPendingFile] = useState<File | null>(null);
  const [previewUrl, setPreviewUrl] = useState<string | undefined>(undefined);
  // existingFileName: 수정 모드 진입 시 서버에 이미 저장돼있는 파일명.
  // 새 이미지를 선택하지 않고 저장할 때, 재업로드 없이 그대로 다시 보내기 위해 씁니다.
  const [existingFileName, setExistingFileName] = useState<string | undefined>(undefined);
  const fileInputRef = useRef<HTMLInputElement>(null);

  // URL에서 파일명만 뽑아냅니다. (예: .../uploads/abc123.jpg → abc123.jpg)
  const extractFileName = (url: string | undefined) => {
    if (!url) return undefined;
    try {
      const path = new URL(url).pathname;
      return path.split("/").pop() || undefined;
    } catch {
      // 절대 URL이 아닌 경우(상대 경로 등)도 대비
      return url.split("/").pop() || undefined;
    }
  };

  // 팝업 노출 옵션
  const [showAsPopup, setShowAsPopup] = useState(false);
  const [popupStart, setPopupStart] = useState(() => toDateInputValue(new Date()));
  const [popupEnd, setPopupEnd] = useState(() => {
    const d = new Date();
    d.setDate(d.getDate() + 7);
    return toDateInputValue(d);
  });

  const today = new Date().toISOString().split('T')[0];

  const [isSubmitting, setIsSubmitting] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);

  // 팝업 목록 (현재 노출 중 / 예정 / 종료된 것 모두 포함해서 받아온 뒤 화면에서 구분)
  const [popups, setPopups] = useState<NoticePopupAdmin[]>([]);
  const [isLoadingPopups, setIsLoadingPopups] = useState(true);

  const refetchPopups = () => {
    setIsLoadingPopups(true);
    getAdminPopups()
      .then(setPopups)
      .catch((err) => console.error("팝업 목록 조회 실패:", err))
      .finally(() => setIsLoadingPopups(false));
  };

  useEffect(() => {
    refetchPopups();
  }, []);

  const handleDeletePopup = async (popupId: number) => {
    await deletePopup(popupId);
    refetchPopups();
  };

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

  // 컴포넌트가 사라질 때 남아있는 blob 미리보기 URL을 정리합니다.
  useEffect(() => {
    return () => {
      if (previewUrl && previewUrl.startsWith("blob:")) URL.revokeObjectURL(previewUrl);
    };
  }, [previewUrl]);

  // 로컬 blob URL은 미리보기용으로만 쓰고, 다 쓰면 메모리에서 해제해줍니다.
  const revokeIfBlob = (url: string | undefined) => {
    if (url && url.startsWith("blob:")) URL.revokeObjectURL(url);
  };

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    revokeIfBlob(previewUrl);
    setPendingFile(file);
    setPreviewUrl(URL.createObjectURL(file));
    // 새 파일을 골랐으니 기존 파일명 표시는 더 이상 필요 없음
    setExistingFileName(undefined);
    setFormError(null);
  };

  const handleRemoveImage = () => {
    revokeIfBlob(previewUrl);
    setPendingFile(null);
    setPreviewUrl(undefined);
    setExistingFileName(undefined);
    if (fileInputRef.current) fileInputRef.current.value = "";
  };

  const resetForm = () => {
    setEditingId(null);
    setTitle("");
    setContent("");
    setCategory("GENERAL");
    revokeIfBlob(previewUrl);
    setPendingFile(null);
    setPreviewUrl(undefined);
    setExistingFileName(undefined);
    setShowAsPopup(false);
    setFormError(null);
    if (fileInputRef.current) fileInputRef.current.value = "";
  };

  // 목록에서 "수정" 클릭 시 상세 조회 후 폼에 채워넣기
  const handleEditClick = async (noticeId: number) => {
    setFormError(null);
    setIsLoadingDetail(true);
    try {
      const detail = await getNotice(noticeId);
      setEditingId(noticeId);
      setTitle(detail.title);
      setContent(detail.content);
      setCategory(detail.category);
      // 상세 조회 응답의 fileUrl은 미리보기용 완전한 URL입니다.
      // 새 이미지를 선택하지 않는 한 pendingFile은 비워둡니다 — 그래야 저장 시
      // 기존 이미지를 건드리지 않고 그대로 유지할 수 있습니다.
      setPendingFile(null);
      setPreviewUrl(detail.fileUrl);
      setExistingFileName(extractFileName(detail.fileUrl));
      setShowAsPopup(false);
      window.scrollTo({ top: 0, behavior: "smooth" });
    } catch (err) {
      console.error("공지 상세 조회 실패:", err);
      setFormError("공지 정보를 불러오지 못했습니다. 다시 시도해주세요.");
    } finally {
      setIsLoadingDetail(false);
    }
  };

  const handleSubmit = async () => {
    if (!title.trim() || !content.trim()) return;
    if (showAsPopup && !pendingFile) {
      setFormError("팝업으로 노출하려면 이미지를 먼저 선택해주세요.");
      return;
    }
    if (showAsPopup && popupStart > popupEnd) {
      setFormError("팝업 종료일은 시작일보다 이후여야 합니다.");
      return;
    }

    setFormError(null);
    setIsSubmitting(true);
    try {
      // 실제 업로드는 여기, 등록/수정을 실행하는 시점에만 한 번 일어납니다.
      let uploadedFileName: string | undefined;
      if (pendingFile) {
        const uploaded = await uploadNoticeFile(pendingFile);
        uploadedFileName = uploaded.fileName;
      }

      if (editingId) {
        // 새 이미지를 선택했으면 그 파일명을, 아니면 기존 파일명을 그대로 보냅니다.
        // (백엔드가 수정 시에도 fileUrl을 필수로 요구하기 때문에, 재업로드 없이
        // existingFileName을 재사용해서 저장 폴더에 중복 파일이 쌓이는 걸 방지합니다.)
        const fileUrlToSend = uploadedFileName ?? existingFileName;
        await updateNotice(editingId, {
          title,
          content,
          ...(fileUrlToSend ? { fileUrl: fileUrlToSend } : {}),
        });
      } else {
        await createNotice({ title, content, category, fileUrl: uploadedFileName });
      }

      if (showAsPopup && uploadedFileName) {
        await createPopup({
          title,
          fileUrl: uploadedFileName,
          altText: title,
          startDate: toIsoStart(popupStart),
          endDate: toIsoEnd(popupEnd),
        });
        refetchPopups();
      }

      resetForm();
      if (page === 0) {
        refetch(0);
      } else {
        setPage(0); // useEffect가 알아서 refetch
      }
    } catch (err) {
      console.error(editingId ? "공지 수정 실패:" : "공지 등록 실패:", err);
      setFormError(editingId ? "공지 수정에 실패했습니다. 다시 시도해주세요." : "공지 등록에 실패했습니다. 다시 시도해주세요.");
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleDelete = async (noticeId: number) => {
    await deleteNotice(noticeId);
    if (editingId === noticeId) resetForm();
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
        <div className="flex items-center justify-between">
          <p className="font-semibold text-sm">{editingId ? "공지 수정" : "새 공지 작성"}</p>
          {editingId && (
            <button onClick={resetForm} className="text-xs text-gray-400 hover:text-gray-600">
              취소하고 새 글 작성
            </button>
          )}
        </div>
        {isLoadingDetail && <p className="text-xs text-gray-400">불러오는 중...</p>}
        <select
          value={category}
          onChange={(e) => setCategory(e.target.value as NoticeCategory)}
          disabled={!!editingId}
          className="w-full border rounded-lg px-3 py-2 text-sm disabled:bg-gray-100 disabled:text-gray-400"
        >
          {Object.entries(NOTICE_CATEGORY_LABELS).map(([value, label]) => (
            <option key={value} value={value}>{label}</option>
          ))}
        </select>
        {editingId && (
          <p className="text-xs text-gray-400">카테고리는 수정할 수 없어요.</p>
        )}
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
          <div className="border rounded-lg px-3 py-2">
            <input
              ref={fileInputRef}
              type="file"
              accept="image/*"
              onChange={handleFileChange}
              className="w-full text-sm file:mr-3 file:py-1 file:px-3 file:rounded-md file:border-0 file:text-xs file:font-medium file:bg-violet-50 file:text-violet-600 hover:file:bg-violet-100"
            />
            {/* <input type="file">는 브라우저 보안 정책상 코드로 파일명을 채워 넣을 수 없어서,
                기존 이미지가 있을 때는 이렇게 별도 텍스트로 파일명을 보여줍니다. */}
            {existingFileName && !pendingFile && (
              <p className="mt-1 text-xs text-gray-400">현재 이미지: {existingFileName}</p>
            )}
          </div>
          {previewUrl && (
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
          )}
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
                min={today}
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
              {!pendingFile && (
                <span className="text-xs text-red-500">팝업 노출에는 새 이미지 선택이 필요해요</span>
              )}
            </div>
          )}
        </div>

        {formError && <p className="text-xs text-red-500">{formError}</p>}

        <button
          onClick={handleSubmit}
          disabled={isSubmitting}
          className="px-4 py-2 rounded-lg bg-violet-600 text-white text-sm font-medium disabled:opacity-50"
        >
          {isSubmitting ? (editingId ? "수정 중..." : "등록 중...") : editingId ? "수정 저장" : "등록"}
        </button>
      </div>

      <div className="border rounded-xl p-4 space-y-3">
        <p className="font-semibold text-sm">팝업 노출 현황</p>
        {isLoadingPopups && <p className="text-sm text-gray-400">불러오는 중...</p>}
        {!isLoadingPopups && popups.length === 0 && (
          <p className="text-sm text-gray-400">등록된 팝업이 없습니다.</p>
        )}
        {!isLoadingPopups && popups.length > 0 && (
          <div className="divide-y">
            {popups
              .slice()
              .sort((a, b) => {
                // 노출중인 것 먼저, 그 다음 시작일이 가까운 순
                if (a.isActive !== b.isActive) return a.isActive ? -1 : 1;
                return a.startDate.localeCompare(b.startDate);
              })
              .map((p) => {
                const now = new Date();
                const isUpcoming = !p.isActive && new Date(p.startDate) > now;
                const statusLabel = p.isActive ? "노출중" : isUpcoming ? "예정" : "종료";
                const statusClass = p.isActive
                  ? "bg-green-600 text-white"
                  : isUpcoming
                    ? "bg-amber-500 text-white"
                    : "bg-gray-300 text-gray-600";
                return (
                  <div key={p.popupId} className="flex items-center justify-between py-3 gap-3">
                    <div className="flex items-center gap-3 min-w-0">
                      <img
                        src={p.fileUrl}
                        alt={p.altText || p.title}
                        className="w-12 h-12 rounded-md border object-cover shrink-0"
                      />
                      <div className="min-w-0">
                        <div className="flex items-center gap-2">
                          <span className={`text-xs px-2 py-0.5 rounded ${statusClass}`}>{statusLabel}</span>
                          <p className="text-sm font-medium truncate">{p.title}</p>
                        </div>
                        <span className="text-xs text-gray-400">
                          {formatNoticeDate(p.startDate)} ~ {formatNoticeDate(p.endDate)}
                        </span>
                      </div>
                    </div>
                    <button
                      onClick={() => handleDeletePopup(p.popupId)}
                      className="text-xs text-red-500 shrink-0"
                    >
                      삭제
                    </button>
                  </div>
                );
              })}
          </div>
        )}
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
              <button onClick={() => handleEditClick(n.noticeId)} className="text-xs text-blue-600">
                수정
              </button>
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
              className={`w-7 h-7 rounded-lg text-sm ${i === page
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