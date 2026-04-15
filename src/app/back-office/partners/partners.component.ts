import { Component, Inject, OnInit, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser, CommonModule } from '@angular/common';
import { PartnerService } from '../../front-office/services/partner.service';
import { PartnerHiring, PartnerStatus } from '../../front-office/models/partner.model';

@Component({
  selector: 'app-partners',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './partners.component.html',
  styleUrl: './partners.component.scss'
})
export class PartnersComponent implements OnInit {
  partners: PartnerHiring[] = [];
  isLoading = true;
  errorMessage = '';
  selectedPartner: PartnerHiring | null = null;

  constructor(
    private partnerService: PartnerService,
    @Inject(PLATFORM_ID) private platformId: Object
  ) { }

  ngOnInit(): void {
    this.loadPartners();
  }

  loadPartners(): void {
    this.isLoading = true;
    this.partnerService.getAllPartners(0, 50).subscribe({
      next: (response: any) => {
        this.partners = response.content;
        this.isLoading = false;
      },
      error: (err) => {
        this.errorMessage = 'Failed to load partners.';
        this.isLoading = false;
        console.error(err);
      }
    });
  }

  updateStatus(id: string, status: string): void {
    const newStatus = status as PartnerStatus;

    // Optimistic update
    const previousPartners = [...this.partners];
    const partnerindex = this.partners.findIndex(p => p.id === id);
    if (partnerindex !== -1) {
      this.partners[partnerindex] = { ...this.partners[partnerindex], status: newStatus };
    }
    if (this.selectedPartner && this.selectedPartner.id === id) {
      this.selectedPartner = { ...this.selectedPartner, status: newStatus };
    }

    this.partnerService.updateStatus(id, status).subscribe({
      next: (updatedPartner) => {
        const confirmedStatus = (updatedPartner && updatedPartner.status)
          ? updatedPartner.status
          : newStatus;

        // Confirm update with backend response or fallback
        if (partnerindex !== -1) {
          this.partners[partnerindex] = { ...this.partners[partnerindex], status: confirmedStatus };
          this.partners = [...this.partners]; // Force UI update
        }
        if (this.selectedPartner && this.selectedPartner.id === id) {
          this.selectedPartner = { ...this.selectedPartner, status: confirmedStatus };
        }

        if (isPlatformBrowser(this.platformId)) {
          alert(`Partner status successfully updated to ${confirmedStatus}!`);
        }
      },
      error: (error) => {
        console.error('Error updating partner status:', error);
        // Revert on error
        this.partners = previousPartners;
        if (this.selectedPartner && this.selectedPartner.id === id) {
          if (isPlatformBrowser(this.platformId)) {
            alert('Failed to update status. Reverting changes.');
          }
          this.loadPartners();
        }
      }
    });
  }

  approvePartner(id: string): void {
    this.updateStatus(id, PartnerStatus.ACCEPTED);
  }

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

  selectPartner(partner: PartnerHiring): void {
    this.selectedPartner = partner;
  }

  closeDetails(): void {
    this.selectedPartner = null;
  }

  deletePartner(id: string): void {
    if (isPlatformBrowser(this.platformId)) {
      if (confirm('Are you sure you want to delete this application?')) {
        this.partnerService.deletePartner(id).subscribe({
          next: () => {
            this.partners = this.partners.filter(p => p.id !== id);
          },
          error: (err) => {
            console.error('Error deleting partner:', err);
            alert('Failed to delete application. Please try again.');
          }
        });
      }
    }
  }

  getStatusClass(status: string | undefined): string {
    if (!status) return 'pending';
    return status.toLowerCase();
  }
}
