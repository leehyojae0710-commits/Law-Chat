import { useState } from "react";
import { noticeCategories, notices } from "../features/notice/data";
import { NoticeSearchBar } from "../features/notice/components/NoticeSearchBar";
import { NoticeCategoryTabs } from "../features/notice/components/NoticeCategoryTabs";
import { NoticeListItem } from "../features/notice/components/NoticeListItem";
import { RecentNoticePanel } from "../features/notice/components/RecentNoticePanel";

export const NoticePage = () => {
  const [active, setActive] = useState("전체");

  const filtered = active === "전체" ? notices : notices.filter((n) => n.category === active);

  return (
    <div className="max-w-6xl mx-auto px-8 py-10 grid grid-cols-[1fr_320px] gap-6">
      <div className="space-y-4">
        <h1 className="text-2xl font-bold">공지 사항</h1>
        <NoticeSearchBar />
        <NoticeCategoryTabs categories={noticeCategories} active={active} onSelect={setActive} />
        <div className="space-y-3">
          {filtered.map((n) => (
            <NoticeListItem key={n.id} notice={n} />
          ))}
        </div>
      </div>
      <RecentNoticePanel />
    </div>
  );
};
