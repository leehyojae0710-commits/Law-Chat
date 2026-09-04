import { useEffect, useState } from "react";
import { getCourtNames } from "../../../api/precedents";

interface CourtNameSelectProps {
  /** undefined = "전체" (필터 없음) */
  value: string | undefined;
  onChange: (value: string | undefined) => void;
}

const ALL = "전체";

export const CourtNameSelect = ({ value, onChange }: CourtNameSelectProps) => {
  const [names, setNames] = useState<string[]>([]);

  useEffect(() => {
    let cancelled = false;
    getCourtNames()
      .then((list) => {
        if (!cancelled) setNames(list);
      })
      .catch(() => {
        // 목록 조회가 실패해도 나머지 검색 기능은 계속 동작해야 하므로 조용히 무시한다
      });
    return () => {
      cancelled = true;
    };
  }, []);

  return (
    <label className="flex items-center gap-2 text-sm">
      <span className="w-16 shrink-0 text-gray-500">법원명</span>
      <select
        value={value ?? ALL}
        onChange={(e) => onChange(e.target.value === ALL ? undefined : e.target.value)}
        className="border rounded px-2 py-1 text-sm text-gray-600"
      >
        <option value={ALL}>전체</option>
        {names.map((name) => (
          <option key={name} value={name}>
            {name}
          </option>
        ))}
      </select>
    </label>
  );
};