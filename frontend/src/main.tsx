import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import { BrowserRouter } from "react-router-dom";
import App from "./App.tsx";
import "./styles/index.css";
// 다크모드 상태를 앱 시작과 동시에(모달을 열지 않아도) 적용하기 위해
// 여기서 한 번 import해서 스토어 모듈의 최상위 코드가 즉시 실행되게 합니다.
import "../src/store/themsStore.ts";

createRoot(document.getElementById("root")!).render(
  <StrictMode>
    <BrowserRouter>
      <App />
    </BrowserRouter>
  </StrictMode>
);