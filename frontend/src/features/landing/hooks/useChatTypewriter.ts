import { useEffect, useState } from "react";
import { chatExamples } from "../data";

const TYPE_SPEED = 40;
const DELETE_SPEED = 20;
const HOLD_TIME = 2200;
const NEXT_DELAY = 400;

export const useChatTypewriter = () => {
  const [exampleIndex, setExampleIndex] = useState(0);
  const [segmentIndex, setSegmentIndex] = useState(0);
  const [charCount, setCharCount] = useState(0);
  const [phase, setPhase] = useState<"typing" | "holding" | "deleting">("typing");

  const example = chatExamples[exampleIndex];
  const segments = [example.question, example.answerTitle, ...example.bullets];

  useEffect(() => {
    const currentText = segments[segmentIndex] ?? "";

    if (phase === "typing") {
      if (charCount < currentText.length) {
        const t = setTimeout(() => setCharCount((c) => c + 1), TYPE_SPEED);
        return () => clearTimeout(t);
      }
      if (segmentIndex < segments.length - 1) {
        const t = setTimeout(() => {
          setSegmentIndex((i) => i + 1);
          setCharCount(0);
        }, 250);
        return () => clearTimeout(t);
      }
      const t = setTimeout(() => setPhase("holding"), 0);
      return () => clearTimeout(t);
    }

    if (phase === "holding") {
      const t = setTimeout(() => setPhase("deleting"), HOLD_TIME);
      return () => clearTimeout(t);
    }

    if (phase === "deleting") {
      if (charCount > 0) {
        const t = setTimeout(() => setCharCount((c) => c - 1), DELETE_SPEED);
        return () => clearTimeout(t);
      }
      if (segmentIndex > 0) {
        const t = setTimeout(() => {
          setSegmentIndex((i) => i - 1);
          setCharCount(segments[segmentIndex - 1].length);
        }, 100);
        return () => clearTimeout(t);
      }
      const t = setTimeout(() => {
        setExampleIndex((i) => (i + 1) % chatExamples.length);
        setSegmentIndex(0);
        setCharCount(0);
        setPhase("typing");
      }, NEXT_DELAY);
      return () => clearTimeout(t);
    }
  }, [phase, charCount, segmentIndex, segments, exampleIndex]);

  const displayed = segments.map((text, i) => {
    if (i < segmentIndex) return text;
    if (i === segmentIndex) return text.slice(0, charCount);
    return "";
  });

  return {
    question: displayed[0],
    answerTitle: displayed[1],
    bullets: displayed.slice(2),
  };
};