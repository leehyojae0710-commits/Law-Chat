interface CategoryTabsProps {
  categories: string[];
  active: string;
  onSelect: (category: string) => void;
}

export const CategoryTabs = ({ categories, active, onSelect }: CategoryTabsProps) => {
  return (
    <div className="flex gap-2">
      {categories.map((category) => (
        <button
          key={category}
          onClick={() => onSelect(category)}
          className={`px-4 py-2 rounded-full text-sm border ${
            active === category ? "bg-purple-600 text-white border-purple-600" : "text-gray-600"
          }`}
        >
          {category}
        </button>
      ))}
    </div>
  );
};
