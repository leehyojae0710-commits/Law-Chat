export const formatDate = (isoString: string): string => {
  return new Date(isoString).toLocaleString("ko-KR", {
    month: "short",
    day: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
};
