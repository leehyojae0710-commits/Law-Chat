export interface Notice {
  id: string;
  category: string;
  title: string;
  date: string;
  pinned?: boolean;
  popupStartDate?: string; // 팝업 노출 시작일 (YYYY-MM-DD)
  popupEndDate?: string;   // 팝업 노출 종료일 (YYYY-MM-DD)
  imageUrl?: string;
}
