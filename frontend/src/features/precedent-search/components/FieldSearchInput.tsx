import { useEffect, useState } from "react";

interface FieldSearchInputProps {
  label: string;
  placeholder?: string;
  value: string;
  onSearch: (value: string) => void;
}

/**
 * 2번 이미지의 "사건번호 / 사건명 / 참조조문" 입력창처럼 라벨이 위에 붙는 단일 텍스트 검색창.
 * Enter 또는 blur 시점에만 실제 검색을 트리거해서, 타이핑 중에는 요청이 나가지 않는다.
 */
export const FieldSearchInput = ({ label, placeholder, value, onSearch }: FieldSearchInputProps) => {
  const [draft, setDraft] = useState(value);

  // 외부(예: 초기화)에서 value가 바뀌면 입력창도 동기화한다.
  useEffect(() => {
    setDraft(value);
  }, [value]);

  const commit = () => {
    if (draft.trim() !== value) onSearch(draft.trim());
  };

  return (
    <label className="block text-sm">
      <span className="block text-gray-500 mb-1">{label}</span>
      <input
        type="text"
        value={draft}
        onChange={(e) => setDraft(e.target.value)}
        onBlur={commit}
        onKeyDown={(e) => {
          if (e.key === "Enter") commit();
        }}
        placeholder={placeholder}
        className="w-full border rounded-lg px-3 py-2 text-sm"
      />
    </label>
  );
};
