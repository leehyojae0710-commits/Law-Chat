import type { NoticePopup } from "./types";

// 팝업 테스트용 목데이터. VITE_USE_MOCK_NOTICE=true일 때만 사용됨.
export const mockPopupNotices: NoticePopup[] = [
  {
    popupId: 1,
    noticeId: null,
    title: "[필독] 개인정보처리방침 및 이용약관 개정 안내",
    fileUrl: "https://placehold.co/440x300",
  },
];