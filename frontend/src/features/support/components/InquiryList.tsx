import type { Inquiry } from "../types";

interface InquiryListProps {
  inquiries: Inquiry[];
  onSelect: (id: string) => void;
}

export const InquiryList = ({ inquiries, onSelect }: InquiryListProps) => {
  return (
    <div className="border rounded-xl divide-y">
      {inquiries.map((inq) => (
        <button key={inq.id} onClick={() => onSelect(inq.id)} className="w-full text-left p-4">
          <p className="font-medium">{inq.title}</p>
          <span className="text-xs text-gray-400">{inq.status} · {inq.date}</span>
        </button>
      ))}
    </div>
  );
};
