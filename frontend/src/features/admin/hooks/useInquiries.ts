import { useEffect, useState, useCallback } from "react";
import { getInquiries, answerInquiry, type Inquiry } from "../../../api/admin";

export const useInquiries = () => {
  const [inquiries, setInquiries] = useState<Inquiry[]>([]);
  const [isLoading, setIsLoading] = useState(true);

  const refetch = useCallback(() => {
    setIsLoading(true);
    getInquiries()
      .then(setInquiries)
      .finally(() => setIsLoading(false));
  }, []);

  useEffect(() => {
    refetch();
  }, [refetch]);

  const submitAnswer = async (inquiryId: string, answer: string) => {
    await answerInquiry(inquiryId, answer);
    await refetch(); // 답변 후 목록 새로고침 (미답변 -> 답변완료로 상태 갱신)
  };

  return { inquiries, isLoading, submitAnswer };
};
