import { useState } from "react";
import { getMe, updateProfile, updateProfileImg } from "../../../api/auth";


export const Profile = () => {

    const [userNickName, setUserNickName] = useState("");
    const [userEmail, setUserEmail] = useState("");
    const [userisAdmin, setUserisAdmin] = useState(false);
    const [userPhone, setUserPhone] = useState("");
    const [userProfileImg, setUserProfileImg] = useState("");
    const [newNickName, setNewNickName] = useState("");
    const [newPhone, setNewPhone] = useState("");
    const [newProfileImg, setNewProfileImg] = useState<File | null>(null);

    const userProfile = async () => {
        try {
            const response = await getMe();
            console.log("User profile response:", response);
            if (response) {
                setUserNickName(response.nickname);
                setUserEmail(response.email);
                setUserisAdmin(response.isAdmin);
                setUserPhone(response.phone);
                setUserProfileImg(response.profileImg || "");
                console.log("User profile fetched successfully:", response);
            }
            else {
                console.error("Failed to fetch user profile");
            }
        } catch (error) {
            console.error("Error fetching user profile:", error);
            return null;
        }
    }
    const uploadProfile = async () => {
        const payload: { nickName?: string; phone?: string } = {}
        if (newNickName) payload.nickName = newNickName;
        if (newPhone) payload.phone = newPhone;
        try {
            const res = await updateProfile(payload);
            if (res) {
                setUserNickName(res.nickname);
                setUserPhone(res.phone);
            }
        }
        catch (error) {
            console.error("Error updating profile:", error);
            return null;
        }
    }
    return (
        <div >
            <button onClick={userProfile}>
                찾기 버튼
            </button>
            <h2>사용자 프로필</h2>
            <p>이름: {userNickName}</p>
            <p>이메일: {userEmail}</p>
            <p>전화번호: {userPhone}</p>
            <p>관리자: {userisAdmin ? "관리자" : "일반 회원"}</p>
            {userProfileImg && (
                <img src={userProfileImg} alt="Profile" />
            )}

            <button onClick={uploadProfile}>
                프로필 업데이트
            </button>
            <input
                type="text"
                placeholder="새로운 닉네임"
                value={newNickName}
                onChange={(e) => setNewNickName(e.target.value)}
            />
            <input
                type="text"
                placeholder="새로운 전화번호"
                value={newPhone}
                onChange={(e) => setNewPhone(e.target.value)}
            />
        </div>
    )
}