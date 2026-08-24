import type { Notice } from "../types";

interface NoticeListItemProps {
  notice: Notice;
}

export const NoticeListItem = ({ notice }: NoticeListItemProps) => {
  return (
    <div className="flex items-center justify-between border rounded-lg p-4">
      <div className="flex items-center gap-2">
        {notice.pinned && <span className="text-xs bg-purple-600 text-white px-2 py-1 rounded">고정</span>}
        <span className="text-xs bg-gray-100 px-2 py-1 rounded">{notice.category}</span>
        <p className="font-medium">{notice.title}</p>
      </div>
      <span className="text-xs text-gray-400">{notice.date}</span>
    </div>
  );
};
