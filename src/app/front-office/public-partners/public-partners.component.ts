import { Component, OnInit } from '@angular/core';
import { PartnerService } from '../services/partner.service';
import { PartnerHiring, PartnerStatus } from '../models/partner.model';

@Component({
  selector: 'app-public-partners',
  templateUrl: './public-partners.component.html',
  styleUrl: './public-partners.component.scss'
})
export class PublicPartnersComponent implements OnInit {
  partners: PartnerHiring[] = [];
  isLoading = true;
  errorMessage = '';

  constructor(private partnerService: PartnerService) { }

  ngOnInit(): void {
    this.loadAcceptedPartners();
  }

  loadAcceptedPartners(): void {
    this.isLoading = true;
    this.partnerService.getAllPartners(0, 100, PartnerStatus.ACCEPTED).subscribe({
      next: (response: any) => {
        this.partners = response.content;
        this.isLoading = false;
      },
      error: (err: any) => {
        console.error('Error fetching partners:', err);
        this.errorMessage = 'Failed to load partners. Please try again later.';
        this.isLoading = false;
      }
    });
  }

  getLogoUrl(partner: PartnerHiring): string {
    return this.partnerService.getDocumentUrl(partner.id!, 'LOGO');
  }
}
