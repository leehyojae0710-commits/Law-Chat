import { useMyInquiries } from "../hooks/useMyInquiries";
import { formatInquiryDate } from "../types";

interface InquiryListProps {
  onSelect: (inquiryId: number) => void;
}

export const InquiryList = ({ onSelect }: InquiryListProps) => {
  const { inquiries, isLoading, page, setPage, totalPages } = useMyInquiries();

  if (isLoading) return <p className="text-sm text-gray-400">불러오는 중...</p>;

  if (inquiries.length === 0) {
    return (
      <div className="border rounded-xl p-10 text-center mx-auto">
        <p className="text-sm text-gray-400">등록된 문의가 없습니다.</p>
      </div>
    );
  }

  return (
    <div className="space-y-3">
      <div className="border rounded-xl divide-y">
        {inquiries.map((inq) => (
          <button
            key={inq.inquiryId}
            onClick={() => onSelect(inq.inquiryId)}
            className="w-full text-left p-4 hover:bg-gray-50"
          >
            <div className="flex items-center gap-2 mb-1">
              <span
                className={`text-xs px-2 py-0.5 rounded ${
                  inq.status === "PENDING" ? "bg-amber-50 text-amber-600" : "bg-green-50 text-green-600"
                }`}
              >
                {inq.statusLabel}
              </span>
              <p className="font-medium text-sm">{inq.title}</p>
            </div>
            <span className="text-xs text-gray-400">
              {inq.categoryLabel} · {formatInquiryDate(inq.createdAt)}
            </span>
          </button>
        ))}
      </div>

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
              className={`w-7 h-7 rounded-lg text-sm ${
                i === page ? "bg-violet-600 text-white font-medium" : "text-gray-500 hover:bg-violet-50"
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