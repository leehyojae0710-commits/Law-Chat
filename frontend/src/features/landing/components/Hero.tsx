import { Link } from "react-router-dom";
import { ChatPreviewCard } from "./ChatPreviewCard";
import { GuestFeatureList } from "./GuestFeatureList";

const badges = ["24시간 상담", "근거 조문 제시", "개인정보 마스킹"];

export const Hero = () => {

  return (
    <section className="bg-violet-50/60 shadow-sm">
      <div className="max-w-[1700px] mx-auto px-10 py-16">
        <div className="grid grid-cols-1 lg:grid-cols-[2fr_0.85fr_0.7fr] gap-6 items-start">
          {/* 좌측 텍스트 */}
          <div>
            <h1 className="text-4xl font-bold leading-[1.3] text-slate-900">
              일상 속 법률 문제,
              <br />
              <span className="text-violet-600">AI가 쉽게 정리해 드립니다.</span>
            </h1>
            <p className="mt-5 text-[15px] text-slate-500 leading-relaxed whitespace-pre-line">
              법률 용어를 몰라도 괜찮습니다. 평소 쓰는 말로 질문하면
              <br/>
              AI가 법률 표현으로 바꿔 근거 조문·판례와 함께 안내합니다.
            </p>

            <div className="mt-7 flex items-center gap-3">
              <Link
                to="/login"
                className="px-5 py-3 rounded-xl bg-violet-600 text-white text-sm font-semibold hover:bg-violet-700 transition-colors"
              >
                로그인하고 질문하기
              </Link>
              <button className="px-5 py-3 rounded-xl border border-slate-200 bg-white text-sm font-medium text-slate-700 hover:bg-slate-50 transition-colors">
                이용 방법 보기
              </button>
            </div>

            <div className="mt-8 flex items-center gap-6">
              {badges.map((b) => (
                <span key={b} className="flex items-center gap-1.5 text-xs text-slate-500">
                  <span className="w-1.5 h-1.5 rounded-full bg-violet-400" />
                  {b}
                </span>
              ))}
            </div>
          </div>

          <ChatPreviewCard />
          <GuestFeatureList />
        </div>
      </div>
    </section>
  );
};
