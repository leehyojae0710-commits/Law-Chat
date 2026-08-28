import type {
  ChatMessage,
  Conversation,
  ConversationDetail,
  FeedbackReasonOption,
  LegalSource,
} from "./types";

// 새 상담 화면 하단에 노출되는 예시 질문 (스토리보드 "이렇게 질문해 보세요")
export const exampleQuestions = [
  "전세 계약 끝났는데 보증금을 안 줘요",
  "회사에서 갑자기 그만두라고 했어요",
  "중고로 산 물건이 가짜였어요",
  "층간소음 때문에 이웃과 다퉜어요",
];

// 답변 피드백(싫어요) 사유 — 어드민 ADM-07 검수 분류와 매핑됩니다.
export const feedbackReasons: FeedbackReasonOption[] = [
  { code: "TERM_MISMATCH", label: "법률 용어 변환이 정확하지 않음" },
  { code: "WRONG_CATEGORY", label: "법률 분야가 잘못 분류됨" },
  { code: "WRONG_SOURCE", label: "근거 조문이나 판례가 사실과 다름" },
  { code: "OFF_INTENT", label: "질문 의도와 다른 답변" },
  { code: "OTHER", label: "기타" },
];

interface AnswerRule {
  keywords: string[];
  category: Conversation["category"];
  title: string;
  content: string;
  sources: LegalSource[];
}

// 백엔드 AI 파이프라인이 붙기 전까지, 일상어 키워드로 답변을 흉내내는 규칙 목록입니다.
// 실제 연동 시에는 사용되지 않고 features/chat/mockChat.ts 에서만 참조됩니다.
const answerRules: AnswerRule[] = [
  {
    keywords: ["빌려", "빌렸", "대여", "돈을 못", "안 갚"],
    category: "금전/채무",
    title: "대여금 반환청구 상담",
    content:
      "**대여금 반환청구에 해당합니다.**\n\n" +
      "말씀하신 내용이면 빌려준 돈을 돌려받을 근거는 충분해요. 차용증이 없어도 계좌이체 내역과 대화 기록이 대여 사실을 증명하는 자료가 될 수 있어요.\n\n" +
      "- 민법 제598조(소비대차)에 따라 대여금 반환을 청구할 수 있어요\n" +
      "- 소멸시효는 10년이며, 변제기일부터 계산돼요\n" +
      "- 우선 상환을 요청하는 내용증명을 보내는 것이 일반적인 첫 단계예요",
    sources: [
      { lawName: "민법", articleNumber: "제598조", url: "https://law.go.kr" },
      { lawName: "민법", articleNumber: "제162조(소멸시효)", url: "https://law.go.kr" },
    ],
  },
  {
    keywords: ["보증금", "전세", "임대차", "임차"],
    category: "부동산",
    title: "임차보증금 반환청구 상담",
    content:
      "**임차보증금 반환청구에 해당합니다.**\n\n" +
      "임대차가 종료됐는데도 보증금을 돌려받지 못하고 있다면, 임대인에게 지연손해금까지 함께 청구할 수 있어요.\n\n" +
      "- 민법 제618조에 따라 임대차 종료 시 보증금 반환 의무가 발생해요\n" +
      "- 반환이 지체되면 민법 제379조 법정이율에 따른 지연손해금을 청구할 수 있어요\n" +
      "- 계속 거부한다면 임차권등기명령 신청을 검토해보세요",
    sources: [
      { lawName: "민법", articleNumber: "제618조", url: "https://law.go.kr" },
      { lawName: "주택임대차보호법", articleNumber: "제3조의2", url: "https://law.go.kr" },
    ],
  },
  {
    keywords: ["해고", "그만두라", "퇴사", "권고사직"],
    category: "근로/직장",
    title: "부당해고 구제신청 상담",
    content:
      "**부당해고 구제신청 대상일 수 있어요.**\n\n" +
      "정당한 이유 없이 해고를 통보받았다면 노동위원회에 구제신청을 할 수 있어요.\n\n" +
      "- 근로기준법 제23조에 따라 정당한 이유 없는 해고는 금지돼요\n" +
      "- 해고 통보를 받은 날로부터 3개월 이내에 구제신청을 해야 해요\n" +
      "- 해고 통보서, 근로계약서, 급여명세서를 미리 준비해두면 도움이 돼요",
    sources: [
      { lawName: "근로기준법", articleNumber: "제23조", url: "https://law.go.kr" },
      { lawName: "근로기준법", articleNumber: "제28조(구제신청)", url: "https://law.go.kr" },
    ],
  },
  {
    keywords: ["사기", "중고", "가짜", "환불"],
    category: "계약/거래",
    title: "중고거래 사기 고소 상담",
    content:
      "**사기죄 성립 여부를 검토해볼 수 있어요.**\n\n" +
      "물건 대금을 받고 발송하지 않았거나 실제와 다른 물건을 보냈다면 편취의 범의가 인정될 수 있어요.\n\n" +
      "- 형법 제347조(사기)에 따라 처벌 대상이 될 수 있어요\n" +
      "- 계좌이체 내역, 채팅 기록을 증거로 확보해두세요\n" +
      "- 경찰서 사이버수사팀에 고소장을 접수할 수 있어요",
    sources: [{ lawName: "형법", articleNumber: "제347조", url: "https://law.go.kr" }],
  },
  {
    keywords: ["임금", "월급", "체불", "퇴직금"],
    category: "근로/직장",
    title: "임금체불 진정 상담",
    content:
      "**임금체불 진정 대상에 해당해요.**\n\n" +
      "퇴직 후에도 임금이나 퇴직금을 받지 못했다면 고용노동부에 진정을 제기할 수 있어요.\n\n" +
      "- 근로기준법 제36조에 따라 퇴직일로부터 14일 이내 임금을 지급해야 해요\n" +
      "- 미지급 임금 청구권의 소멸시효는 3년이에요\n" +
      "- 사업장 관할 고용노동청에 온라인으로 진정을 접수할 수 있어요",
    sources: [
      { lawName: "근로기준법", articleNumber: "제36조", url: "https://law.go.kr" },
      { lawName: "근로기준법", articleNumber: "제49조(소멸시효)", url: "https://law.go.kr" },
    ],
  },
  {
    keywords: ["이혼", "재산분할", "양육권"],
    category: "이혼/가족",
    title: "이혼 재산분할 상담",
    content:
      "**이혼 시 재산분할 청구가 가능해요.**\n\n" +
      "혼인 중 함께 형성한 재산은 명의와 관계없이 분할 대상이 될 수 있어요.\n\n" +
      "- 민법 제839조의2에 따라 이혼 시 재산분할을 청구할 수 있어요\n" +
      "- 재산분할 청구권은 이혼일로부터 2년 이내에 행사해야 해요\n" +
      "- 혼인 전 재산이나 상속재산은 원칙적으로 분할 대상에서 제외돼요",
    sources: [{ lawName: "민법", articleNumber: "제839조의2", url: "https://law.go.kr" }],
  },
];

const fallbackAnswer: AnswerRule = {
  keywords: [],
  category: "기타",
  title: "새 상담",
  content:
    "**말씀하신 내용을 확인했어요.**\n\n" +
    "조금 더 정확한 안내를 위해 상황을 조금 더 구체적으로 알려주시겠어요? 언제, 누구와, 어떤 일이 있었는지 알려주시면 관련 법 조문과 대응 방법을 안내해드릴게요.\n\n" +
    "- 예: 계약서나 문자, 계좌이체 내역처럼 남아있는 증거가 있는지\n" +
    "- 예: 상대방에게 이미 요청이나 통보를 한 적이 있는지",
  sources: [],
};

export const matchAnswerRule = (question: string): AnswerRule => {
  const found = answerRules.find((rule) => rule.keywords.some((kw) => question.includes(kw)));
  return found ?? fallbackAnswer;
};

const now = Date.now();
const hoursAgo = (h: number) => new Date(now - h * 60 * 60 * 1000).toISOString();
const daysAgo = (d: number) => new Date(now - d * 24 * 60 * 60 * 1000).toISOString();

// 상담 히스토리 목록 mock 데이터 (스토리보드 "상담 히스토리 목록" 화면 기준)
export const mockConversations: Conversation[] = [
  {
    id: "conv-1",
    title: "대여금 반환청구 상담",
    preview: "차용증 없이 대여한 금원의 반환 가능 여부와 소멸시효",
    category: "금전/채무",
    updatedAt: daysAgo(0),
    hasSummary: false,
    isFavorite: false,
  },
  {
    id: "conv-2",
    title: "임차보증금 반환청구 상담",
    preview: "임대차 종료 후 보증금 미반환 시 대응 절차",
    category: "부동산",
    updatedAt: daysAgo(6),
    hasSummary: false,
    isFavorite: true,
  },
  {
    id: "conv-3",
    title: "부당해고 구제신청 상담",
    preview: "해고 통보일 기준 구제신청 기간과 필요 서류",
    category: "근로/직장",
    updatedAt: daysAgo(14),
    hasSummary: true,
    isFavorite: true,
  },
  {
    id: "conv-4",
    title: "중고거래 사기 고소 상담",
    preview: "물품 미발송 사안의 사기죄 성립 요건과 고소 절차",
    category: "계약/거래",
    updatedAt: daysAgo(23),
    hasSummary: false,
    isFavorite: true,
  },
  {
    id: "conv-5",
    title: "임금체불 진정 상담",
    preview: "퇴직 후 미지급 임금의 청구 방법과 소멸시효",
    category: "근로/직장",
    updatedAt: daysAgo(40),
    hasSummary: false,
    isFavorite: true,
  },
  {
    id: "conv-6",
    title: "층간소음 손해배상 상담",
    preview: "반복된 층간소음에 대한 손해배상 청구 가능 여부",
    category: "기타",
    updatedAt: daysAgo(52),
    hasSummary: false,
    isFavorite: false,
  },
];

// conv-1(대여금 반환청구) 대화 내용 — "이어서 상담" 화면과 동일하게 맞춰둔 mock 메시지
export const mockConversationMessages: Record<string, ChatMessage[]> = {
  "conv-1": [
    {
      id: "msg-1",
      role: "user",
      content:
        "친구한테 500만원 빌려줬는데 1년 넘게 안 갚고 연락도 피해요.\n차용증은 안 썼고 계좌이체 내역이랑 카톡 대화는 있어요.",
      createdAt: hoursAgo(2),
    },
    {
      id: "msg-2",
      role: "assistant",
      content:
        "말씀하신 내용이면 친구분께 빌려준 돈을 돌려받을 근거는 충분해요.\n" +
        "차용증이 없어도 계좌이체 내역과 카톡 대화가 대여 사실을 증명하는 자료가 될 수 있어요.\n" +
        "우선 상환을 요청하는 문자나 카톡을 남겨서 기록을 만들어 두시는 걸 권해드려요.",
      createdAt: hoursAgo(2),
    },
  ],
};

export const getConversationDetail = (id: string): ConversationDetail | undefined => {
  const conversation = mockConversations.find((c) => c.id === id);
  if (!conversation) return undefined;
  return { ...conversation, messages: mockConversationMessages[id] ?? [] };
};
