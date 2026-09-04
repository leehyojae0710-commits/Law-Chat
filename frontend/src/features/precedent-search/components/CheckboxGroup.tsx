interface CheckboxGroupProps {
  /** 왼쪽에 붙는 라벨 (예: "법원종류", "사건종류") - 2번 이미지의 좌측 라벨 컬럼과 동일한 역할 */
  label: string;
  /** "전체"를 제외한 선택 가능한 항목들 */
  items: string[];
  /** 현재 선택된 항목들. 빈 배열이면 "전체"가 체크된 상태(=필터 없음)로 그려진다 */
  selected: string[];
  onChange: (next: string[]) => void;
}

/**
 * "전체"를 체크하면 나머지가 전부 해제되고, 개별 항목을 하나라도 체크하면 "전체"는 자동 해제된다.
 * 반대로 선택된 항목을 전부 해제하면 다시 "전체"가 체크된 것처럼 보인다(=필터 없음과 동일한 상태).
 */
export const CheckboxGroup = ({ label, items, selected, onChange }: CheckboxGroupProps) => {
  const isAll = selected.length === 0;

  const toggleAll = () => {
    if (!isAll) onChange([]);
  };

  const toggleItem = (item: string) => {
    if (selected.includes(item)) {
      onChange(selected.filter((s) => s !== item));
    } else {
      onChange([...selected, item]);
    }
  };

  return (
    <div className="flex items-start gap-x-4 text-sm">
      <span className="w-16 shrink-0 text-gray-500 pt-1">{label}</span>
      <div className="flex flex-wrap gap-x-4 gap-y-2">
        <label className="flex items-center gap-1.5 cursor-pointer select-none">
          <input
            type="checkbox"
            checked={isAll}
            onChange={toggleAll}
            className="w-4 h-4 accent-purple-600"
          />
          전체
        </label>
        {items.map((item) => (
          <label key={item} className="flex items-center gap-1.5 cursor-pointer select-none">
            <input
              type="checkbox"
              checked={selected.includes(item)}
              onChange={() => toggleItem(item)}
              className="w-4 h-4 accent-purple-600"
            />
            {item}
          </label>
        ))}
      </div>
    </div>
  );
};