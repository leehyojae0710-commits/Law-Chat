interface DateRangeFilterProps {
  /** yyyy-MM-dd 또는 빈 문자열(=미지정) */
  from: string;
  to: string;
  onChangeFrom: (value: string) => void;
  onChangeTo: (value: string) => void;
}

/**
 * "최근 1년/3년" 프리셋 대신 선고일자 범위를 직접 입력받는다.
 * decidedDateFrom/decidedDateTo는 백엔드에 이미 있던 파라미터이고 decided_date 컬럼에
 * 인덱스(idx_precedents_decided_date)가 걸려 있어 새로 추가되는 무거운 쿼리 경로는 없다.
 */
export const DateRangeFilter = ({ from, to, onChangeFrom, onChangeTo }: DateRangeFilterProps) => {
  return (
    <div className="flex items-start gap-x-4 text-sm">
      <span className="w-16 shrink-0 text-gray-500 pt-1.5">선고일자</span>
      <div className="flex items-center gap-2">
        <input
          type="date"
          value={from}
          onChange={(e) => onChangeFrom(e.target.value)}
          max={to || undefined}
          className="border rounded-lg px-2 py-1.5 text-sm text-gray-600"
        />
        <span className="text-gray-400">~</span>
        <input
          type="date"
          value={to}
          onChange={(e) => onChangeTo(e.target.value)}
          min={from || undefined}
          className="border rounded-lg px-2 py-1.5 text-sm text-gray-600"
        />
      </div>
    </div>
  );
};
