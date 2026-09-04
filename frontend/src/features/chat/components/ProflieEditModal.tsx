import { useState, useRef } from "react";
import { useUserStore } from "../../../store/userStore";
import { useThemeStore } from "../../../store/themsStore";

interface ProfileEditModalProps {
    onClose: () => void;
}

export const ProfileEditModal = ({ onClose }: ProfileEditModalProps) => {
    const { userNickName, userPhone, userProfileImg, updateUserProfile, updateUserProfileImg } =
        useUserStore();

    const [nickname, setNickname] = useState(userNickName);
    const [phone, setPhone] = useState(userPhone);
    const [previewUrl, setPreviewUrl] = useState(userProfileImg);
    const [selectedFile, setSelectedFile] = useState<File | null>(null);
    const [isSaving, setIsSaving] = useState(false);
    const fileInputRef = useRef<HTMLInputElement>(null);
    const { isDark, toggleTheme } = useThemeStore();

    const handleImageClick = () => {
        fileInputRef.current?.click();
    };

    const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        const file = e.target.files?.[0];
        if (!file) return;
        setSelectedFile(file);
        setPreviewUrl(URL.createObjectURL(file));
    };

    const handleSave = async () => {
        setIsSaving(true);
        try {
            if (nickname !== userNickName || phone !== userPhone) {
                await updateUserProfile({
                    newNickName: nickname !== userNickName ? nickname : undefined,
                    newPhone: phone !== userPhone ? phone : undefined,
                });
            }
            if (selectedFile) {
                await updateUserProfileImg(selectedFile);
            }
            onClose();
        } finally {
            setIsSaving(false);
        }
    };

    return (
        // 배경 오버레이: 클릭해도 닫히지 않도록 별도 핸들러를 걸지 않음
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40">
            {/* 모달 박스: 이벤트 버블링 막아서 오버레이 클릭과 분리 */}
            <div
                className="w-80 rounded-xl bg-white p-5 shadow-lg"
                onClick={(e) => e.stopPropagation()}
            >
                <div className="mb-4 flex items-center justify-between">
                    <h2 className="text-sm font-semibold text-slate-900">프로필 수정</h2>
                    <button
                        onClick={onClose}
                        className="text-slate-400 hover:text-slate-600"
                        aria-label="닫기"
                    >
                        ✕
                    </button>
                </div>

                <div className="mb-4 flex flex-col items-center gap-2">
                    <button
                        type="button"
                        onClick={handleImageClick}
                        className="h-16 w-16 overflow-hidden rounded-full bg-slate-200 text-xs font-medium text-slate-600"
                    >
                        {previewUrl ? (
                            <img src={previewUrl} alt="프로필 미리보기" className="h-full w-full object-cover" />
                        ) : (
                            <span className="flex h-full w-full items-center justify-center">{nickname.slice(0, 2)}</span>
                        )}
                    </button>
                    <input
                        ref={fileInputRef}
                        type="file"
                        accept="image/*"
                        className="hidden"
                        onChange={handleFileChange}
                    />
                    <button
                        type="button"
                        onClick={handleImageClick}
                        className="text-xs text-violet-600 hover:underline"
                    >
                        사진 변경
                    </button>
                </div>

                <div className="mb-3">
                    <label className="mb-1 block text-xs font-medium text-slate-500">닉네임</label>
                    <input
                        type="text"
                        value={nickname}
                        onChange={(e) => setNickname(e.target.value)}
                        className="w-full rounded-lg border border-slate-200 px-3 py-2 text-sm focus:border-violet-500 focus:outline-none"
                    />
                </div>

                <div className="mb-5">
                    <label className="mb-1 block text-xs font-medium text-slate-500">전화번호</label>
                    <input
                        type="text"
                        value={phone}
                        onChange={(e) => setPhone(e.target.value)}
                        className="w-full rounded-lg border border-slate-200 px-3 py-2 text-sm focus:border-violet-500 focus:outline-none"
                    />
                </div>

                <div className="mb-5 flex items-center justify-between rounded-lg border border-slate-200 px-3 py-2.5">
                    <span className="text-sm text-slate-700">다크 모드</span>
                    <button
                        type="button"
                        role="switch"
                        aria-checked={isDark}
                        onClick={toggleTheme}
                        className={`relative h-6 w-11 shrink-0 rounded-full transition-colors ${isDark ? "bg-violet-600" : "bg-slate-300"
                            }`}
                    >
                        <span
                            className={`absolute left-0.5 top-0.5 h-5 w-5 rounded-full bg-white shadow transition-transform 
                                ${isDark ? "translate-x-5" : "translate-x-0"
                                }`}
                        />
                    </button>
                </div>

                <div className="flex justify-end gap-2">
                    <button
                        onClick={onClose}
                        className="rounded-lg px-3 py-1.5 text-sm text-slate-500 hover:bg-slate-50"
                    >
                        취소
                    </button>
                    <button
                        onClick={handleSave}
                        disabled={isSaving}
                        className="rounded-lg bg-violet-600 px-3 py-1.5 text-sm font-medium text-white hover:bg-violet-700 disabled:opacity-50"
                    >
                        {isSaving ? "저장 중..." : "저장"}
                    </button>
                </div>
            </div>
        </div>
    );
};