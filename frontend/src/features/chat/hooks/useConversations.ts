import { useCallback, useEffect, useState } from "react";
import type { Conversation } from "../types";
import {
  mockDeleteConversation,
  mockListConversations,
  mockToggleFavoriteConversation,
} from "../mockChat";
import {
  deleteConversationApi,
  listConversationsApi,
  toggleFavoriteConversationApi,
} from "../../../api/chat";

// 백엔드가 준비되기 전에는 .env의 VITE_USE_MOCK_CHAT=true 로 mock 데이터를 사용합니다.
// 백엔드 연동 시 .env에서 VITE_USE_MOCK_CHAT=false 로만 바꾸면 실제 API를 호출합니다.
const USE_MOCK_CHAT = import.meta.env.VITE_USE_MOCK_CHAT === "true";

interface UseConversationsOptions {
  favoriteOnly?: boolean;
}

export const useConversations = ({ favoriteOnly = false }: UseConversationsOptions = {}) => {
  const [conversations, setConversations] = useState<Conversation[]>([]);
  const [keyword, setKeyword] = useState("");
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    setIsLoading(true);
    setError(null);
    try {
      const params = { favorite: favoriteOnly, keyword: keyword || undefined };
      const list = USE_MOCK_CHAT ? await mockListConversations(params) : await listConversationsApi(params);
      setConversations(list);
    } catch {
      setError("상담 히스토리를 불러오지 못했어요.");
    } finally {
      setIsLoading(false);
    }
  }, [favoriteOnly, keyword]);

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [favoriteOnly, keyword]);

  const toggleFavorite = useCallback(
    async (id: string, next: boolean) => {
      setConversations((prev) =>
        favoriteOnly && !next
          ? prev.filter((c) => c.id !== id)
          : prev.map((c) => (c.id === id ? { ...c, isFavorite: next } : c))
      );
      try {
        if (USE_MOCK_CHAT) {
          await mockToggleFavoriteConversation(id, next);
        } else {
          await toggleFavoriteConversationApi(id, next);
        }
      } catch {
        load();
      }
    },
    [favoriteOnly, load]
  );

  const removeConversation = useCallback(async (id: string) => {
    setConversations((prev) => prev.filter((c) => c.id !== id));
    try {
      if (USE_MOCK_CHAT) {
        await mockDeleteConversation(id);
      } else {
        await deleteConversationApi(id);
      }
    } catch {
      load();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  return { conversations, keyword, setKeyword, isLoading, error, toggleFavorite, removeConversation, reload: load };
};