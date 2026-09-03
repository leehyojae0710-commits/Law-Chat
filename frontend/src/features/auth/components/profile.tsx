import { getMe } from "../../../api/auth";

export const Profile =  () => {
    const userToken = sessionStorage.getItem("userToken");
    let userName = "";
    let userEmail = "";
    let userisAdmin = false;

    const userProfile = async () => {
         try {
                const response = await getMe();
                if (response) {
                    userName = response.name;
                    userEmail = response.email;
                    userisAdmin = response.isAdmin;
                    console.log("User profile fetched successfully:", response);
                }
                else{
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
            <p>이름: {userName}</p>
            <p>이메일: {userEmail}</p>
            <p>관리자: {userisAdmin ? "관리자" : "일반 회원"}</p>
        </div>
    )
}