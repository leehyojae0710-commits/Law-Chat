interface QuestionCardProps {
  text: string;
  category: string;
  onClick?: () => void;
}

export const QuestionCard = ({ text, category, onClick }: QuestionCardProps) => {
  return (
    <div className="rounded-lg border p-4 shadow-sm">
      <p className="text-sm text-gray-700">{text}</p>
      <button
        onClick={onClick}
        className="mt-3 rounded-full bg-purple-50 px-3 py-1 text-xs text-purple-600"
      >
        {category}
      </button>
    </div>
  );
};
