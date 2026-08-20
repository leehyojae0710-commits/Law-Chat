interface InquiryThreadProps {
  inquiryId: string;
}

export const InquiryThread = ({ inquiryId }: InquiryThreadProps) => {
  return (
    <div className="border rounded-xl p-6">
      {/* TODO: inquiryId로 상세 대화 내역 조회 후 렌더링 */}
      <p className="text-sm text-gray-400">문의 #{inquiryId} 상세 내용</p>
    </div>
  );
};
