export interface UserUpdateDTO {
    username?: string;
    firstName?: string;
    lastName?: string;
    email?: string;
    phoneNumber?: string;
}

export interface UserUpdatePasswordDTO {
    currentPassword: string;
    newPassword: string;
    confirmNewPassword: string;
}
