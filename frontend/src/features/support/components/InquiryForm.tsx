const inquiryTypes = ["버그 제보", "이용 문의", "결제·요금", "계정", "기타"];

export const InquiryForm = () => {
  return (
    <div className="border rounded-xl p-6 space-y-4">
      <div>
        <p className="font-semibold">1:1 문의하기</p>
        <p className="text-sm text-gray-500">궁금한 점이나 불편한 점을 남겨주시면 빠르게 확인할게요</p>
      </div>
      <div>
        <p className="text-sm font-medium mb-2">문의 유형</p>
        <div className="flex gap-2">
          {inquiryTypes.map((t) => (
            <button key={t} className="px-3 py-2 rounded-full border text-sm">
              {t}
            </button>
          ))}
        </div>
      </div>
      {/* TODO: 제목, 발생 시기, 문의 내용, 파일첨부, 등록 버튼 */}
    </div>
  );
};
