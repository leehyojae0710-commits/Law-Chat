const guestFeatures = ["AI 간편 상담", "FAQ 확인", "상담 예시 보기"];

export const GuestFeatureList = () => {
  return (
    <div className="border border-slate-200 rounded-xl p-6 shadow-sm bg-white flex flex-col h-full">
      <p className="text-sm font-medium mb-3">로그인 없이 가능</p>
      <ul className="space-y-3 text-sm text-gray-600">
        {guestFeatures.map((f) => (
          <li key={f} className="flex items-center gap-2">
            <span className="w-1.5 h-1.5 rounded-full bg-violet-300 shrink-0" />
            {f}
          </li>
        ))}
      </ul>
      <div className="mt-auto pt-4">
        <div className="bg-violet-50 rounded-lg px-3 py-2 text-xs text-violet-700">
          로그인 없이도 대부분의 기능을 바로 체험할 수 있어요.
        </div>
      </div>
    </div>
  );
};
