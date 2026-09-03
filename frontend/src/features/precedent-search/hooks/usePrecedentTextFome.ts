export const toLineBreaks = (text: string) => text.replace(/<br\s*\/?>/gi, "\n");

export const truncateText = (text: string, maxLength: number) =>
  text.length > maxLength ? `${text.slice(0, maxLength)}...` : text;