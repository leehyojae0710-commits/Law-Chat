const badges = ["24시간 상담", "근거 조문 제시", "개인정보 마스킹"];

export const FeatureBadges = () => {
  return (
    <div className="flex gap-6 text-sm text-gray-600">
      {badges.map((b) => (
        <span key={b}>{b}</span>
      ))}
    </div>
  );
};
