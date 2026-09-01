import { useRef, useState } from "react";
import { createInquiry, uploadInquiryScreenshot } from "../../../api/inquiry";
import type { InquiryCategory } from "../types";
import { INQUIRY_CATEGORY_LABELS } from "../types";

const CATEGORY_OPTIONS = Object.keys(INQUIRY_CATEGORY_LABELS) as InquiryCategory[];

interface InquiryFormProps {
  // 등록 성공 후 문의함 탭으로 이동시키기 위한 콜백
  onSubmitted?: () => void;
}

export const InquiryForm = ({ onSubmitted }: InquiryFormProps) => {
  const [category, setCategory] = useState<InquiryCategory>("BUG");
  const [title, setTitle] = useState("");
  const [content, setContent] = useState("");
  const [pendingFile, setPendingFile] = useState<File | null>(null);
  const [previewUrl, setPreviewUrl] = useState<string | undefined>(undefined);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const revokeIfBlob = (url?: string) => {
    if (url && url.startsWith("blob:")) URL.revokeObjectURL(url);
  };

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    revokeIfBlob(previewUrl);
    setPendingFile(file);
    setPreviewUrl(URL.createObjectURL(file));
    setFormError(null);
  };

  const handleRemoveImage = () => {
    revokeIfBlob(previewUrl);
    setPendingFile(null);
    setPreviewUrl(undefined);
    if (fileInputRef.current) fileInputRef.current.value = "";
  };

  const resetForm = () => {
    setCategory("BUG");
    setTitle("");
    setContent("");
    revokeIfBlob(previewUrl);
    setPendingFile(null);
    setPreviewUrl(undefined);
    if (fileInputRef.current) fileInputRef.current.value = "";
  };

  const handleSubmit = async () => {
    const trimmedTitle = title.trim();
    const trimmedContent = content.trim();

    if (trimmedTitle.length < 2 || trimmedTitle.length > 100) {
      setFormError("제목은 2~100자로 입력해주세요.");
      return;
    }
    if (trimmedContent.length < 10 || trimmedContent.length > 2000) {
      setFormError("문의 내용은 10~2000자로 입력해주세요.");
      return;
    }

    setFormError(null);
    setIsSubmitting(true);
    try {
      // 스크린샷은 여기서 실제로 업로드하고, DB 저장용인 fileName만 등록 요청에 실어 보냅니다.
      let screenshotFileName: string | undefined;
      if (pendingFile) {
        const uploaded = await uploadInquiryScreenshot(pendingFile);
        screenshotFileName = uploaded.fileName;
      }

      await createInquiry({
        category,
        title: trimmedTitle,
        content: trimmedContent,
        screenshotUrl: screenshotFileName,
      });

      resetForm();
      onSubmitted?.();
    } catch (err) {
      console.error("문의 등록 실패:", err);
      setFormError("문의 등록에 실패했습니다. 다시 시도해주세요.");
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="border rounded-xl p-6 space-y-4">
      <div>
        <p className="font-semibold">1:1 문의하기</p>
        <p className="text-sm text-gray-500">궁금한 점이나 불편한 점을 남겨주시면 빠르게 확인할게요</p>
      </div>

      <div>
        <p className="text-sm font-medium mb-2">문의 유형</p>
        <div className="flex gap-2 flex-wrap">
          {CATEGORY_OPTIONS.map((c) => (
            <button
              key={c}
              onClick={() => setCategory(c)}
              className={`px-3 py-2 rounded-full border text-sm transition-colors ${
                category === c
                  ? "bg-violet-600 text-white border-violet-600"
                  : "text-gray-600 hover:border-violet-300"
              }`}
            >
              {INQUIRY_CATEGORY_LABELS[c]}
            </button>
          ))}
        </div>
      </div>

      <div>
        <p className="text-sm font-medium mb-1">제목</p>
        <input
          value={title}
          onChange={(e) => setTitle(e.target.value)}
          maxLength={100}
          placeholder="제목을 입력해주세요 (2~100자)"
          className="w-full border rounded-lg px-3 py-2 text-sm"
        />
      </div>

      <div>
        <p className="text-sm font-medium mb-1">문의 내용</p>
        <textarea
          value={content}
          onChange={(e) => setContent(e.target.value)}
          maxLength={2000}
          rows={6}
          placeholder="문의 내용을 자세히 입력해주세요 (10~2000자)"
          className="w-full border rounded-lg px-3 py-2 text-sm"
        />
        <p className="text-xs text-gray-400 text-right mt-1">{content.length} / 2000</p>
      </div>

      <div>
        <p className="text-sm font-medium mb-1">스크린샷 첨부 (선택)</p>
        <input
          ref={fileInputRef}
          type="file"
          accept="image/*"
          onChange={handleFileChange}
          className="w-full text-sm file:mr-3 file:py-1 file:px-3 file:rounded-md file:border-0 file:text-xs file:font-medium file:bg-violet-50 file:text-violet-600 hover:file:bg-violet-100"
        />
        {previewUrl && (
          <div className="mt-2 flex items-center gap-2">
            <img src={previewUrl} alt="첨부 미리보기" className="w-20 h-20 rounded-lg border object-cover" />
            <button onClick={handleRemoveImage} className="text-xs text-red-500">
              이미지 제거
            </button>
          </div>
        )}
      </div>

      {formError && <p className="text-xs text-red-500">{formError}</p>}

      <button
        onClick={handleSubmit}
        disabled={isSubmitting}
        className="px-4 py-2 rounded-lg bg-violet-600 text-white text-sm font-medium disabled:opacity-50"
      >
        {isSubmitting ? "등록 중..." : "등록"}
      </button>
    </div>
  );
};