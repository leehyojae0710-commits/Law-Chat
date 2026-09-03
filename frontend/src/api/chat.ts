import axios, { type InternalAxiosRequestConfig, type AxiosError } from "axios";
import type {
  ChatMessage,
  Conversation,
  ConversationDetail,
  ConversationCategory,
  FeedbackReasonCode,
  FeedbackType,
  SendMessageResult,
} from "../features/chat/types";

// 백엔드 API 기본 URL (환경변수 또는 로컬 8080)
// 주의: .env에는 VITE_API_URL만 정의돼 있음 (api/client.ts와 이름 맞춤).
// 예전에 VITE_API_BASE_URL로 돼있던 건 .env에 없는 이름이라 항상 기본값(localhost)만 타고 있었음.
const BASE_URL = import.meta.env.VITE_API_URL || "http://localhost:8080/api";

export const apiClient = axios.create({
  baseURL: BASE_URL,
  headers: {
    "Content-Type": "application/json",
  },
  timeout: 120000, // LLM 답변 대기 시간 고려 120초(2분) 설정
  withCredentials: true,
});

/**
 * 1. 요청 인터셉터: 매 API 요청마다 로컬스토리지의 토큰을 헤더에 주입
 */
apiClient.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    // sessionStorage와 localStorage 양쪽에서 토큰을 모두 탐색 (client.ts와 동일하게 맞춤 —
    // authStore.ts가 실제로는 sessionStorage에 토큰을 저장하고 있어서, localStorage만 보면
    // 로그인은 성공해도 이후 요청엔 토큰이 안 실려 401이 났었습니다.)
    const token =
      sessionStorage.getItem("accessToken") ||
      localStorage.getItem("accessToken") ||
      sessionStorage.getItem("token") ||
      localStorage.getItem("token") ||
      sessionStorage.getItem("jwtToken") ||
      localStorage.getItem("jwtToken");

    if (token && config.headers) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error: AxiosError) => {
    return Promise.reject(error);
  }
);

/**
 * 2. 응답 인터셉터: 401(토큰 만료/인증 실패) 발생 시 처리
 */
apiClient.interceptors.response.use(
  (response) => {
    return response;
  },
  async (error: AxiosError) => {
    if (error.response?.status === 401) {
      console.warn("[Auth] 401 Unauthorized: 인증 토큰이 만료되었거나 유효하지 않습니다.");

      // 로그인/회원가입 요청 자체가 아니라면 만료된 토큰 정리 후 로그인 유도 가능
      const isAuthRequest = error.config?.url?.includes("/auth/");
      if (!isAuthRequest) {
        sessionStorage.removeItem("accessToken");
        sessionStorage.removeItem("token");
        sessionStorage.removeItem("jwtToken");
        localStorage.removeItem("accessToken");
        localStorage.removeItem("token");
        localStorage.removeItem("jwtToken");

        // 필요 시 로그인 페이지로 리다이렉트
        // window.location.href = "/login";
      }
    }
    return Promise.reject(error);
  }
);

export default apiClient;

/**
 * 아래부터는 ChatController.java(/api/chat/*)와 실제로 통신하는 함수들입니다.
 * 이 파일에 axios 클라이언트 설정만 있고 이 함수들이 없어서
 * useChat.ts / useConversations.ts에서 import가 깨져 있었습니다 (빌드 실패 원인).
 */

// ---- 백엔드 응답 DTO (ChatSessionResponse / ChatMessageResponse 1:1) ----
interface ChatSessionResponseDto {
  sessionId: number;
  title: string;
  isFavorite: boolean;
  createdAt: string;
  updatedAt: string;
}

interface ChatMessageResponseDto {
  id: number;
  role: "user" | "assistant";
  content: string;
  sources?: { lawName: string; articleNumber: string; url: string }[];
  createdAt: string;
}

// 백엔드 ChatSessionResponse에는 아직 preview/category/hasSummary 필드가 없어서
// 프론트 Conversation 타입에 맞춰 기본값을 채워 넣습니다.
// (백엔드에 이 필드들이 추가되면 여기 매핑만 고치면 됩니다 — 지금은 목록에서
//  미리보기 문구나 카테고리 배지가 정확하지 않을 수 있어요.)
const toConversation = (dto: ChatSessionResponseDto): Conversation => ({
  id: String(dto.sessionId),
  title: dto.title,
  preview: "",
  category: "기타" as ConversationCategory,
  updatedAt: dto.updatedAt,
  hasSummary: false,
  isFavorite: dto.isFavorite,
});

const toChatMessage = (dto: ChatMessageResponseDto): ChatMessage => ({
  id: String(dto.id),
  role: dto.role,
  content: dto.content,
  sources: dto.sources,
  createdAt: dto.createdAt,
});

export const listConversationsApi = async (params?: {
  favorite?: boolean;
  keyword?: string;
}): Promise<Conversation[]> => {
  const url = params?.favorite ? "/chat/sessions/favorites" : "/chat/sessions";
  const { data } = await apiClient.get<ChatSessionResponseDto[]>(url);
  let list = data.map(toConversation);
  if (params?.keyword?.trim()) {
    const kw = params.keyword.trim();
    list = list.filter((c) => c.title.includes(kw));
  }
  return list;
};

export const getConversation = async (id: string): Promise<ConversationDetail> => {
  // 세션 목록에는 있지만 "세션 하나만 조회"하는 엔드포인트가 백엔드에 없어서
  // 목록에서 찾는 방식으로 우회합니다 (세션 개수가 많아지면 전용 GET /sessions/{id}가 필요해요).
  const [{ data: sessions }, { data: messages }] = await Promise.all([
    apiClient.get<ChatSessionResponseDto[]>("/chat/sessions"),
    apiClient.get<ChatMessageResponseDto[]>(`/chat/sessions/${id}/messages`),
  ]);
  const session = sessions.find((s) => String(s.sessionId) === id);
  if (!session) throw new Error("존재하지 않는 상담이에요");
  return { ...toConversation(session), messages: messages.map(toChatMessage) };
};

export const startConversation = async (content: string): Promise<SendMessageResult> => {
  // 백엔드에 "세션 생성 + 첫 메시지 전송"을 한 번에 처리하는 엔드포인트가 없어서
  // POST /sessions(제목만) -> POST /sessions/{id}/messages(내용) 두 번 호출로 처리합니다.
  const title = content.length > 20 ? `${content.slice(0, 20)}…` : content;
  const { data: session } = await apiClient.post<ChatSessionResponseDto>(
    "/chat/sessions",
    null,
    { params: { title } }
  );
  const { data: assistantDto } = await apiClient.post<ChatMessageResponseDto>(
    `/chat/sessions/${session.sessionId}/messages`,
    { content }
  );
  // 백엔드는 AI 답변만 돌려주고 방금 보낸 사용자 메시지는 다시 안 돌려줘서, 화면에는 로컬에서 만들어 붙입니다.
  const userMessage: ChatMessage = {
    id: `local-${Date.now()}`,
    role: "user",
    content,
    createdAt: new Date().toISOString(),
  };
  return {
    conversation: toConversation(session),
    userMessage,
    assistantMessage: toChatMessage(assistantDto),
  };
};

export const sendMessage = async (
  conversationId: string,
  content: string
): Promise<{ userMessage: ChatMessage; assistantMessage: ChatMessage }> => {
  const { data: assistantDto } = await apiClient.post<ChatMessageResponseDto>(
    `/chat/sessions/${conversationId}/messages`,
    { content }
  );
  const userMessage: ChatMessage = {
    id: `local-${Date.now()}`,
    role: "user",
    content,
    createdAt: new Date().toISOString(),
  };
  return { userMessage, assistantMessage: toChatMessage(assistantDto) };
};

export const toggleFavoriteConversationApi = async (
  id: string,
  _next: boolean
): Promise<Conversation> => {
  // 백엔드가 "토글" 방식(호출할 때마다 반전)이라 next 값은 프론트 낙관적 업데이트용으로만 쓰고,
  // 실제 요청에는 반영하지 않습니다.
  const { data } = await apiClient.patch<ChatSessionResponseDto>(`/chat/sessions/${id}/favorite`);
  return toConversation(data);
};

export const deleteConversationApi = async (id: string): Promise<void> => {
  await apiClient.delete(`/chat/sessions/${id}`);
};

export const submitMessageFeedback = async (
  messageId: string,
  payload: { type: FeedbackType; reasonCode?: FeedbackReasonCode; detail?: string }
): Promise<void> => {
  // 백엔드 FeedbackRequest는 { isPositive, reason } 형태라 프론트의 type/reasonCode/detail을 합쳐서 보냅니다.
  await apiClient.post(`/chat/messages/${messageId}/feedback`, {
    isPositive: payload.type === "like",
    reason: [payload.reasonCode, payload.detail].filter(Boolean).join(": ") || undefined,
  });
};