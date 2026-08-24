export const SavedPrecedentPanel = () => {
  return (
    <div className="border rounded-xl p-4">
      <p className="font-semibold mb-1">저장한 판례</p>
      <p className="text-xs text-gray-400 mb-4">로그인하면 검색 결과를 저장할 수 있어요</p>
      {/* TODO: 로그인 여부에 따라 목록 또는 로그인 유도 UI */}
    </div>
  );
};
