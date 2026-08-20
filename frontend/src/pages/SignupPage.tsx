import { Link } from "react-router-dom";
import { SignupForm } from "../features/auth/components/SignupForm";

export const SignupPage = () => {
    return (
        <div className="min-h-screen bg-violet-50 flex items-center justify-center">
            <div className="max-w-[500px] mx-auto px-6 py-20 flex flex-col items-center gap-6 bg-white rounded-xl shadow-sm">
                <Link to="/" className="flex flex-col items-center gap-2 text-center">
                    <div className="w-20 h-20 rounded-full bg-violet-600 text-white flex items-center justify-center font-bold text-4xl">
                        L
                    </div>
                    <div>
                        <p className="font-bold text-lg text-slate-900">LawChat</p>
                        <p className="text-xs text-slate-400">AI 법률 상담 챗봇</p>
                    </div>
                </Link>
                <SignupForm />
            </div>
        </div>
    );
};