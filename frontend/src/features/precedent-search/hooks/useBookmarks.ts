import { useCallback, useEffect, useState } from "react";
import { useAuthStore } from "../../../store/authStore";
import { addBookmark, getBookmarks, removeBookmark } from "../../../api/precedents";
import type { Precedent, PrecedentBookmark } from "../types";

export const useBookmarks = () => {
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated);
  const [bookmarks, setBookmarks] = useState<PrecedentBookmark[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    if (!isAuthenticated) {
      setBookmarks([]);
      return;
    }
    setIsLoading(true);
    setError(null);
    try {
      setBookmarks(await getBookmarks());
    } catch {
      setError("저장한 판례를 불러오지 못했어요.");
    } finally {
      setIsLoading(false);
    }
  }, [isAuthenticated]);

  useEffect(() => {
    load();
  }, [load]);

  const isBookmarked = useCallback(
    (precedentId: Precedent["id"]) => bookmarks.some((b) => b.precedentId === precedentId),
    [bookmarks]
  );

  // 낙관적 업데이트 없이 단순하게: 요청 후 목록을 다시 불러온다 (북마크는 자주 바뀌지 않는 데이터라 충분)
  const toggleBookmark = useCallback(
    async (precedentId: Precedent["id"]) => {
      if (!isAuthenticated) return;
      const bookmarked = isBookmarked(precedentId);
      try {
        if (bookmarked) {
          await removeBookmark(precedentId);
        } else {
          await addBookmark(precedentId);
        }
        await load();
      } catch {
        setError("북마크 처리 중 오류가 발생했어요.");
      }
    },
    [isAuthenticated, isBookmarked, load]
  );

  return { bookmarks, isLoading, error, isBookmarked, toggleBookmark, reload: load };
};
