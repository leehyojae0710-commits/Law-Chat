import { useState } from "react";
import { QuestionCard } from "./QuestionCard";
import { CategoryTabs } from "./CategoryTabs";
import { questions, categories } from "../data";

export const PopularQuestions = () => {
  const [activeCategory, setActiveCategory] = useState<string>("전체");

  const filtered =
    activeCategory === "전체"
      ? questions
      : questions.filter((q) => q.category === activeCategory);

  return (
    <section className="space-y-4 p-10">
      <h2 className="text-lg font-semibold">많은 분들이 이런 질문을 하셨어요</h2>
      <CategoryTabs categories={categories} active={activeCategory} onSelect={setActiveCategory} />
      <div className="grid grid-cols-5 gap-4">
        {filtered.map((q) => (
          <QuestionCard key={q.id} text={q.text} category={q.category} />
        ))}
      </div>
    </section>
  );
};
