import { useCallback, useEffect, useState } from "react";
import { getAdminInquiries, answerInquiry, type AdminInquiryItem } from "../../../api/admin";
import type { InquiryCategory, InquiryStatus } from "../../support/types";

const PAGE_SIZE = 20;

export const useInquiries = () => {
  const [inquiries, setInquiries] = useState<AdminInquiryItem[]>([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [statusFilter, setStatusFilterState] = useState<InquiryStatus | undefined>(undefined);
  const [categoryFilter, setCategoryFilterState] = useState<InquiryCategory | undefined>(undefined);
  const [isLoading, setIsLoading] = useState(true);

  const refetch = useCallback(
    (targetPage: number) => {
      setIsLoading(true);
      getAdminInquiries(statusFilter, categoryFilter, targetPage, PAGE_SIZE)
        .then((res) => {
          setInquiries(res.content);
          setTotalPages(res.totalPages);
          setPage(res.number);
        })
        .catch((err) => console.error("문의 목록 조회 실패:", err))
        .finally(() => setIsLoading(false));
    },
    [statusFilter, categoryFilter]
  );

  useEffect(() => {
    refetch(page);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [page, statusFilter, categoryFilter]);

  // 필터를 바꾸면 지금 보던 페이지 번호가 새 결과 범위 밖일 수 있으니 0페이지로 되돌림
  const setStatusFilter = (next: InquiryStatus | undefined) => {
    setStatusFilterState(next);
    setPage(0);
  };
  const setCategoryFilter = (next: InquiryCategory | undefined) => {
    setCategoryFilterState(next);
    setPage(0);
  };

  const submitAnswer = async (inquiryId: number, answerContent: string) => {
    await answerInquiry(inquiryId, answerContent);
    await refetch(page); // 답변 후 목록 새로고침 (답변대기 -> 답변완료로 상태 갱신)
  };

  return {
    inquiries,
    isLoading,
    page,
    setPage,
    totalPages,
    statusFilter,
    setStatusFilter,
    categoryFilter,
    setCategoryFilter,
    submitAnswer,
  };
};