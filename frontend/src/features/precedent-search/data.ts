import type { Precedent } from "./types";

export const precedents: Precedent[] = [
  {
    id: "p1",
    court: "대법원",
    decidedDate: "2023.11.09",
    caseNumber: "2023다252341",
    title: "임대차 종료 후 보증금 미반환에 따른 손해배상 범위",
    summary: "임대인이 보증금을 지체 없이 반환하지 않은 경우, 임차인은 지연손해금을 청구할 수 있으며...",
    category: "민법",
  },
  {
    id: "p2",
    court: "서울중앙지방법원",
    decidedDate: "2022.06.15",
    caseNumber: "2022가단98765",
    title: "차용증 없는 금전소비대차의 증명책임과 인정 자료",
    summary: "계좌이체 내역과 문자메시지 등 정황 증거만으로도 소비대차 사실을 인정할 수 있다는...",
    category: "민법",
  },
];
