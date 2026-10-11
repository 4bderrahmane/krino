export interface UserUpdateDTO {
    version: number;
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
