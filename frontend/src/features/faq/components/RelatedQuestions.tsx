import { relatedQuestions } from "../data";

export const RelatedQuestions = () => {
  return (
    <div className="border rounded-xl p-4">
      <p className="font-semibold mb-1">이런 질문도 있어요</p>
      <p className="text-xs text-gray-400 mb-3">임베딩 유사도 기반 추천</p>
      <div className="space-y-3">
        {relatedQuestions.map((q) => (
          <div key={q.id} className="border-b pb-3">
            <p className="text-sm mb-1">{q.text}</p>
            <span className="text-xs bg-purple-50 text-purple-600 rounded-full px-2 py-1">
              {q.tag} 유사도 {q.similarity}
            </span>
          </div>
        ))}
      </div>
    </div>
  );
};
