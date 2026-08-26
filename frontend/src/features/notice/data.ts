import type { Notice } from "./types";

export const noticeCategories = ["전체", "서비스 업데이트", "점검 안내", "이용약관"];

export const notices: Notice[] = [
  { id: "n1", category: "이용약관", title: "[필독] 개인정보처리방침 및 이용약관 개정 안내",detail:"안녕하세요 이용약관 관련 테스트 입니다", date: "2026.08.10", pinned: true },
  { id: "n2", category: "서비스 업데이트", title: "AI 답변에 판례 원문 링크가 함께 제공됩니다",detail:"", date: "2026.08.05" },
  { id: "n3", category: "점검 안내", title: "8월 14일(금) 새벽 서버 정기 점검 예정",detail:"", date: "2026.08.09" },
  { id: "n4", category: "점검 안내", title: "8월 14일(금) 새벽 서버 정기 점검 예정",detail:"", date: "2026.08.09" },
  { id: "n5", category: "점검 안내", title: "8월 14일(금) 새벽 서버 정기 점검 예정",detail:"", date: "2026.08.09" },
  { id: "n6", category: "점검 안내", title: "8월 14일(금) 새벽 서버 정기 점검 예정",detail:"", date: "2026.08.09" },
  { id: "n7", category: "점검 안내", title: "8월 14일(금) 새벽 서버 정기 점검 예정",detail:"", date: "2026.08.09" },
  { id: "n8", category: "점검 안내", title: "8월 14일(금) 새벽 서버 정기 점검 예정",detail:"", date: "2026.08.09" },
  { id: "n9", category: "점검 안내", title: "8월 14일(금) 새벽 서버 정기 점검 예정",detail:"", date: "2026.08.09" },
  { id: "n10", category: "점검 안내", title: "8월 14일(금) 새벽 서버 정기 점검 예정",detail:"", date: "2026.08.09" },
  { id: "n11", category: "점검 안내", title: "8월 14일(금) 새벽 서버 정기 점검 예정",detail:"", date: "2026.08.09" },
];

// 팝업 테스트용 목데이터. VITE_USE_MOCK_NOTICE=true일 때만 사용됨.
export const mockPopupNotices: Notice[] = [
  {
    id: "n1",
    category: "이용약관",
    title: 
    "[필독] 개인정보처리방침 및 이용약관 개정 안내\n안녕하세요 이용약관 관련해서 팝업 테스트 중입니다",
    detail:"안녕하세요 이용약관 관련 테스트 입니다",
    date: "2026.08.10",
    pinned: true,
    popupStartDate: "2026-08-01",
    popupEndDate: "2026-12-31",
  },
  {
    id: "n3",
    category: "점검 안내",
    title: "8월 14일(금) 새벽 서버 정기 점검 예정",
    detail:"",
    date: "2026.08.09",
    popupStartDate: "2026-08-01",
    popupEndDate: "2026-12-31",
    imageUrl: "https://placehold.co/440x300",
  },
];