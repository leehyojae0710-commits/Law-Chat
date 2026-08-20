import type { FaqItem, RelatedQuestion } from "./types";

export const faqCategories = ["전체", "이용 방법", "요금", "개인정보", "AI 답변", "변호사 연결"];

export const faqItems: FaqItem[] = [
  {
    id: "faq1",
    category: "AI 답변",
    question: "AI 답변을 법률 자문으로 믿어도 되나요?",
    answer:
      "본 서비스는 참고용 법률 정보를 제공하며 변호사의 법률 자문을 대체하지 않습니다. 답변에는 항상 근거 조문과 판례 원문 링크를 함께 제공하므로 직접 확인하실 수 있으며, 구체적 사건은 변호사 상담을 권합니다.",
  },
  {
    id: "faq2",
    category: "개인정보",
    question: "상담 내용에 이름이나 계좌번호를 써도 되나요?",
    answer: "상담 내용에 개인 정보를 입력하는 것은 개인정보 보호를 위해 주의가 필요합니다. 가능한 한 구체적인 정보는 피하고, 일반적인 질문을 통해 상담을 진행하는 것을 권장합니다."
  },
  {
    id: "faq3",
    category: "개인정보",
    question: "대화 기록은 얼마나 보관되나요?",
    answer: "대화 기록은 회원탈퇴 시까지 보관되며, 이를 삭제하고자 하는 경우 고객센터로 문의하시기 바랍니다."
  },
  {
    id: "faq4",
    category: "이용 방법",
    question: "AI 답변을 PDF로 다운로드할 수 있나요?",
    answer: "AI 답변은 PDF로 다운로드할 수 있으며, 상담 요약서에는 근거 조문과 판례 원문 링크가 포함되어 있습니다."
  },
  {
    id: "faq5",
    category: "요금",
    question: "AI 상담은 무료인가요?",
    answer: "AI 상담은 무료로 제공되며, 변호사 연결 서비스는 별도의 요금이 발생할 수 있습니다."
  },
  {
    id: "faq6",
    category: "변호사 연결",
    question: "변호사 연결 서비스는 어떻게 이용하나요?",
    answer: "변호사 연결 서비스는 제공하지 않고 있으며, 추후 업데이트를 통해 안내드릴 예정입니다."
  },
];

export const relatedQuestions: RelatedQuestion[] = [
  { id: "r1", text: "돈을 빌려주고 못 받고 있어요", tag: "민법", similarity: 0.91 },
  { id: "r2", text: "차용증 없이 빌려준 돈도 받을 수 있나요", tag: "민법", similarity: 0.88 },
  { id: "r3", text: "내용증명은 어떻게 보내나요", tag: "민법", similarity: 0.76 },
];
