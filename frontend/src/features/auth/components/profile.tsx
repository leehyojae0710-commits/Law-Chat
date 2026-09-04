import { useState } from "react";
import { getMe } from "../../../api/auth";


export const Profile = () => {

    const [userNickName, setUserNickName] = useState("");
    const [userEmail, setUserEmail] = useState("");
    const [userisAdmin, setUserisAdmin] = useState(false);
    const [userPhone, setUserPhone] = useState("");
    const [userProfileImg, setUserProfileImg] = useState("");

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
    return (
        <div >
            <button onClick={userProfile}>
                버튼
            </button>
            <h2>사용자 프로필</h2>
            <p>이름: {userNickName}</p>
            <p>이메일: {userEmail}</p>
            <p>전화번호: {userPhone}</p>
            <p>관리자: {userisAdmin ? "관리자" : "일반 회원"}</p>
            {userProfileImg && (
                <img src={userProfileImg} alt="Profile" />
            )}
        </div>
    )
}