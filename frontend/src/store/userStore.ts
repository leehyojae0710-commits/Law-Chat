import { create } from "zustand";
import { getMe, updateProfile, updateProfileImg } from "../api/auth";

interface UserState {
    userNickName: string;
    userEmail: string;
    userIsAdmin: boolean;
    userPhone: string;
    userProfileImg: string;
    setUserProfile: (data: {
        nickname: string;
        email: string;
        isAdmin: boolean;
        phone: string;
        profileImg: string;
    }) => void;
    setUserNickName: (nickname: string) => void;
    setUserEmail: (email: string) => void;
    setUserIsAdmin: (isAdmin: boolean) => void;
    setUserPhone: (phone: string) => void;
    setUserProfileImg: (profileImg: string) => void;

    fetchUserProfile: () => Promise<void>;
    updateUserProfile: (data: { newNickName?: string; newPhone?: string }) => Promise<void>;
    updateUserProfileImg: (profileImg: File) => Promise<void>;
}

export const useUserStore = create<UserState>((set) => ({
    userNickName: "",
    userEmail: "",
    userIsAdmin: false,
    userPhone: "",
    userProfileImg: "",
    setUserProfile: (data) =>
        set({
            userNickName: data.nickname || "",
            userEmail: data.email || "",
            userIsAdmin: data.isAdmin || false,
            userPhone: data.phone || "",
            userProfileImg: data.profileImg || "",
        }),
    setUserNickName: (nickname) => set({ userNickName: nickname }),
    setUserEmail: (email) => set({ userEmail: email }),
    setUserIsAdmin: (isAdmin) => set({ userIsAdmin: isAdmin }),
    setUserPhone: (phone) => set({ userPhone: phone }),
    setUserProfileImg: (profileImg) => set({ userProfileImg: profileImg }),

    fetchUserProfile: async () => {
        try {
            const response = await getMe();
            if (response) {
                set({
                    userNickName: response.nickname,
                    userEmail: response.email,
                    userIsAdmin: response.isAdmin,
                    userPhone: response.phone,
                    userProfileImg: response.profileImg || "",
                });
            }
        } catch (error) {
            console.error("Error fetching user profile:", error);
        }
    },
    updateUserProfile: async (data: { newNickName?: string; newPhone?: string }) => {
        const payload: { nickname?: string; phone?: string } = {};
        if (data.newNickName) payload.nickname = data.newNickName;
        if (data.newPhone) payload.phone = data.newPhone;

        try {
            const res = await updateProfile(payload);
            if (res) {
                set({ userNickName: res.nickname, userPhone: res.phone });
            }
        } catch (error) {
            console.error("Error updating profile:", error);
        }
    },
    updateUserProfileImg: async (profileImg: File) => {
        try {
            const res = await updateProfileImg(profileImg);
            if (res) {
                set({ userProfileImg: res.profileImg || "" });
            }
        } catch (error) {
            console.error("Error updating profile image:", error);
        }
    },
}));