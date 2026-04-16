import { Component, OnInit } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { AdminInstallmentService, InstallmentPlan, Installment, InstallmentRefundRequest } from '../../services/admin-installment.service';
import jsPDF from 'jspdf';
import { CartService } from '../../../front-office/services/cart.service';
import { AdminPaymentService } from '../../services/admin-payment.service';


type SortField = 'planId' | 'learnerId' | 'totalAmount' | 'status' | 'createdAt' | 'numberOfInstallments';
type SortDirection = 'asc' | 'desc';



@Component({
  selector: 'app-admin-installments',
  templateUrl: './admin-installments.component.html',
  styleUrls: ['./admin-installments.component.scss']
})
export class AdminInstallmentsComponent implements OnInit {

  plans: InstallmentPlan[] = [];
  loading = false;
  error: string | null = null;
  successMessage: string | null = null;

  bakchichPending: any[] = [];
  loadingBakchich = false;
  confirmingBakchich: number | null = null;
  

  invoicesInstallment: any[] = [];
  loadingInvoices = false;
  selectedTab: 'plans' | 'bakchich' | 'invoices' | 'refunds' = 'plans';

  // Refunds
adminName = 'Admin';
allInstallmentRefunds: InstallmentRefundRequest[] = [];
refundFilterStatus: 'ALL' | 'PENDING' | 'PROCESSED' | 'REJECTED' = 'PENDING';
loadingRefunds = false;
showApproveRefundModal = false;
showRejectRefundModal = false;
selectedRefund: InstallmentRefundRequest | null = null;
rejectionReason = '';
processingRefund = false;



  // Détail plan sélectionné
  selectedPlan: InstallmentPlan | null = null;
  showDetailModal = false;

  // Filtres
  showFilters = false;
  searchTerm = '';
  filterStatus = 'all';
  filterInstallments = 'all';
  dateFrom = '';
  dateTo = '';

  // Tri
  sortField: SortField = 'createdAt';
  sortDirection: SortDirection = 'desc';

  // Pagination
  currentPage = 1;
  itemsPerPage = 10;

  invoiceCurrentPage = 1;
invoiceItemsPerPage = 10;

  // Chargement multi-learners (simulation)
  learnerIds = [1, 2, 3, 4, 5];

  constructor(private adminInstallmentService: AdminInstallmentService,
    private cartService: CartService,
    private adminPaymentService: AdminPaymentService
  ) {}

  ngOnInit(): void {
    this.loadInstallmentInvoices();
    this.loadAllPlans();
    this.loadBakchichInstallmentPending();
    this.loadAllInstallmentRefunds();
  }

  loadInstallmentInvoices(): void {
  this.loadingInvoices = true;
  this.adminPaymentService.getInstallmentInvoices().subscribe({
    next: (data) => {
      this.invoicesInstallment = data;
      this.invoiceCurrentPage = 1;
      this.loadingInvoices = false;
    },
    error: () => {
      this.loadingInvoices = false;
    }
  });
}

private getLearnerIdsFromInstallmentInvoices(): number[] {
  const ids = this.invoicesInstallment
    .map(inv => Number(inv?.learnerId))
    .filter(id => Number.isFinite(id) && id > 0);

  const unique = Array.from(new Set(ids));
  return unique.length > 0 ? unique : this.learnerIds;
}

private extractErrorMessage(error: unknown): string {
  if (error instanceof HttpErrorResponse) {
    const payload = error.error;
    if (typeof payload === 'string' && payload.trim()) return payload;
    if (payload && typeof payload === 'object') {
      const message = (payload as any).message || (payload as any).error;
      if (typeof message === 'string' && message.trim()) return message;
    }
    return `HTTP ${error.status}${error.statusText ? ` - ${error.statusText}` : ''}`;
  }
  return 'Erreur serveur inconnue';
}

formatCurrencyInvoice(amount: number): string {
  return new Intl.NumberFormat('fr-FR', { style: 'currency', currency: 'TND' }).format(amount);
}
  loadBakchichInstallmentPending(): void {
  this.loadingBakchich = true;
  // ✅ Utiliser getBakchichPendingInstallment au lieu de getBakchichPending
  this.cartService.getBakchichPendingInstallment().subscribe({
    next: (data) => {
      this.bakchichPending = data;
      this.loadingBakchich = false;
    },
    error: () => {
      this.loadingBakchich = false;
    }
  });
}

confirmBakchichInstallment(id: number): void {
  this.confirmingBakchich = id;
  this.cartService.confirmBakchichPayment(id, 'Agent Admin').subscribe({
    next: () => {
      this.confirmingBakchich = null;
      this.successMessage = '✅ Paiement Bakchich installment confirmé !';
      setTimeout(() => this.successMessage = null, 4000);
      this.loadBakchichInstallmentPending();
      this.loadAllPlans();
    },
    error: (err) => {
      this.confirmingBakchich = null;
      this.error = 'Erreur: ' + (err.error || err.message);
      setTimeout(() => this.error = null, 4000);
    }
  });
}

cancelBakchichInstallment(id: number): void {
  if (!confirm('Annuler ce paiement Bakchich ?')) return;
  this.cartService.cancelBakchichPayment(id).subscribe({
    next: () => {
      this.successMessage = 'Paiement Bakchich annulé';
      setTimeout(() => this.successMessage = null, 4000);
      this.loadBakchichInstallmentPending();
    },
    error: (err) => {
      this.error = 'Erreur: ' + (err.error || err.message);
      setTimeout(() => this.error = null, 4000);
    }
  });
}

isExpiringSoon(expiresAt: string): boolean {
  if (!expiresAt) return false;
  const diff = new Date(expiresAt).getTime() - new Date().getTime();
  return diff > 0 && diff < 3 * 60 * 60 * 1000;
}

  loadAllPlans(): void {
    this.loading = true;
    this.error = null;
    this.plans = [];

    this.adminInstallmentService.getAllPlans().subscribe({
    next: (plans: InstallmentPlan[]) => {
      console.log('Plans chargés:', plans); // vérifiez learnerId et createdAt
      this.plans = plans;
      this.loading = false;
    },
    error: (allPlansErr) => {
      const learnerIds = this.getLearnerIdsFromInstallmentInvoices();
      this.adminInstallmentService.getAllPlansByLearners(learnerIds).subscribe({
        next: (plans: InstallmentPlan[]) => {
          this.plans = plans;
          this.loading = false;
          if (!plans.length) {
            this.error = `Aucun plan trouvé (fallback actif). Détail: ${this.extractErrorMessage(allPlansErr)}`;
          }
        },
        error: (fallbackErr) => {
          this.error = `Erreur lors du chargement des plans: ${this.extractErrorMessage(allPlansErr)} | Fallback: ${this.extractErrorMessage(fallbackErr)}`;
          this.loading = false;
        }
      });
    }
  });
}

  triggerOverdueCheck(): void {
    this.adminInstallmentService.checkOverdue().subscribe({
      next: () => {
        this.successMessage = 'Vérification des échéances effectuée avec succès !';
        setTimeout(() => this.successMessage = null, 4000);
        this.loadAllPlans();
      },
      error: () => {
        this.error = 'Erreur lors de la vérification des échéances';
        setTimeout(() => this.error = null, 4000);
      }
    });
  }

  openDetail(plan: InstallmentPlan): void {
    this.selectedPlan = plan;
    this.showDetailModal = true;
  }

  closeDetail(): void {
    this.selectedPlan = null;
    this.showDetailModal = false;
  }

  // ===== FILTRES =====
  get filteredPlans(): InstallmentPlan[] {
    let filtered = [...this.plans];

    if (this.searchTerm) {
      const term = this.searchTerm.toLowerCase();
      filtered = filtered.filter(p =>
        p.planId?.toString().includes(term) ||
        p.learnerId?.toString().includes(term) ||
        p.status?.toLowerCase().includes(term)
      );
    }

    if (this.filterStatus !== 'all') {
      filtered = filtered.filter(p =>
        p.status?.toLowerCase() === this.filterStatus.toLowerCase()
      );
    }

    if (this.filterInstallments !== 'all') {
      filtered = filtered.filter(p =>
        p.numberOfInstallments?.toString() === this.filterInstallments
      );
    }

    if (this.dateFrom) {
      const from = new Date(this.dateFrom);
      filtered = filtered.filter(p => new Date(p.createdAt) >= from);
    }

    if (this.dateTo) {
      const to = new Date(this.dateTo);
      to.setHours(23, 59, 59);
      filtered = filtered.filter(p => new Date(p.createdAt) <= to);
    }

    filtered.sort((a, b) => {
      let aVal: any, bVal: any;
      switch (this.sortField) {
        case 'planId':          aVal = a.planId;               bVal = b.planId;               break;
        case 'learnerId':       aVal = a.learnerId;            bVal = b.learnerId;            break;
        case 'totalAmount':     aVal = a.totalAmount;          bVal = b.totalAmount;          break;
        case 'status':          aVal = a.status;               bVal = b.status;               break;
        case 'createdAt':       aVal = new Date(a.createdAt);  bVal = new Date(b.createdAt);  break;
        case 'numberOfInstallments': aVal = a.numberOfInstallments; bVal = b.numberOfInstallments; break;
        default: return 0;
      }
      if (aVal < bVal) return this.sortDirection === 'asc' ? -1 : 1;
      if (aVal > bVal) return this.sortDirection === 'asc' ? 1 : -1;
      return 0;
    });

    return filtered;
  }

  get paginatedPlans(): InstallmentPlan[] {
    const start = (this.currentPage - 1) * this.itemsPerPage;
    return this.filteredPlans.slice(start, start + this.itemsPerPage);
  }

  get totalPages(): number {
    return Math.ceil(this.filteredPlans.length / this.itemsPerPage);
  }

  sort(field: SortField): void {
    if (this.sortField === field) {
      this.sortDirection = this.sortDirection === 'asc' ? 'desc' : 'asc';
    } else {
      this.sortField = field;
      this.sortDirection = 'asc';
    }
    this.currentPage = 1;
  }

  getSortIcon(field: SortField): string {
    if (this.sortField !== field) return '↕';
    return this.sortDirection === 'asc' ? '↑' : '↓';
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
    this.filterInstallments = 'all';
    this.dateFrom = '';
    this.dateTo = '';
    this.currentPage = 1;
  }

  // ===== STATISTIQUES =====
  getTotalRevenue(): number {
    return this.plans
      .filter(p => p.status !== 'DEFAULTED')
      .reduce((sum, p) => sum + (p.amountWithFees || 0), 0);
  }

  getActivePlans(): number {
    return this.plans.filter(p => p.status === 'ACTIVE').length;
  }

  getDefaultedPlans(): number {
    return this.plans.filter(p => p.status === 'DEFAULTED').length;
  }

  getCompletedPlans(): number {
    return this.plans.filter(p => p.status === 'COMPLETED').length;
  }

  getPlansByStatus(): { status: string; count: number; total: number }[] {
    const map = new Map<string, { count: number; total: number }>();
    this.plans.forEach(p => {
      const s = p.status || 'UNKNOWN';
      const ex = map.get(s) || { count: 0, total: 0 };
      map.set(s, { count: ex.count + 1, total: ex.total + (p.amountWithFees || 0) });
    });
    return Array.from(map.entries()).map(([status, data]) => ({ status, ...data }));
  }

  getPlansByInstallments(): { count: number; nb: number; total: number }[] {
    const map = new Map<number, { count: number; total: number }>();
    this.plans.forEach(p => {
      const n = p.numberOfInstallments;
      const ex = map.get(n) || { count: 0, total: 0 };
      map.set(n, { count: ex.count + 1, total: ex.total + (p.amountWithFees || 0) });
    });
    return Array.from(map.entries()).map(([nb, data]) => ({ nb, ...data }));
  }

  // ===== EXPORT CSV =====
  exportToCSV(): void {
    const headers = ['Plan ID', 'Learner ID', 'Total', 'Avec frais', 'Frais %', 'Nb echeances', 'Par echeance', 'Statut', 'Date creation'];
    let csv = headers.join(',') + '\n';

    this.filteredPlans.forEach(p => {
      csv += `${p.planId},${p.learnerId},${p.totalAmount},${p.amountWithFees},${p.feePercentage},${p.numberOfInstallments},${p.installmentAmount},${p.status},${p.createdAt}\n`;
    });

    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
    const link = document.createElement('a');
    link.setAttribute('href', URL.createObjectURL(blob));
    link.setAttribute('download', `installments_${new Date().toISOString().split('T')[0]}.csv`);
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  }

  // ===== PDF PAR PLAN =====
  downloadPlanPDF(plan: InstallmentPlan): void {
    const doc = new jsPDF();
    const pageWidth = doc.internal.pageSize.getWidth();

    // Header
    doc.setFillColor(124, 58, 237);
    doc.rect(0, 0, pageWidth, 40, 'F');
    doc.setTextColor(255, 255, 255);
    doc.setFontSize(18);
    doc.setFont('helvetica', 'bold');
    doc.text('FORMINI - Plan de Paiement Echelonne', 20, 18);
    doc.setFontSize(10);
    doc.setFont('helvetica', 'normal');
    doc.text('Plan ID: #' + plan.planId + '  |  Learner: #' + plan.learnerId, 20, 30);
    doc.text('Date: ' + new Date(plan.createdAt).toLocaleDateString('fr-FR'), pageWidth - 20, 30, { align: 'right' });

    let y = 55;

    // Résumé
    doc.setTextColor(26, 26, 46);
    doc.setFontSize(13);
    doc.setFont('helvetica', 'bold');
    doc.text('Recapitulatif', 20, y);
    y += 6;
    doc.setDrawColor(124, 58, 237);
    doc.line(20, y, pageWidth - 20, y);
    y += 10;

    const rows = [
      ['Plan', plan.numberOfInstallments + 'x paiements'],
      ['Montant total', plan.totalAmount.toFixed(2) + ' TND'],
      ['Frais', plan.feePercentage + '%'],
      ['Total avec frais', plan.amountWithFees.toFixed(2) + ' TND'],
      ['Par echeance', plan.installmentAmount.toFixed(2) + ' TND'],
      ['Statut', plan.status],
    ];

    rows.forEach(([label, value]) => {
      doc.setFont('helvetica', 'normal');
      doc.setFontSize(11);
      doc.setTextColor(100, 100, 120);
      doc.text(label + ':', 25, y);
      doc.setFont('helvetica', 'bold');
      doc.setTextColor(26, 26, 46);
      doc.text(value, pageWidth - 25, y, { align: 'right' });
      y += 10;
    });

    y += 8;

    // Tableau échéances
    doc.setFontSize(13);
    doc.setFont('helvetica', 'bold');
    doc.setTextColor(26, 26, 46);
    doc.text('Echeances', 20, y);
    y += 6;
    doc.line(20, y, pageWidth - 20, y);
    y += 8;

    // En-têtes
    doc.setFillColor(124, 58, 237);
    doc.rect(20, y - 5, pageWidth - 40, 11, 'F');
    doc.setTextColor(255, 255, 255);
    doc.setFontSize(9);
    doc.text('N°', 27, y + 2);
    doc.text('Date limite', 55, y + 2);
    doc.text('Montant', 105, y + 2);
    doc.text('Date paiement', 135, y + 2);
    doc.text('Statut', 175, y + 2);
    y += 13;

    plan.installments?.forEach((inst: Installment, i: number) => {
      if (i % 2 === 0) {
        doc.setFillColor(250, 248, 255);
        doc.rect(20, y - 5, pageWidth - 40, 10, 'F');
      }

      doc.setTextColor(124, 58, 237);
      doc.setFont('helvetica', 'bold');
      doc.setFontSize(9);
      doc.text('#' + inst.installmentNumber, 27, y + 1);

      doc.setTextColor(80, 80, 80);
      doc.setFont('helvetica', 'normal');
      doc.text(new Date(inst.dueDate).toLocaleDateString('fr-FR'), 55, y + 1);
      doc.text(inst.amount.toFixed(2) + ' TND', 105, y + 1);
      doc.text(inst.paidDate ? new Date(inst.paidDate).toLocaleDateString('fr-FR') : '-', 135, y + 1);

      // Badge statut
      const statusColors: any = {
        PAID: [209, 250, 229],
        PENDING: [240, 240, 240],
        OVERDUE: [254, 226, 226],
        FAILED: [254, 226, 226]
      };
      const statusTextColors: any = {
        PAID: [6, 95, 70],
        PENDING: [100, 100, 100],
        OVERDUE: [153, 27, 27],
        FAILED: [153, 27, 27]
      };
      const bg = statusColors[inst.status] || [240, 240, 240];
      const tc = statusTextColors[inst.status] || [100, 100, 100];
      doc.setFillColor(bg[0], bg[1], bg[2]);
      doc.roundedRect(170, y - 3, 22, 7, 1, 1, 'F');
      doc.setTextColor(tc[0], tc[1], tc[2]);
      doc.setFontSize(7);
      doc.text(inst.status, 181, y + 2, { align: 'center' });

      y += 12;
    });

    // Footer
    doc.setFillColor(245, 240, 255);
    doc.rect(0, 275, pageWidth, 22, 'F');
    doc.setTextColor(124, 58, 237);
    doc.setFontSize(8);
    doc.text('Formini - contact@formini.tn', pageWidth / 2, 285, { align: 'center' });

    const blob = doc.output('blob');
    window.open(URL.createObjectURL(blob), '_blank');
    doc.save('plan-' + plan.planId + '.pdf');
  }

  // Helpers
  getPaidCount(plan: InstallmentPlan): number {
    return plan.installments?.filter((i: Installment) => i.status === 'PAID').length || 0;
  }

  formatDate(d: string): string {
    if (!d) return '-';
    return new Date(d).toLocaleDateString('fr-FR', {
      year: 'numeric', month: 'short', day: 'numeric',
      hour: '2-digit', minute: '2-digit'
    });
  }

  formatCurrency(amount: number): string {
    return new Intl.NumberFormat('fr-FR', { style: 'currency', currency: 'TND' }).format(amount);
  }

  getStatusColor(status: string): string {
    const colors: any = { 'ACTIVE': '#10b981', 'COMPLETED': '#3b82f6', 'DEFAULTED': '#ef4444' };
    return colors[status] || '#64748b';
  }

  getPagesArray(): number[] {
    return Array.from({ length: this.totalPages }, (_, i) => i + 1);
  }
  // ✅ Supprimer une facture installment
deleteInstallmentInvoice(invoice: any): void {
  if (!confirm(`Supprimer la facture ${invoice.invoiceNumber} ?`)) return;
  this.adminPaymentService.deleteInvoice(invoice.id).subscribe({
    next: () => {
      this.invoicesInstallment = this.invoicesInstallment.filter(i => i.id !== invoice.id);
      if (this.invoiceCurrentPage > this.invoiceTotalPages && this.invoiceTotalPages > 0) {
        this.invoiceCurrentPage = this.invoiceTotalPages;}
    },
    error: () => {
      this.error = 'Erreur lors de la suppression de la facture';
      setTimeout(() => this.error = null, 4000);
    }
  });
}

// ✅ Voir les détails d'une facture dans un modal
selectedInvoiceDetail: any = null;
showInvoiceModal = false;

openInvoiceDetail(invoice: any): void {
  this.selectedInvoiceDetail = invoice;
  this.showInvoiceModal = true;
}

closeInvoiceDetail(): void {
  this.selectedInvoiceDetail = null;
  this.showInvoiceModal = false;
}

// ✅ Générer et ouvrir la facture PDF dans une nouvelle fenêtre
openInstallmentInvoicePDF(invoice: any): void {
  const doc = new jsPDF();
  const pageWidth = doc.internal.pageSize.getWidth();

  // Header violet
  doc.setFillColor(124, 58, 237);
  doc.rect(0, 0, pageWidth, 40, 'F');
  doc.setTextColor(255, 255, 255);
  doc.setFontSize(18);
  doc.setFont('helvetica', 'bold');
  doc.text('FORMINI - Facture Echelonnee', 20, 18);
  doc.setFontSize(10);
  doc.setFont('helvetica', 'normal');
  doc.text('N° ' + invoice.invoiceNumber, 20, 30);
  doc.text('Date: ' + new Date(invoice.issueDate).toLocaleDateString('fr-FR'), pageWidth - 20, 30, { align: 'right' });

  let y = 55;

  doc.setTextColor(26, 26, 46);
  doc.setFontSize(13);
  doc.setFont('helvetica', 'bold');
  doc.text('Details de la Facture', 20, y);
  y += 6;
  doc.setDrawColor(124, 58, 237);
  doc.line(20, y, pageWidth - 20, y);
  y += 12;

  const rows = [
    ['Numero Facture', invoice.invoiceNumber],
    ['Learner ID', '#' + invoice.learnerId],
    ['Plan Installment', '#' + invoice.installmentPlanId],
    ['Montant', invoice.totalAmount.toFixed(2) + ' TND'],
    ['Devise', invoice.currency],
    ['Statut', invoice.status],
    ['Date emission', new Date(invoice.issueDate).toLocaleDateString('fr-FR')],
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

  doc.setFontSize(13);
  doc.setFont('helvetica', 'bold');
  doc.setTextColor(26, 26, 46);
  doc.text('Cours', 20, y);
  y += 6;
  doc.line(20, y, pageWidth - 20, y);
  y += 10;

  invoice.purchasedCourses?.forEach((course: string, i: number) => {
    doc.setFillColor(i % 2 === 0 ? 250 : 245, 248, 255);
    doc.rect(20, y - 5, pageWidth - 40, 10, 'F');
    doc.setFont('helvetica', 'normal');
    doc.setFontSize(11);
    doc.setTextColor(80, 80, 80);
    doc.text('• ' + course, 25, y + 1);
    y += 12;
  });

  doc.setFillColor(245, 240, 255);
  doc.rect(0, 275, pageWidth, 22, 'F');
  doc.setTextColor(124, 58, 237);
  doc.setFontSize(8);
  doc.text('Formini - contact@formini.tn', pageWidth / 2, 285, { align: 'center' });

  const blob = doc.output('blob');
  window.open(URL.createObjectURL(blob), '_blank');
}
// ===== GETTERS PAGINATION FACTURES =====
get filteredInvoices(): any[] {
  return this.invoicesInstallment; // tu peux ajouter filtres ici plus tard
}

get paginatedInvoices(): any[] {
  const start = (this.invoiceCurrentPage - 1) * this.invoiceItemsPerPage;
  return this.filteredInvoices.slice(start, start + this.invoiceItemsPerPage);
}

get invoiceTotalPages(): number {
  return Math.ceil(this.filteredInvoices.length / this.invoiceItemsPerPage);
}

getInvoicePagesArray(): number[] {
  return Array.from({ length: this.invoiceTotalPages }, (_, i) => i + 1);
}

goToInvoicePage(page: number): void {
  if (page >= 1 && page <= this.invoiceTotalPages) {
    this.invoiceCurrentPage = page;
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }
}
deletePlan(plan: InstallmentPlan): void {
  if (!confirm(`Supprimer le plan #${plan.planId} (Learner #${plan.learnerId}) ?\nCette action est irréversible.`)) return;
  this.adminInstallmentService.deletePlan(plan.planId).subscribe({
    next: () => {
      this.plans = this.plans.filter(p => p.planId !== plan.planId);
      this.successMessage = `✅ Plan #${plan.planId} supprimé avec succès`;
      setTimeout(() => this.successMessage = null, 4000);
      // Ajuster la page si elle devient vide
      if (this.currentPage > this.totalPages && this.totalPages > 0) {
        this.currentPage = this.totalPages;
      }
    },
    error: () => {
      this.error = 'Erreur lors de la suppression du plan';
      setTimeout(() => this.error = null, 4000);
    }
  });
}
// ===== GETTERS refunds =====

get pendingInstallmentRefunds(): InstallmentRefundRequest[] {
  return this.allInstallmentRefunds.filter(r => r.planId != null && r.status === 'PENDING');
}

get approvedInstallmentCount(): number {
  return this.allInstallmentRefunds.filter(r => r.planId != null && r.status === 'PROCESSED').length;
}

get rejectedInstallmentCount(): number {
  return this.allInstallmentRefunds.filter(r => r.planId != null && r.status === 'REJECTED').length;
}
get filteredInstallmentRefunds(): InstallmentRefundRequest[] {
  // ✅ Seulement les remboursements AVEC un plan échelonné
  let refunds = this.allInstallmentRefunds.filter(r => r.planId != null);
  
  if (this.refundFilterStatus === 'ALL') return refunds;
  return refunds.filter(r => r.status === this.refundFilterStatus);
}
// ===== MÉTHODES refunds =====

loadAllInstallmentRefunds(): void {
  this.loadingRefunds = true;
  this.adminInstallmentService.getAllInstallmentRefunds().subscribe({
    next: (refunds) => {
      this.allInstallmentRefunds = refunds.sort((a, b) =>
        new Date(b.requestDate).getTime() - new Date(a.requestDate).getTime()
      );
      this.loadingRefunds = false;
    },
    error: () => { this.loadingRefunds = false; }
  });
}

setRefundFilter(status: 'ALL' | 'PENDING' | 'PROCESSED' | 'REJECTED'): void {
  this.refundFilterStatus = status;
}

openApproveRefundModal(refund: InstallmentRefundRequest): void {
  this.selectedRefund = refund;
  this.showApproveRefundModal = true;
}

openRejectRefundModal(refund: InstallmentRefundRequest): void {
  this.selectedRefund = refund;
  this.rejectionReason = '';
  this.showRejectRefundModal = true;
}

closeInstallmentRefundModals(): void {
  this.showApproveRefundModal = false;
  this.showRejectRefundModal = false;
  this.selectedRefund = null;
  this.rejectionReason = '';
}

confirmInstallmentApprove(): void {
  if (!this.selectedRefund) return;
  this.processingRefund = true;
  this.adminInstallmentService.approveInstallmentRefund(
    this.selectedRefund.id, this.adminName
  ).subscribe({
    next: (updated) => {
      const i = this.allInstallmentRefunds.findIndex(r => r.id === updated.id);
      if (i !== -1) this.allInstallmentRefunds[i] = updated;
      this.processingRefund = false;
      this.closeInstallmentRefundModals();
      this.successMessage = '✅ Remboursement approuvé ! Avoir N° ' + updated.creditNoteNumber;
      setTimeout(() => this.successMessage = null, 5000);
    },
    error: (err) => {
      this.processingRefund = false;
      this.error = '❌ Erreur: ' + (err.error || err.message);
      setTimeout(() => this.error = null, 4000);
    }
  });
}

confirmInstallmentReject(): void {
  if (!this.selectedRefund || !this.rejectionReason.trim()) return;
  this.processingRefund = true;
  this.adminInstallmentService.rejectInstallmentRefund(
    this.selectedRefund.id, this.adminName, this.rejectionReason.trim()
  ).subscribe({
    next: (updated) => {
      const i = this.allInstallmentRefunds.findIndex(r => r.id === updated.id);
      if (i !== -1) this.allInstallmentRefunds[i] = updated;
      this.processingRefund = false;
      this.closeInstallmentRefundModals();
      this.successMessage = 'Demande rejetée.';
      setTimeout(() => this.successMessage = null, 4000);
    },
    error: (err) => {
      this.processingRefund = false;
      this.error = '❌ Erreur: ' + (err.error || err.message);
      setTimeout(() => this.error = null, 4000);
    }
  });
}

truncate(text: string | undefined | null, max: number = 60): string {
  if (!text) return '';
  return text.length > max ? text.slice(0, max) + '...' : text;
}
}