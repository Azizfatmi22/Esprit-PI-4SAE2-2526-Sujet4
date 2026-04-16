import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { CouponService } from '../../front-office/services/coupon.service';
import { CourseService } from '../../front-office/services/course.service';
import { UserService } from '../../front-office/services/user.service';
import { Coupon, CouponStatus } from '../../front-office/models/coupon.model';
import { CourseSummary } from '../../front-office/courses/modules/course.model';

@Component({
    selector: 'app-coupons',
    standalone: true,
    imports: [CommonModule, FormsModule],
    templateUrl: './coupons.component.html',
    styleUrl: './coupons.component.scss'
})
export class CouponsComponent implements OnInit {
    courses: CourseSummary[] = [];
    selectedCourseId: number | null = null;
    expirationMinutes: number = 60;
    partnerId: string | null = null;
    myCoupons: Coupon[] = [];
    isCreating = false;

    constructor(
        private couponService: CouponService,
        private courseService: CourseService,
        private userService: UserService
    ) { }

    async ngOnInit() {
        const user = await this.userService.loadUser();
        this.partnerId = user?.id || null;
        this.loadCourses();
        this.loadMyCoupons();
    }

    loadCourses() {
        // In a real app, we might filter by partnerId if the API supports it
        this.courseService.getAllCourses(0, 100).subscribe(response => {
            this.courses = response.content || response; // Handle different API response shapes
        });
    }

    loadMyCoupons() {
        // For now, we fetch all available coupons and filter in memory if needed
        // or add a partner-specific endpoint if requested
        this.couponService.getAvailableCoupons().subscribe(coupons => {
            this.myCoupons = coupons;
        });
    }

    createCoupon() {
        if (!this.selectedCourseId || !this.partnerId) return;

        const selectedCourse = this.courses.find(c => c.id === this.selectedCourseId);
        if (!selectedCourse) return;

        const newCoupon: Coupon = {
            code: '', // Backend generates this
            courseId: selectedCourse.id!,
            courseName: selectedCourse.title,
            expirationMinutes: this.expirationMinutes,
            status: CouponStatus.ACTIVE,
            partnerId: this.partnerId
        };

        this.isCreating = true;
        this.couponService.createCoupon(newCoupon).subscribe({
            next: () => {
                this.isCreating = false;
                this.selectedCourseId = null;
                this.expirationMinutes = 60;
                this.loadMyCoupons();
                alert('🎟️ Coupon créé avec succès !');
            },
            error: (err) => {
                this.isCreating = false;
                alert(err.error?.message || 'Erreur lors de la création du coupon');
            }
        });
    }

    getLink(code: string): string {
        return `${window.location.origin}/coupons`;
    }
}
