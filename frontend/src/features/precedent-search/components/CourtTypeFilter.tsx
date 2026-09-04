import { CheckboxGroup } from "./CheckboxGroup";
import type { CourtType } from "../types";

interface CourtTypeFilterProps {
  /** 선택된 법원종류들. 빈 배열 = 전체 법원(필터 없음) */
  selected: CourtType[];
  onChange: (next: CourtType[]) => void;
}

const COURT_TYPES: CourtType[] = ["대법원", "고등법원", "하급심"];

export const CourtTypeFilter = ({ selected, onChange }: CourtTypeFilterProps) => (
  <CheckboxGroup
    label="법원종류"
    items={COURT_TYPES}
    selected={selected}
    onChange={(next) => onChange(next as CourtType[])}
  />
);