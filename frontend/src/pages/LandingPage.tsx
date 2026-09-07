import { Hero } from "../features/landing/components/Hero";
import { PopularQuestions } from "../features/landing/components/PopularQuestions";
import { Disclaimer } from "../components/layout/Disclaimer";

export const LandingPage = () => {
  return (
      <div className="bg-white min-h-screen">
        <Hero />
        <PopularQuestions />
        <Disclaimer />
      </div>
  );
};
