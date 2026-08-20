export interface FaqItem {
  id: string;
  category: string;
  question: string;
  answer: string;
}

export interface RelatedQuestion {
  id: string;
  text: string;
  tag: string;
  similarity: number;
}
