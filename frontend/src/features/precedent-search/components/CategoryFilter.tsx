import type { CaseCategory } from "../types";

interface CategoryFilterProps {
  categories: CaseCategory[];
  active: CaseCategory;
  onSelect: (category: CaseCategory) => void;
}

export const precedentCategories: CaseCategory[] = [ "전체","민사", "형사", "일반행정", "가사", "세무"];

export const CategoryFilter = ({ categories, active, onSelect }: CategoryFilterProps) => {
  return (
    <div className="flex flex-wrap gap-2">
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
