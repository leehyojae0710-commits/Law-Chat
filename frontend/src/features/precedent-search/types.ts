export interface Precedent {
  id: string;
  court: string;
  decidedDate: string;
  caseNumber: string;
  title: string;
  summary: string;
  category: string;
  similarity?: number;
}
