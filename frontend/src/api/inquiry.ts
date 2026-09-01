import { apiClient } from "./client";
import type { PageResponse } from "../features/notice/types";
import type { InquiryCategory, InquiryDetail, InquiryListItem } from "../features/support/types";

export interface UploadedInquiryFile {
  fileName: string; // 문의 등록 요청의 screenshotUrl 필드에 그대로 넣을 값 (DB 저장용)
  fileUrl: string; // 업로드 직후 미리보기(<img src>)에만 사용
}

export const uploadInquiryScreenshot = async (file: File): Promise<UploadedInquiryFile> => {
  const formData = new FormData();
  formData.append("file", file);
  // Content-Type을 수동으로 지정하지 않음 — axios/브라우저가 FormData를 보고
  // boundary가 포함된 정확한 multipart Content-Type을 자동으로 설정하도록 둠.
  const res = await apiClient.post<UploadedInquiryFile>("/inquiries/upload", formData);
  return res.data;
};

export const createInquiry = async (inquiry: {
  category: InquiryCategory;
  title: string;
  content: string;
  screenshotUrl?: string; // uploadInquiryScreenshot의 fileName 값 (절대 URL 아님)
}): Promise<number> => {
  const res = await apiClient.post<number>("/inquiries", inquiry);
  return res.data;
};

export const getMyInquiries = async (page = 0, size = 10): Promise<PageResponse<InquiryListItem>> => {
  const res = await apiClient.get<PageResponse<InquiryListItem>>("/inquiries/me", {
    params: { page, size },
  });
  return res.data;
};

export const getInquiry = async (inquiryId: number): Promise<InquiryDetail> => {
  const res = await apiClient.get<InquiryDetail>(`/inquiries/${inquiryId}`);
  return res.data;
};

// 답변 등록 전에만 가능. 답변완료 상태에서 호출하면 409 INQUIRY_ALREADY_ANSWERED
export const deleteInquiry = async (inquiryId: number): Promise<void> => {
  await apiClient.delete(`/inquiries/${inquiryId}`);
};