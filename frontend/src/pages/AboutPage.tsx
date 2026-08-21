import { principles } from "../features/about/data";
import { PrincipleCard } from "../features/about/components/PrincipleCard";
import { StatsBar } from "../features/about/components/StatsBar";
import { Disclaimer } from "../components/layout/Disclaimer";

export const AboutPage = () => {
  return (
    <div className="bg-violet-50 min-h-screen py-5">
    <div className="max-w-6xl mx-auto px-8 py-10 space-y-8 bg-white rounded-xl shadow-sm">
      <div>
        <h1 className="text-2xl font-bold">서비스 소개</h1>
        <p className="text-gray-600 mt-1">
          법률을 몰라도 괜찮습니다. LawChat이 지키는 4가지 원칙입니다.
        </p>
      </div>
      <div className="grid grid-cols-4 gap-4">
        {principles.map((p) => (
          <PrincipleCard key={p.id} order={p.order} title={p.title} description={p.description} />
        ))}
      </div>
      <StatsBar />
      <Disclaimer />
    </div>
    </div>
  );
};
