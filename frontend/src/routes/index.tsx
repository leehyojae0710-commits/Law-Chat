import { Routes, Route } from "react-router-dom";
import { PublicLayout } from "../components/layout/PublicLayout";
import { LandingPage } from "../pages/LandingPage";
import { AboutPage } from "../pages/AboutPage";
import { ServiceFeaturesPage } from "../pages/ServiceFeaturesPage";
import { FaqPage } from "../pages/FaqPage";
import { PrecedentSearchPage } from "../pages/PrecedentSearchPage";
import { SupportPage } from "../pages/SupportPage";
import { NoticePage } from "../pages/NoticePage";
import { LoginPage } from "../pages/LoginPage";
import { SignupPage } from "../pages/SignupPage";
import { ChatPage } from "../pages/ChatPage";
import { AdminPage } from "../pages/AdminPage";
import { AdminRoute } from "./AdminRoute";
import { KakaoCallbackPage } from "../pages/KakaoCallbackPage";
import { NaverCallbackPage } from "../pages/NaverCallbackPage";
import { ChatHeader } from "../components/layout/ChatHeader";

export const AppRoutes = () => {
  return (
    <Routes>
      <Route element={<PublicLayout />}>
        <Route path="/" element={<LandingPage />} />
        <Route path="/about" element={<AboutPage />} />
        <Route path="/features" element={<ServiceFeaturesPage />} />
        <Route path="/faq" element={<FaqPage />} />
        <Route path="/precedents" element={<PrecedentSearchPage />} />
        <Route path="/support" element={<SupportPage />} />
        <Route path="/notices" element={<NoticePage />} />
        <Route path="/kakao/OAuth" element={<KakaoCallbackPage />} />
        <Route path="/naver/OAuth" element={<NaverCallbackPage />} />
      </Route>

      <Route path="/login" element={<LoginPage />} />
      <Route path="/Signup" element={<SignupPage />} />

      <Route element={<ChatHeader />}>
        <Route path="/chat" element={<ChatPage />} />
      </Route>
      
      <Route
        path="/admin"
        element={
          <AdminRoute>
            <AdminPage />
          </AdminRoute>
        }
      />
    </Routes>
  );
};
