export interface LegalSource {
  lawName: string;
  articleNumber: string;
  url: string;
}

export interface ChatMessage {
  id: string;
  role: "user" | "assistant";
  content: string;
  sources?: LegalSource[];
  createdAt: string;
}
