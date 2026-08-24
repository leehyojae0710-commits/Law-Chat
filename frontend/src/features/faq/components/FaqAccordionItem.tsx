import { useState } from "react";
import type { FaqItem } from "../types";

interface FaqAccordionItemProps {
  item: FaqItem;
}

export const FaqAccordionItem = ({ item }: FaqAccordionItemProps) => {
  const [open, setOpen] = useState(false);

  return (
    <div className="border rounded-lg p-4">
      <button
        onClick={() => setOpen(!open)}
        className="w-full flex justify-between items-center text-left font-medium"
      >
        {item.question}
        <span>{open ? "−" : "+"}</span>
      </button>
      {open && item.answer && (
        <p className="mt-3 text-sm text-gray-600 border-t pt-3">{item.answer}</p>
      )}
    </div>
  );
};
