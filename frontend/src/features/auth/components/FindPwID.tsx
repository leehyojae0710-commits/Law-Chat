import { useNavigate } from "react-router-dom";


export const FindPassword_Id = () => {
    const navigate = useNavigate();
    return (
        <div>
            <button
                type="button"
                onClick={() => navigate("/find")}
                className="w-full py-3 rounded-lg bg-violet-600 text-white font-medium disabled:opacity-50">
                아이디, 비밀번호 찾기
            </button>
        </div>
    );
};