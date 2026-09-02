import { useEffect, useState } from "react";
import { getNotices } from "../api/notice";
import type { NoticeCategory, NoticeListItem as NoticeListItemType } from "../features/notice/types";
import { NOTICE_CATEGORY_LABELS } from "../features/notice/types";
import { NoticeCategoryTabs } from "../features/notice/components/NoticeCategoryTabs";
import { NoticeListItem } from "../features/notice/components/NoticeListItem";
import { RecentNoticePanel } from "../features/notice/components/RecentNoticePanel";

const CATEGORY_TAB_TO_VALUE: Record<string, NoticeCategory | undefined> = {
  전체: undefined,
  ...Object.fromEntries(
    Object.entries(NOTICE_CATEGORY_LABELS).map(([key, label]) => [label, key as NoticeCategory])
  ),
};

const noticeCategoryTabs = Object.keys(CATEGORY_TAB_TO_VALUE);

export const NoticePage = () => {
  const [active, setActive] = useState("전체");
  const [notices, setNotices] = useState<NoticeListItemType[]>([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    setIsLoading(true);
    getNotices(CATEGORY_TAB_TO_VALUE[active], page, 10)
      .then((res) => {
        setNotices(res.content);
        setTotalPages(res.totalPages);
      })
      .finally(() => setIsLoading(false));
  }, [active, page]);

  const handleSelectCategory = (category: string) => {
    setActive(category);
    setPage(0);
  };

  return (
    <div className="min-h-[845px] bg-violet-50 flex items-start justify-center py-5">
      <div className="mx-auto w-[1200px] px-8 py-10 grid grid-cols-[1fr_350px] gap-6 bg-white rounded-xl shadow-sm">

        <div className="space-y-4">
          <h1 className="text-2xl font-bold">공지 사항</h1>

          <NoticeCategoryTabs
            categories={noticeCategoryTabs}
            active={active}
            onSelect={handleSelectCategory}
          />

          {isLoading ? (
            <p className="text-sm text-gray-400">불러오는 중...</p>
          ) : (
            <div className="space-y-3">
              {notices.map((n) => (
                <NoticeListItem key={n.noticeId} notice={n} />
              ))}
            </div>
          )}

          {totalPages > 1 && (
            <div className="flex justify-center gap-2 pt-4">
              {Array.from({ length: totalPages }, (_, i) => (
                <button
                  key={i}
                  onClick={() => setPage(i)}
                  className={`w-8 h-8 rounded-full text-sm ${
                    page === i ? "bg-purple-600 text-white" : "text-gray-500 hover:bg-gray-100"
                  }`}
                >
                  {i + 1}
                </button>
              ))}
            </div>
          )}
        </div>

        <RecentNoticePanel />

      </div>
    </div>
  );
};