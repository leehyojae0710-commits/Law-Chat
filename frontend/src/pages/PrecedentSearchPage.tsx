import { useState } from "react";
import { precedentCategories, precedents } from "../features/precedent-search/data";
import { SearchBar } from "../features/precedent-search/components/SearchBar";
import { AiSimilaritySwitch } from "../features/precedent-search/components/AiSimilaritySwitch";
import { CategoryFilter } from "../features/precedent-search/components/CategoryFilter";
import { PrecedentResultCard } from "../features/precedent-search/components/PrecedentResultCard";
import { SavedPrecedentPanel } from "../features/precedent-search/components/SavedPrecedentPanel";
import { Disclaimer } from "../components/layout/Disclaimer";

export const PrecedentSearchPage = () => {
  const [active, setActive] = useState("전체");

  const filtered =
    active === "전체" ? precedents : precedents.filter((p) => p.category === active);

  return (
    <div className="max-w-6xl mx-auto px-8 py-10 grid grid-cols-[1fr_320px] gap-6">
      <div className="space-y-4">
        <div>
          <h1 className="text-2xl font-bold">판례 검색</h1>
          <p className="text-gray-600 mt-1">
            국가법령정보 공동활용 「판례 목록 조회」 API로 정확한 판례를 제공합니다.
          </p>
        </div>
        <SearchBar />
        <AiSimilaritySwitch />
        <CategoryFilter categories={precedentCategories} active={active} onSelect={setActive} />
        <div className="space-y-3">
          {filtered.map((p) => (
            <PrecedentResultCard key={p.id} precedent={p} />
          ))}
        </div>
        <Disclaimer />
      </div>
      <SavedPrecedentPanel />
    </div>
  );
};
