import { useState } from "react";
import type { SupportTab } from "../features/support/types";
import { SupportSideMenu } from "../features/support/components/SupportSideMenu";
import { InquiryForm } from "../features/support/components/InquiryForm";
import { InquiryList } from "../features/support/components/InquiryList";
import { InquiryThread } from "../features/support/components/InquiryThread";
import { FindAccountForm } from "../features/support/components/FindAccountForm";

export const SupportPage = () => {
  const [tab, setTab] = useState<SupportTab>("inquiry-form");
  const [selectedInquiryId, setSelectedInquiryId] = useState<number | null>(null);

  const handleTabSelect = (next: SupportTab) => {
    setSelectedInquiryId(null); // 다른 탭으로 이동하면 상세 보기 상태 초기화
    setTab(next);
  };

  return (
    <div className="max-w-6xl mx-auto px-8 py-10 grid grid-cols-[220px_1fr] gap-8">
      <SupportSideMenu active={tab} onSelect={handleTabSelect} />
      <div>
        {tab === "inquiry-form" && <InquiryForm onSubmitted={() => handleTabSelect("inquiry-list")} />}
        {tab === "inquiry-list" &&
          (selectedInquiryId === null ? (
            <InquiryList onSelect={setSelectedInquiryId} />
          ) : (
            <InquiryThread
              inquiryId={selectedInquiryId}
              onBack={() => setSelectedInquiryId(null)}
              onDeleted={() => setSelectedInquiryId(null)}
            />
          ))}
        {tab === "find-account" && <FindAccountForm />}
      </div>
    </div>
  );
};