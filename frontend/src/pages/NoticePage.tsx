import { useState } from "react";
import { noticeCategories, notices } from "../features/notice/data";
import { NoticeCategoryTabs } from "../features/notice/components/NoticeCategoryTabs";
import { NoticeListItem } from "../features/notice/components/NoticeListItem";
import { RecentNoticePanel } from "../features/notice/components/RecentNoticePanel";

let count =0;

export const NoticePage = () => {
  const [active, setActive] = useState("전체");

  const filtered =
    active === "전체"
      ? notices
      : notices.filter((n) => n.category === active);

  return (
    <div className="min-h-[845px] bg-violet-50 flex items-start justify-center py-5">
      <div className="mx-auto w-[1200px] px-8 py-10 grid grid-cols-[1fr_350px] gap-6 bg-white rounded-xl shadow-sm">
        
        <div className="space-y-4">
          <h1 className="text-2xl font-bold">공지 사항</h1>

          <NoticeCategoryTabs
            categories={noticeCategories}
            active={active}
            onSelect={setActive}
          />

          <div className="space-y-3">
            {filtered.map((n) => (
              <NoticeListItem key={n.id} notice={n} />
            ))}
          </div>
        </div>

        <RecentNoticePanel />

      </div>
    </div>
  );
};
