import { useState } from "react";
import type { SupportTab } from "../features/support/types";
import { SupportSideMenu } from "../features/support/components/SupportSideMenu";
import { InquiryForm } from "../features/support/components/InquiryForm";
import { InquiryList } from "../features/support/components/InquiryList";
import { FindAccountForm } from "../features/support/components/FindAccountForm";

export const SupportPage = () => {
  const [tab, setTab] = useState<SupportTab>("inquiry-form");

  return (
    <div className="max-w-6xl mx-auto px-8 py-10 grid grid-cols-[220px_1fr] gap-8">
      <SupportSideMenu active={tab} onSelect={setTab} />
      <div>
        {tab === "inquiry-form" && <InquiryForm />}
        {tab === "inquiry-list" && <InquiryList inquiries={[]} onSelect={() => {}} />}
        {tab === "find-account" && <FindAccountForm />}
      </div>
    </div>
  );
};
