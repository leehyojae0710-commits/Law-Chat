import { useChatTypewriter } from "../hooks/useChatTypewriter";

export const ChatPreviewCard = () => {
  const { question, answerTitle, bullets } = useChatTypewriter();

  return (
    <div className="border border-slate-200 rounded-xl p-4 shadow-sm bg-white flex flex-col h-full">
      <div className="inline-block self-start bg-gray-100 rounded-full px-3 py-1 text-xs mb-3">
        예시 질문
      </div>
      <p className="text-sm text-gray-600 mb-3 min-h-[20px]">{question}</p>

      <div className="bg-violet-50 border border-violet-100 rounded-lg p-4 flex-1">
        <div className="flex items-start gap-2 mb-2">
          <span
            className={`w-2 h-2 rounded-full bg-violet-500 mt-1.5 shrink-0 transition-opacity duration-200 ${answerTitle ? "opacity-100" : "opacity-0"
              }`}
          />
          <p className="text-sm font-medium text-violet-900 min-h-[20px]">{answerTitle}</p>
        </div>
        <ul className="text-xs text-slate-500 space-y-1 pl-4">
          {bullets.map((b, i) => (
            <li key={i} className="min-h-[16px]">{b && `· ${b}`}</li>
          ))}
        </ul>
      </div>
    </div>
  );
};