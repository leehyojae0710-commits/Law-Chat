import { useCallback, useEffect, useState } from "react";
import { getMyInquiries } from "../../../api/inquiry";
import type { InquiryListItem } from "../types";

const PAGE_SIZE = 10;

export const useMyInquiries = () => {
  const [inquiries, setInquiries] = useState<InquiryListItem[]>([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [isLoading, setIsLoading] = useState(true);

  const refetch = useCallback((targetPage: number) => {
    setIsLoading(true);
    getMyInquiries(targetPage, PAGE_SIZE)
      .then((res) => {
        setInquiries(res.content);
        setTotalPages(res.totalPages);
        setPage(res.number);
      })
      .catch((err) => console.error("내 문의 목록 조회 실패:", err))
      .finally(() => setIsLoading(false));
  }, []);

  useEffect(() => {
    refetch(page);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [page]);

  return { inquiries, isLoading, page, setPage, totalPages, refetch: () => refetch(page) };
};