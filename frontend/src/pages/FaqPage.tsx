import { useState } from "react";
import { faqCategories, faqItems } from "../features/faq/data";
import { FaqCategoryTabs } from "../features/faq/components/FaqCategoryTabs";
import { FaqAccordionItem } from "../features/faq/components/FaqAccordionItem";
import { RelatedQuestions } from "../features/faq/components/RelatedQuestions";

export const FaqPage = () => {
  const [active, setActive] = useState("전체");

  const filtered = active === "전체" ? faqItems : faqItems.filter((f) => f.category === active);

  return (
    <div className="min-h-screen bg-violet-50 flex items-top justify-center py-5">
      <div className="w-[1500px] mx-auto px-8 py-10 grid grid-cols-[1fr_320px] gap-6 bg-white rounded-xl shadow-sm">
        <div className="space-y-4">
          <FaqCategoryTabs categories={faqCategories} active={active} onSelect={setActive} />
          <div className="space-y-3">
            {filtered.map((item) => (
              <FaqAccordionItem key={item.id} item={item} />
            ))}
          </div>
        </div>
        <RelatedQuestions />
      </div>
    </div>
  );
};
