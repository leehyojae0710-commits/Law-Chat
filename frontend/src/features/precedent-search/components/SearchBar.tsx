export const SearchBar = () => {
  return (
    <div className="flex gap-2">
      <input
        type="text"
        placeholder="사건명·키워드·조문으로 검색해 보세요"
        className="flex-1 border rounded-lg px-4 py-3 text-sm"
      />
      <button className="px-6 py-3 rounded-lg bg-purple-600 text-white text-sm font-medium">
        검색
      </button>
    </div>
  );
};
