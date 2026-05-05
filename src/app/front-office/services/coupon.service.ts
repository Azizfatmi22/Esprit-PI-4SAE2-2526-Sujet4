import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, BehaviorSubject } from 'rxjs';
import { tap } from 'rxjs/operators';
import { Coupon, CouponClaim } from '../models/coupon.model';

@Injectable({
    providedIn: 'root'
})
export class CouponService {
    private apiUrl = 'http://localhost:8085/api/coupons';

    private claimedCountSubject = new BehaviorSubject<number>(0);
    public claimedCount$ = this.claimedCountSubject.asObservable();

    constructor(private http: HttpClient) { }

    createCoupon(coupon: Coupon): Observable<Coupon> {
        return this.http.post<Coupon>(this.apiUrl, coupon);
    }

    getAvailableCoupons(userId?: string): Observable<Coupon[]> {
        const url = userId ? `${this.apiUrl}/available?userId=${userId}` : `${this.apiUrl}/available`;
        return this.http.get<Coupon[]>(url);
    }

    claimCoupon(code: string, userId: string): Observable<CouponClaim> {
        return this.http.post<CouponClaim>(`${this.apiUrl}/${code}/claim`, { userId }).pipe(
            tap(() => this.refreshClaimedCount(userId))
        );
    }

    getMyClaims(userId: string): Observable<CouponClaim[]> {
        return this.http.get<CouponClaim[]>(`${this.apiUrl}/my/${userId}`).pipe(
            tap(claims => this.claimedCountSubject.next(claims.filter(c => !c.used).length))
        );
    }

    redeemCoupon(code: string, userId: string, courseId: number): Observable<CouponClaim> {
        return this.http.post<CouponClaim>(`${this.apiUrl}/${code}/redeem`, { userId, courseId }).pipe(
            tap(() => this.refreshClaimedCount(userId))
        );
    }

    private refreshClaimedCount(userId: string): void {
        this.getMyClaims(userId).subscribe();
    }
}
