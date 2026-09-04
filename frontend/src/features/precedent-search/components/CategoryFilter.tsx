import { CheckboxGroup } from "./CheckboxGroup";
import type { CaseCategory } from "../types";

interface CategoryFilterProps {
  /** "전체" 제외 선택된 사건종류들. 빈 배열 = 전체(필터 없음) */
  selected: CaseCategory[];
  onChange: (next: CaseCategory[]) => void;
}

// DB(case_type_name)에 실제로 들어있는 값 그대로 사용한다.
// "선거,특별"은 2번 이미지엔 없는 카테고리지만 검색 자체는 계속 지원해야 해서 뒤에 추가했다.
export const precedentCategories: Exclude<CaseCategory, "전체">[] = [
  "민사",
  "형사",
  "일반행정",
  "가사",
  "세무",
  "특허",
  "선거,특별",
];

export const CategoryFilter = ({ selected, onChange }: CategoryFilterProps) => (
  <CheckboxGroup
    label="사건종류"
    items={precedentCategories}
    selected={selected}
    onChange={(next) => onChange(next as CaseCategory[])}
  />
);