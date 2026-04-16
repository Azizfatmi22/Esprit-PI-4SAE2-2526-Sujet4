import { Component, Inject, OnInit, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser, CommonModule } from '@angular/common';
import { PartnerService } from '../../front-office/services/partner.service';
import { PartnerHiring, PartnerStatus } from '../../front-office/models/partner.model';
import { CouponService } from '../../front-office/services/coupon.service';
import { CourseService } from '../../front-office/services/course.service';
import { Coupon, CouponStatus } from '../../front-office/models/coupon.model';
import { CourseSummary } from '../../front-office/courses/modules/course.model';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-partners',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './partners.component.html',
  styleUrl: './partners.component.scss'
})
export class PartnersComponent implements OnInit {
  partners: PartnerHiring[] = [];
  filteredPartners: PartnerHiring[] = [];
  isLoading = true;
  errorMessage = '';
  selectedPartner: PartnerHiring | null = null;
  filterKeyword = '';

  // Coupon Form properties
  isCouponFormVisible = false;
  availableCourses: CourseSummary[] = [];
  newCoupon: Partial<Coupon> = {
    code: '',
    expirationMinutes: 60,
    status: CouponStatus.ACTIVE
  };

  constructor(
    private partnerService: PartnerService,
    private couponService: CouponService,
    private courseService: CourseService,
    @Inject(PLATFORM_ID) private platformId: Object
  ) { }

  ngOnInit(): void {
    this.loadPartners();
    this.loadCourses();
  }

  // --- Stats Getters for Dashboard ---
  get totalPartners(): number { return this.partners.length; }
  get acceptedCount(): number { return this.partners.filter(p => p.status === 'ACCEPTED').length; }
  get pendingCount(): number { return this.partners.filter(p => !p.status || p.status === 'PENDING').length; }
  get rejectedCount(): number { return this.partners.filter(p => p.status === 'REJECTED').length; }

  getStatsPercentage(count: number): number {
    if (this.totalPartners === 0) return 0;
    return Math.round((count / this.totalPartners) * 100);
  }

  onFilterChange(): void {
    if (!this.filterKeyword.trim()) {
      this.filteredPartners = [...this.partners];
    } else {
      const kw = this.filterKeyword.toLowerCase();
      this.filteredPartners = this.partners.filter(p => 
        p.organizationName.toLowerCase().includes(kw) || 
        p.email.toLowerCase().includes(kw)
      );
    }
  }

  clearFilters(): void {
    this.filterKeyword = '';
    this.onFilterChange();
  }

  loadCourses(): void {
    this.courseService.getAllCourses(0, 100).subscribe({
      next: (response: any) => {
        this.availableCourses = response.content;
      },
      error: (err: any) => console.error('Failed to load courses', err)
    });
  }

  toggleCouponForm(): void {
    this.isCouponFormVisible = !this.isCouponFormVisible;
    if (this.isCouponFormVisible && this.selectedPartner) {
      const randomStr = Math.random().toString(36).substring(2, 8).toUpperCase();
      this.newCoupon.code = `PRT-${this.selectedPartner.organizationName.substring(0, 3).toUpperCase()}-${randomStr}`;
      this.newCoupon.partnerId = this.selectedPartner.id;
    }
  }

  submitCouponForm(): void {
    if (!this.newCoupon.code || !this.newCoupon.courseId || !this.newCoupon.expirationMinutes) {
      alert('Please fill all fields');
      return;
    }

    const selectedCourse = this.availableCourses.find(c => c.id === Number(this.newCoupon.courseId));
    if (selectedCourse) {
      this.newCoupon.courseName = selectedCourse.title;
    }

    this.couponService.createCoupon(this.newCoupon as Coupon).subscribe({
      next: (created: any) => {
        alert(`Coupon ${created.code} successfully created for ${this.selectedPartner?.organizationName}`);
        this.isCouponFormVisible = false;
        this.newCoupon = { code: '', expirationMinutes: 60, status: CouponStatus.ACTIVE };
      },
      error: (err: any) => {
        console.error('Failed to create coupon', err);
        alert('Failed to create coupon. It might already exist.');
      }
    });
  }

  loadPartners(): void {
    this.isLoading = true;
    this.partnerService.getAllPartners(0, 50).subscribe({
      next: (response: any) => {
        this.partners = response.content;
        this.onFilterChange();
        this.isLoading = false;
      },
      error: (err: any) => {
        this.errorMessage = 'Failed to load partners.';
        this.isLoading = false;
        console.error(err);
      }
    });
  }

  updateStatus(id: string, status: string): void {
    const newStatus = status as PartnerStatus;
    const previousPartners = [...this.partners];
    const partnerindex = this.partners.findIndex(p => p.id === id);
    
    if (partnerindex !== -1) {
      this.partners[partnerindex] = { ...this.partners[partnerindex], status: newStatus };
    }
    if (this.selectedPartner && this.selectedPartner.id === id) {
      this.selectedPartner = { ...this.selectedPartner, status: newStatus };
    }

    this.partnerService.updateStatus(id, status).subscribe({
      next: (updatedPartner: any) => {
        const confirmedStatus = (updatedPartner && updatedPartner.status) ? updatedPartner.status : newStatus;
        if (partnerindex !== -1) {
          this.partners[partnerindex] = { ...this.partners[partnerindex], status: confirmedStatus };
          this.onFilterChange();
        }
        if (this.selectedPartner && this.selectedPartner.id === id) {
          this.selectedPartner = { ...this.selectedPartner, status: confirmedStatus };
        }
        if (isPlatformBrowser(this.platformId)) {
          alert(`Partner status successfully updated to ${confirmedStatus}!`);
        }
      },
      error: (error: any) => {
        console.error('Error updating partner status:', error);
        this.partners = previousPartners;
        this.onFilterChange();
        if (isPlatformBrowser(this.platformId)) {
          alert('Failed to update status. Reverting changes.');
        }
      }
    });
  }

  approvePartner(id: string): void { this.updateStatus(id, PartnerStatus.ACCEPTED); }
  rejectPartner(id: string): void {
    if (isPlatformBrowser(this.platformId)) {
      if (confirm('Are you sure you want to reject this application?')) {
        this.updateStatus(id, PartnerStatus.REJECTED);
      }
    }
  }

  getLogoUrl(partner: PartnerHiring): string {
    return this.partnerService.getDocumentUrl(partner.id!, 'LOGO');
  }

  viewDocument(partnerId: string, type: string): void {
    if (isPlatformBrowser(this.platformId)) {
      window.open(this.partnerService.getDocumentUrl(partnerId, type), '_blank');
    }
  }

  selectPartner(partner: PartnerHiring): void { this.selectedPartner = partner; }
  closeDetails(): void { this.selectedPartner = null; }

  deletePartner(id: string): void {
    if (isPlatformBrowser(this.platformId) && confirm('Are you sure you want to delete this application?')) {
      this.partnerService.deletePartner(id).subscribe({
        next: () => {
          this.partners = this.partners.filter(p => p.id !== id);
          this.onFilterChange();
          if (this.selectedPartner && this.selectedPartner.id === id) this.selectedPartner = null;
        },
        error: (err) => {
          console.error('Error deleting partner:', err);
          alert('Failed to delete application.');
        }
      });
    }
  }

  getStatusClass(status: string | undefined): string {
    if (!status) return 'pending';
    switch (status.toUpperCase()) {
      case 'ACCEPTED': return 'accepted';
      case 'REJECTED': return 'rejected';
      case 'INVESTIGATING': return 'investigating';
      default: return 'pending';
    }
  }

  isAccepted(partner: PartnerHiring | null): boolean {
    return partner?.status?.toUpperCase() === 'ACCEPTED';
  }
}
