import type { Precedent } from "../types";

interface PrecedentResultCardProps {
  precedent: Precedent;
}

export const PrecedentResultCard = ({ precedent }: PrecedentResultCardProps) => {
  return (
    <div className="border rounded-lg p-4">
      <p className="text-xs text-gray-400 mb-1">
        {precedent.court} · {precedent.decidedDate} 선고 · {precedent.caseNumber}
      </p>
      <p className="font-semibold mb-1">{precedent.title}</p>
      <p className="text-sm text-gray-600">{precedent.summary}</p>
    </div>
  );
};
