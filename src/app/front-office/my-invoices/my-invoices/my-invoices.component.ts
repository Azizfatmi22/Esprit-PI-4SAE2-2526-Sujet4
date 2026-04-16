import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { CartService, Invoice } from '../../services/cart.service';
import jsPDF from 'jspdf';
import { UserService } from '../../services/user.service';
import { User } from '../../../user';
@Component({
  selector: 'app-my-invoices',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './my-invoices.component.html',
  styleUrls: ['./my-invoices.component.scss']
})
export class MyInvoicesComponent implements OnInit {
  currentUser: User | null = null;
  learnerId: string | null = null;

  // ===== FACTURES SÉPARÉES =====
  directInvoices: Invoice[] = [];
  installmentInvoices: Invoice[] = [];
  loadingDirect = false;
  loadingInstallment = false;

  // Map planId -> factures (calculée une seule fois au chargement)
  planInvoicesMap: { [planId: number]: Invoice[] } = {};
  invoices: Invoice[] = [];
  installmentSummaries: any[] = [];
  loading = false;
  loadingSummary = false;

  selectedTab: 'invoices' | 'installments' = 'invoices';
  invoiceSubTab: 'direct' | 'installment' = 'direct';

  // ===== RECHERCHE & FILTRES — DIRECT =====
  directSearchTerm = '';
  directFilterStatus = 'all';
  directDateFrom = '';
  directDateTo = '';
  directCurrentPage = 1;

  // ===== RECHERCHE & FILTRES — INSTALLMENT INVOICES =====
  instInvSearchTerm = '';
  instInvFilterStatus = 'all';
  instInvCurrentPage = 1;
  instInvItemsPerPage = 6;

  // ===== RECHERCHE & FILTRES — anciens =====
  searchTerm = '';
  filterStatus = 'all';
  dateFrom = '';
  dateTo = '';

  // ===== PLANS =====
  planSearchTerm = '';
  planFilterStatus = 'all';

  // ===== PAGINATION =====
  currentPage = 1;
  itemsPerPage = 6;
  planCurrentPage = 1;
  planItemsPerPage = 6;

  // ===== MODALS =====
  selectedInvoice: Invoice | null = null;
  showDetailModal = false;
  selectedPlan: any | null = null;
  showPlanModal = false;

  // ===== MESSAGES =====
  successMessage: string | null = null;
  errorMessage: string | null = null;

  constructor(
    private cartService: CartService,
    private userService: UserService
  ) {}

  async ngOnInit(): Promise<void> {
    this.currentUser = this.userService.getUser() || null;
    if (!this.currentUser) {
      this.currentUser = (await this.userService.loadUser()) || null;
    }

    this.learnerId = this.currentUser?.id ?? null;
    if (!this.learnerId) {
      this.errorMessage = 'Utilisateur non connecté. Veuillez vous reconnecter.';
      return;
    }

    this.loadInvoices();
    this.loadInstallmentSummary();
  }

  // ================================================================
  // CHARGEMENT
  // ================================================================

  loadInvoices(): void {
    if (!this.learnerId) {
      return;
    }

    const learnerId = this.learnerId;
    this.loadingDirect = true;
    this.cartService.getDirectInvoices(learnerId).subscribe({
      next: (data) => {
        // Dédupliquer par id
        this.directInvoices = data.filter((inv, index, self) =>
          index === self.findIndex(i => i.id === inv.id)
        );
        this.loadingDirect = false;
      },
      error: () => { this.loadingDirect = false; }
    });

    this.loadingInstallment = true;
    this.cartService.getInstallmentInvoices(learnerId).subscribe({
      next: (data) => {
        // Dédupliquer par id
        const unique = data.filter((inv, index, self) =>
          index === self.findIndex(i => i.id === inv.id)
        );
        this.installmentInvoices = unique;

        // Construire la Map planId -> Invoice[] une seule fois
        this.planInvoicesMap = {};
unique.forEach(inv => {
  if (inv.installmentPlanId != null) {
    if (!this.planInvoicesMap[inv.installmentPlanId]) {
      this.planInvoicesMap[inv.installmentPlanId] = [];
    }
    this.planInvoicesMap[inv.installmentPlanId].push(inv);
  }
});

        this.loadingInstallment = false;
      },
      error: () => { this.loadingInstallment = false; }
    });
  }

  loadInstallmentSummary(): void {
    if (!this.learnerId) {
      return;
    }

    this.loadingSummary = true;
    this.cartService.getInstallmentSummary(this.learnerId).subscribe({
      next: (data) => {
        const seen = new Set<number>();
        this.installmentSummaries = data.filter(s => {
          if (seen.has(s.planId)) return false;
          seen.add(s.planId);
          return true;
        });
        this.loadingSummary = false;
      },
      error: () => { this.loadingSummary = false; }
    });
  }

  // ================================================================
  // GETTER MAP — utilisé dans le HTML
  // ================================================================

  getInvoicesForPlan(planId: number): Invoice[] {
  return this.planInvoicesMap[planId] || [];
}
  // ================================================================
  // FILTRES — FACTURES DIRECTES
  // ================================================================

  get filteredDirectInvoices(): Invoice[] {
    let filtered = [...this.directInvoices];

    if (this.directSearchTerm.trim()) {
      const term = this.directSearchTerm.toLowerCase();
      filtered = filtered.filter(inv =>
        inv.invoiceNumber?.toLowerCase().includes(term) ||
        inv.status?.toLowerCase().includes(term) ||
        inv.purchasedCourses?.some(c => c.toLowerCase().includes(term))
      );
    }

    if (this.directFilterStatus !== 'all') {
      filtered = filtered.filter(inv =>
        (inv.status || '').toLowerCase() === this.directFilterStatus.toLowerCase()
      );
    }

    if (this.directDateFrom) {
      const from = new Date(this.directDateFrom);
      filtered = filtered.filter(inv => new Date(inv.issueDate) >= from);
    }

    if (this.directDateTo) {
      const to = new Date(this.directDateTo);
      to.setHours(23, 59, 59);
      filtered = filtered.filter(inv => new Date(inv.issueDate) <= to);
    }

    return filtered;
  }

  get directTotalPages(): number {
    return Math.ceil(this.filteredDirectInvoices.length / this.itemsPerPage);
  }

  get paginatedDirectInvoices(): Invoice[] {
    const start = (this.directCurrentPage - 1) * this.itemsPerPage;
    return this.filteredDirectInvoices.slice(start, start + this.itemsPerPage);
  }

  getDirectPagesArray(): number[] {
    return Array.from({ length: this.directTotalPages }, (_, i) => i + 1);
  }

  goToDirectPage(page: number): void {
    if (page >= 1 && page <= this.directTotalPages) {
      this.directCurrentPage = page;
      window.scrollTo({ top: 0, behavior: 'smooth' });
    }
  }

  resetDirectFilters(): void {
    this.directSearchTerm = '';
    this.directFilterStatus = 'all';
    this.directDateFrom = '';
    this.directDateTo = '';
    this.directCurrentPage = 1;
  }

  getDirectStatuses(): string[] {
    return [...new Set(this.directInvoices.map(i => i.status).filter(Boolean))];
  }

  // ================================================================
  // FILTRES — FACTURES ÉCHELONNÉES
  // ================================================================

  get filteredInstallmentInvoices(): Invoice[] {
    let filtered = [...this.installmentInvoices];

    if (this.instInvSearchTerm.trim()) {
      const term = this.instInvSearchTerm.toLowerCase();
      filtered = filtered.filter(inv =>
        inv.invoiceNumber?.toLowerCase().includes(term) ||
        inv.status?.toLowerCase().includes(term) ||
        inv.purchasedCourses?.some(c => c.toLowerCase().includes(term))
      );
    }

    if (this.instInvFilterStatus !== 'all') {
      filtered = filtered.filter(inv =>
        (inv.status || '').toLowerCase() === this.instInvFilterStatus.toLowerCase()
      );
    }

    return filtered;
  }

  get instInvTotalPages(): number {
    return Math.ceil(this.filteredInstallmentInvoices.length / this.instInvItemsPerPage);
  }

  get paginatedInstallmentInvoices(): Invoice[] {
    const start = (this.instInvCurrentPage - 1) * this.instInvItemsPerPage;
    return this.filteredInstallmentInvoices.slice(start, start + this.instInvItemsPerPage);
  }

  getInstInvPagesArray(): number[] {
    return Array.from({ length: this.instInvTotalPages }, (_, i) => i + 1);
  }

  goToInstInvPage(page: number): void {
    if (page >= 1 && page <= this.instInvTotalPages) {
      this.instInvCurrentPage = page;
      window.scrollTo({ top: 0, behavior: 'smooth' });
    }
  }

  resetInstInvFilters(): void {
    this.instInvSearchTerm = '';
    this.instInvFilterStatus = 'all';
    this.instInvCurrentPage = 1;
  }

  getInstInvStatuses(): string[] {
    return [...new Set(this.installmentInvoices.map(i => i.status).filter(Boolean))];
  }

  // ================================================================
  // ANCIENS GETTERS — compatibilité
  // ================================================================

  get filteredInvoices(): Invoice[] {
    return [...this.directInvoices, ...this.installmentInvoices];
  }

  get totalPages(): number {
    return Math.ceil(this.filteredInvoices.length / this.itemsPerPage);
  }

  get paginatedInvoices(): Invoice[] {
    const start = (this.currentPage - 1) * this.itemsPerPage;
    return this.filteredInvoices.slice(start, start + this.itemsPerPage);
  }

  getPagesArray(): number[] {
    return Array.from({ length: this.totalPages }, (_, i) => i + 1);
  }

  goToPage(page: number): void {
    if (page >= 1 && page <= this.totalPages) {
      this.currentPage = page;
      window.scrollTo({ top: 0, behavior: 'smooth' });
    }
  }

  resetFilters(): void {
    this.searchTerm = '';
    this.filterStatus = 'all';
    this.dateFrom = '';
    this.dateTo = '';
    this.currentPage = 1;
  }

  getUniqueStatuses(): string[] {
    const all = [...this.directInvoices, ...this.installmentInvoices];
    return [...new Set(all.map(i => i.status).filter(Boolean))];
  }

  // ================================================================
  // MODAL DÉTAIL FACTURE
  // ================================================================

  openDetail(invoice: Invoice): void {
    this.selectedInvoice = invoice;
    this.showDetailModal = true;
    document.body.style.overflow = 'hidden';
  }

  closeDetail(): void {
    this.selectedInvoice = null;
    this.showDetailModal = false;
    document.body.style.overflow = '';
  }

  // ================================================================
  // SUPPRESSION FACTURE
  // ================================================================

  deleteInvoice(invoice: Invoice): void {
    if (!confirm(`Supprimer la facture ${invoice.invoiceNumber} ?\nCette action est irréversible.`)) return;

    this.cartService.deleteInvoice(invoice.id).subscribe({
      next: () => {
        if (invoice.installmentPlanId) {
          this.installmentInvoices = this.installmentInvoices.filter(i => i.id !== invoice.id);
          // Mettre à jour la Map
          this.planInvoicesMap[invoice.installmentPlanId] =
  (this.planInvoicesMap[invoice.installmentPlanId] || []).filter(i => i.id !== invoice.id);
          if (this.instInvCurrentPage > this.instInvTotalPages && this.instInvTotalPages > 0) {
            this.instInvCurrentPage = this.instInvTotalPages;
          }
        } else {
          this.directInvoices = this.directInvoices.filter(i => i.id !== invoice.id);
          if (this.directCurrentPage > this.directTotalPages && this.directTotalPages > 0) {
            this.directCurrentPage = this.directTotalPages;
          }
        }
        this.successMessage = `✅ Facture ${invoice.invoiceNumber} supprimée`;
        setTimeout(() => this.successMessage = null, 4000);
        if (this.showDetailModal) this.closeDetail();
      },
      error: () => {
        this.errorMessage = 'Erreur lors de la suppression';
        setTimeout(() => this.errorMessage = null, 4000);
      }
    });
  }

  // ================================================================
  // PDF FACTURE
  // ================================================================

  downloadInvoicePDF(invoice: Invoice): void {
    const doc = new jsPDF();
    const pageWidth = doc.internal.pageSize.getWidth();

    doc.setFillColor(124, 58, 237);
    doc.rect(0, 0, pageWidth, 45, 'F');
    doc.setTextColor(255, 255, 255);
    doc.setFontSize(20);
    doc.setFont('helvetica', 'bold');
    doc.text('FORMINI', 20, 20);
    doc.setFontSize(11);
    doc.setFont('helvetica', 'normal');
    doc.text('Facture Officielle', 20, 30);
    doc.text('N° ' + invoice.invoiceNumber, 20, 38);
    doc.text('Date: ' + new Date(invoice.issueDate).toLocaleDateString('fr-FR'),
      pageWidth - 20, 38, { align: 'right' });

    let y = 60;

    doc.setFont('helvetica', 'bold');
    doc.setFontSize(13);
    doc.setTextColor(26, 26, 46);
    doc.text('Détails de la Facture', 20, y);
    y += 4;
    doc.setDrawColor(124, 58, 237);
    doc.setLineWidth(0.5);
    doc.line(20, y, pageWidth - 20, y);
    y += 12;

    const rows: [string, string][] = [
      ['Numéro',        invoice.invoiceNumber],
      ['Montant',       invoice.totalAmount.toFixed(2) + ' TND'],
      ['Devise',        invoice.currency || 'TND'],
      ['Statut',        invoice.status],
      ['Date émission', new Date(invoice.issueDate).toLocaleDateString('fr-FR')],
    ];

    rows.forEach(([label, value]) => {
      doc.setFont('helvetica', 'normal');
      doc.setFontSize(11);
      doc.setTextColor(100, 100, 120);
      doc.text(label + ' :', 25, y);
      doc.setFont('helvetica', 'bold');
      doc.setTextColor(26, 26, 46);
      doc.text(value, pageWidth - 25, y, { align: 'right' });
      y += 10;
    });

    y += 8;

    doc.setFont('helvetica', 'bold');
    doc.setFontSize(13);
    doc.setTextColor(26, 26, 46);
    doc.text('Cours achetés', 20, y);
    y += 4;
    doc.line(20, y, pageWidth - 20, y);
    y += 10;

    invoice.purchasedCourses?.forEach((course, i) => {
      doc.setFillColor(i % 2 === 0 ? 248 : 242, 245, 255);
      doc.rect(20, y - 5, pageWidth - 40, 10, 'F');
      doc.setFont('helvetica', 'normal');
      doc.setFontSize(11);
      doc.setTextColor(60, 60, 80);
      doc.text('• ' + course, 26, y + 1);
      y += 12;
    });

    doc.setFillColor(245, 240, 255);
    doc.rect(0, 275, pageWidth, 22, 'F');
    doc.setDrawColor(200, 180, 255);
    doc.line(0, 275, pageWidth, 275);
    doc.setTextColor(124, 58, 237);
    doc.setFontSize(8);
    doc.text('Formini — contact@formini.tn | www.formini.tn', pageWidth / 2, 283, { align: 'center' });
    doc.text('Document généré automatiquement', pageWidth / 2, 290, { align: 'center' });

    doc.save(`facture-${invoice.invoiceNumber}.pdf`);
    const blob = doc.output('blob');
    window.open(URL.createObjectURL(blob), '_blank');
  }

  // ================================================================
  // HELPERS
  // ================================================================

  getTotalRemaining(): number {
    return this.installmentSummaries
      .filter(s => s.status === 'ACTIVE')
      .reduce((sum, s) => sum + s.amountRemaining, 0);
  }

  formatCurrency(amount: number): string {
    return new Intl.NumberFormat('fr-FR', { style: 'currency', currency: 'TND' }).format(amount);
  }

  formatDate(d: string): string {
    if (!d) return '-';
    return new Date(d).toLocaleDateString('fr-FR', { year: 'numeric', month: 'short', day: 'numeric' });
  }

  isOverdue(dateStr: string): boolean {
    if (!dateStr) return false;
    return new Date(dateStr) < new Date();
  }

  // ================================================================
  // PLANS — FILTRES
  // ================================================================

  get filteredPlans(): any[] {
    let filtered = [...this.installmentSummaries];

    if (this.planSearchTerm.trim()) {
      const term = this.planSearchTerm.toLowerCase();
      filtered = filtered.filter(s =>
        s.planId?.toString().includes(term) ||
        s.status?.toLowerCase().includes(term)
      );
    }

    if (this.planFilterStatus !== 'all') {
      filtered = filtered.filter(s =>
        s.status?.toLowerCase() === this.planFilterStatus.toLowerCase()
      );
    }

    return filtered;
  }

  resetPlanFilters(): void {
    this.planSearchTerm = '';
    this.planFilterStatus = 'all';
    this.planCurrentPage = 1;
  }

  // ================================================================
  // PLANS — PAGINATION
  // ================================================================

  get planTotalPages(): number {
    return Math.ceil(this.filteredPlans.length / this.planItemsPerPage);
  }

  get paginatedPlans(): any[] {
    const start = (this.planCurrentPage - 1) * this.planItemsPerPage;
    return this.filteredPlans.slice(start, start + this.planItemsPerPage);
  }

  getPlanPagesArray(): number[] {
    return Array.from({ length: this.planTotalPages }, (_, i) => i + 1);
  }

  goToPlanPage(page: number): void {
    if (page >= 1 && page <= this.planTotalPages) {
      this.planCurrentPage = page;
      window.scrollTo({ top: 0, behavior: 'smooth' });
    }
  }

  // ================================================================
  // PLANS — MODAL
  // ================================================================

  openPlanDetail(plan: any): void {
    this.selectedPlan = plan;
    this.showPlanModal = true;
    document.body.style.overflow = 'hidden';
  }

  closePlanDetail(): void {
    this.selectedPlan = null;
    this.showPlanModal = false;
    document.body.style.overflow = '';
  }

  // ================================================================
  // PLANS — SUPPRESSION
  // ================================================================

  deletePlan(plan: any): void {
    if (!confirm(`Supprimer le Plan #${plan.planId} ?\nToutes les échéances seront supprimées.`)) return;

    this.cartService.deletePlan(plan.planId).subscribe({
      next: () => {
        this.installmentSummaries = this.installmentSummaries.filter(s => s.planId !== plan.planId);
        delete this.planInvoicesMap[plan.planId];

        this.successMessage = `✅ Plan #${plan.planId} supprimé`;
        setTimeout(() => this.successMessage = null, 4000);
        if (this.planCurrentPage > this.planTotalPages && this.planTotalPages > 0) {
          this.planCurrentPage = this.planTotalPages;
        }
        if (this.showPlanModal) this.closePlanDetail();
      },
      error: () => {
        this.errorMessage = 'Erreur lors de la suppression du plan';
        setTimeout(() => this.errorMessage = null, 4000);
      }
    });
  }

  // ================================================================
  // PLANS — PDF
  // ================================================================

  downloadPlanPDF(plan: any): void {
    const doc = new jsPDF();
    const pageWidth = doc.internal.pageSize.getWidth();

    doc.setFillColor(124, 58, 237);
    doc.rect(0, 0, pageWidth, 45, 'F');
    doc.setTextColor(255, 255, 255);
    doc.setFontSize(20);
    doc.setFont('helvetica', 'bold');
    doc.text('FORMINI', 20, 20);
    doc.setFontSize(11);
    doc.setFont('helvetica', 'normal');
    doc.text('Plan de Paiement Échelonné', 20, 30);
    doc.text('Plan #' + plan.planId, 20, 38);

    let y = 60;

    doc.setFont('helvetica', 'bold');
    doc.setFontSize(13);
    doc.setTextColor(26, 26, 46);
    doc.text('Détails du Plan', 20, y);
    y += 4;
    doc.setDrawColor(124, 58, 237);
    doc.setLineWidth(0.5);
    doc.line(20, y, pageWidth - 20, y);
    y += 12;

    const rows: [string, string][] = [
      ['Plan ID',            '#' + plan.planId],
      ['Statut',             plan.status],
      ['Total échéances',    plan.totalInstallments + 'x'],
      ['Montant total',      plan.totalAmount?.toFixed(2) + ' TND'],
      ['Déjà payé',          plan.amountPaid?.toFixed(2) + ' TND'],
      ['Reste à payer',      plan.amountRemaining?.toFixed(2) + ' TND'],
      ['Échéances payées',   plan.paidInstallments + '/' + plan.totalInstallments],
      ['Prochaine échéance', plan.nextDueDate ? new Date(plan.nextDueDate).toLocaleDateString('fr-FR') : '-'],
      ['Montant dû',         plan.nextInstallmentAmount ? plan.nextInstallmentAmount.toFixed(2) + ' TND' : '-'],
    ];

    rows.forEach(([label, value]) => {
      doc.setFont('helvetica', 'normal');
      doc.setFontSize(11);
      doc.setTextColor(100, 100, 120);
      doc.text(label + ' :', 25, y);
      doc.setFont('helvetica', 'bold');
      doc.setTextColor(26, 26, 46);
      doc.text(value, pageWidth - 25, y, { align: 'right' });
      y += 10;
    });

    doc.setFillColor(245, 240, 255);
    doc.rect(0, 275, pageWidth, 22, 'F');
    doc.setDrawColor(200, 180, 255);
    doc.line(0, 275, pageWidth, 275);
    doc.setTextColor(124, 58, 237);
    doc.setFontSize(8);
    doc.text('Formini — contact@formini.tn | www.formini.tn', pageWidth / 2, 283, { align: 'center' });

    doc.save(`plan-${plan.planId}.pdf`);
    const blob = doc.output('blob');
    window.open(URL.createObjectURL(blob), '_blank');
  }
  trackById(index: number, item: any): number {
    return item.id;
  }

  trackByPlanId(index: number, item: any): number {
    return item.planId;
  }
}