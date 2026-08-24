import { serviceFeatures } from "../features/service-features/data";
import { FeatureCard } from "../features/service-features/components/FeatureCard";
import { Disclaimer } from "../components/layout/Disclaimer";

export const ServiceFeaturesPage = () => {
  return (
    <div className="min-h-screen bg-violet-50 flex items-top justify-center py-5">
      <div className="max-w-6xl h-[650px] mx-auto px-8 py-10 space-y-8 bg-white rounded-xl shadow-sm">
        <div>
          <h1 className="text-2xl font-bold">주요 기능</h1>
          <p className="text-gray-600 mt-1">LawChat이 제공하는 6가지 핵심 기능을 확인해 보세요.</p>
        </div>
        <div className="grid grid-cols-3 gap-4">
          {serviceFeatures.map((f) => (
            <FeatureCard key={f.id} order={f.order} title={f.title} description={f.description} />
          ))}
        </div>
        <Disclaimer />
      </div>
    </div>
  );
};
