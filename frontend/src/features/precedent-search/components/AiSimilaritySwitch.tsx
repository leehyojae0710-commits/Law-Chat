interface AiSimilaritySwitchProps {
  checked: boolean;
  onChange: (next: boolean) => void;
}

export const AiSimilaritySwitch = ({ checked, onChange }: AiSimilaritySwitchProps) => {
  return (
    <div className="flex min-w-0 items-center justify-between border rounded-lg p-4">
      <div>
        <p className="text-sm font-medium">AI 유사 단어 검색</p>
        <p className="text-xs text-gray-500">
          정확히 일치하는 단어가 아니어도 AI가 유사한 표현이 포함된 판례를 함께 찾아드려요
        </p>
      </div>
      <button
        onClick={() => onChange(!checked)}
        className={`w-11 h-6 rounded-full transition ${checked ? "bg-purple-600" : "bg-gray-300"}`}
      >
        <span
          className={`block w-5 h-5 bg-white rounded-full transition-transform ${
            checked ? "translate-x-5" : "translate-x-0.5"
          }`}
        />
      </button>
    </div>
  );
};
