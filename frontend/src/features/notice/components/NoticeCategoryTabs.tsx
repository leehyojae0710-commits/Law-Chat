interface NoticeCategoryTabsProps {
  categories: string[];
  active: string;
  onSelect: (category: string) => void;
}

export const NoticeCategoryTabs = ({ categories, active, onSelect }: NoticeCategoryTabsProps) => {
  return (
    <div className="flex gap-2">
      {categories.map((c) => (
        <button
          key={c}
          onClick={() => onSelect(c)}
          className={`px-4 py-2 rounded-full text-sm border ${
            active === c ? "bg-purple-600 text-white border-purple-600" : "text-gray-600"
          }`}
        >
          {c}
        </button>
      ))}
    </div>
  );
};
