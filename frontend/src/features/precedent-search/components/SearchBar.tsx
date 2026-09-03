import { useState } from "react";

interface SearchBarProps {
  onSearch: (keyword: string) => void;
  defaultValue?: string;
}

export const SearchBar = ({ onSearch, defaultValue = "" }: SearchBarProps) => {
  const [value, setValue] = useState(defaultValue);

  const submit = () => onSearch(value.trim());

  return (
    <div className="flex gap-2">
      <input
        type="text"
        value={value}
        onChange={(e) => setValue(e.target.value)}
        onKeyDown={(e) => {
          if (e.key === "Enter") submit();
        }}
        placeholder="사건명·키워드·조문으로 검색해 보세요"
        className="flex-1 min-w-0 border rounded-lg px-4 py-3 text-sm"
      />
      <button
        onClick={submit}
        className="px-6 py-3 rounded-lg bg-purple-600 text-white text-sm font-medium"
      >
        검색
      </button>
    </div>
  );
};
