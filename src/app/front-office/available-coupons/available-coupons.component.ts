import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { CouponService } from '../services/coupon.service';
import { UserService } from '../services/user.service';
import { Coupon } from '../models/coupon.model';
import { interval, Subscription } from 'rxjs';
import { NotificationService } from '../services/notification.service';

@Component({
    selector: 'app-available-coupons',
    standalone: true,
    imports: [CommonModule, RouterModule],
    templateUrl: './available-coupons.component.html',
    styleUrl: './available-coupons.component.scss'
})
export class AvailableCouponsComponent implements OnInit, OnDestroy {
    availableCoupons: Coupon[] = [];
    userId: string | null = null;
    claiming = false;
    showCongrats = false;
    claimedCouponName = '';
    redirectProgress = 0;
    private timerSubscription?: Subscription;

    constructor(
        private couponService: CouponService,
        private userService: UserService,
        private notificationService: NotificationService,
        private router: Router
    ) { }

    async ngOnInit() {
        const user = await this.userService.loadUser();
        this.userId = user?.id || null;
        this.loadCoupons();

        // Refresh timers every minute
        this.timerSubscription = interval(60000).subscribe(() => {
            this.loadCoupons();
        });
    }

    ngOnDestroy() {
        this.timerSubscription?.unsubscribe();
    }

    loadCoupons() {
        this.couponService.getAvailableCoupons(this.userId || undefined).subscribe(coupons => {
            this.availableCoupons = coupons;
        });
    }

    claim(coupon: Coupon) {
        if (!this.userId || this.claiming) return;

        this.claiming = true;
        this.couponService.claimCoupon(coupon.code, this.userId).subscribe({
            next: (claim) => {
                this.claiming = false;
                this.showCongrats = true;
                this.claimedCouponName = coupon.courseName;
                this.loadCoupons(); // Refresh list to remove claimed one

                // Start redirection progress
                const duration = 3000; // 3 seconds
                const step = 100;
                const intervalTime = duration / (100 / 1);

                const timer = setInterval(() => {
                    this.redirectProgress += 2;
                    if (this.redirectProgress >= 100) {
                        clearInterval(timer);
                        this.router.navigate(['/learner/courses', coupon.courseId]);
                    }
                }, 60);
            },
            error: (err) => {
                this.claiming = false;
                alert(err.error?.message || 'Failed to claim coupon');
            }
        });
    }

    getTimerColor(minutes: number): string {
        if (minutes < 10) return '#ff4d4f';
        if (minutes < 30) return '#faad14';
        return '#52c41a';
    }
}
