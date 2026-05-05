export enum CouponStatus {
    ACTIVE = 'ACTIVE',
    EXPIRED = 'EXPIRED'
}

export interface Coupon {
    id?: string;
    code: string;
    courseId: number;
    courseName: string;
    expirationMinutes: number;
    createdAt?: Date;
    expiresAt?: Date;
    status: CouponStatus;
    partnerId?: string;
    partnerName?: string;
    partnerLogo?: string; // Base64 or Blob URL
    minutesRemaining?: number;
}

export interface CouponClaim {
    id?: string;
    couponCode: string;
    courseId: number;
    courseName: string;
    userId: string;
    claimedAt: Date;
    used: boolean;
    minutesRemaining: number;
}
