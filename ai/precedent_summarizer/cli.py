"""판례 요약 CLI.

사용 예 (실제 학습된 체크포인트가 있는 환경에서):

    python -m precedent_summarizer.cli \\
        --checkpoint "result/2024-02-21 10:36/checkpoint-26606" \\
        --tokenizer "gogamza/kobart-base-v2" \\
        --text-file judgment.txt \\
        --plain
"""
from __future__ import annotations

import argparse
import sys

from .model import explain_for_layperson, load_summarizer, summarize


def main() -> None:
    parser = argparse.ArgumentParser(description="판례요약모델(KoBART) 추론")
    parser.add_argument("--checkpoint", required=True, help="학습된 체크포인트(모델 가중치) 디렉토리 경로")
    parser.add_argument(
        "--tokenizer",
        default=None,
        help="토크나이저 경로(로컬 폴더 또는 HF repo id). 미지정 시 --checkpoint 경로에서 찾는다.",
    )

    text_group = parser.add_mutually_exclusive_group(required=True)
    text_group.add_argument("--text", help="요약할 판례 원문 (짧은 텍스트용, 쉘 인용 주의)")
    text_group.add_argument(
        "--text-file",
        help="판례 원문이 담긴 UTF-8 텍스트 파일 경로. 긴 판결문은 이 옵션을 권장 "
        "(따옴표/특수문자로 인한 쉘 파싱 문제를 피할 수 있음)",
    )

    parser.add_argument(
        "--plain", action="store_true", help="법률 용어에 쉬운 풀이를 덧붙여 출력"
    )
    args = parser.parse_args()

    if args.text_file:
        with open(args.text_file, encoding="utf-8") as f:
            text = f.read()
    else:
        text = args.text

    if not text.strip():
        print("입력한 판례 원문이 비어있습니다.", file=sys.stderr)
        raise SystemExit(1)

    tokenizer, model = load_summarizer(args.checkpoint, tokenizer_path=args.tokenizer)
    summary = summarize(text, tokenizer, model)

    if args.plain:
        summary = explain_for_layperson(summary)

    print(summary)


if __name__ == "__main__":
    main()
