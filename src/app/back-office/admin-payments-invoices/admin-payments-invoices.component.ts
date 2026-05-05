import { Component, OnInit } from '@angular/core';
import { AdminPaymentService, Payment, Invoice } from '../services/admin-payment.service';
import jsPDF from 'jspdf';
import { CartService, RefundRequest } from '../../front-office/services/cart.service';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';


type SortField = 'id' | 'amount' | 'date' | 'status' | 'method';
type SortDirection = 'asc' | 'desc';

@Component({
  selector: 'app-admin-payments-invoices',
  templateUrl: './admin-payments-invoices.component.html',
  styleUrls: ['./admin-payments-invoices.component.scss']
})
export class AdminPaymentsInvoicesComponent implements OnInit {
  payments: Payment[] = [];
  invoices: Invoice[] = [];
  loadingPayments = false;
  loadingInvoices = false;
  error: string | null = null;

  // Filtres
  selectedTab: 'payments' | 'invoices' | 'bakchich' | 'refunds' = 'payments';
  searchTerm: string = '';
  filterStatus: string = 'all';
  filterMethod: string = 'all';
  dateFrom: string = '';
  dateTo: string = '';
  showFilters: boolean = false;

  // Tri
  sortField: SortField = 'date';
  sortDirection: SortDirection = 'desc';

  // Pagination
  currentPage: number = 1;
  itemsPerPage: number = 10;

  //Bakchich
  bakchichPending: any[] = [];
  loadingBakchich = false;
  confirming: number | null = null;

    selectedPayment: Payment | null = null;
  showPaymentModal = false;
  selectedInvoiceModal: Invoice | null = null;
  showInvoiceModal = false;

  invoicesDirect: Invoice[] = [];
  invoicesInstallment: Invoice[] = [];

  // ===== REFUNDS =====
adminName = 'Admin';
allRefunds: RefundRequest[] = [];
refundFilterStatus: 'ALL' | 'PENDING' | 'PROCESSED' | 'REJECTED' = 'PENDING';
loadingRefunds = false;
showApproveModal = false;
showRejectModal = false;
selectedRefund: RefundRequest | null = null;
rejectionReason = '';
processing = false;
get pendingRefunds(): RefundRequest[] {
  return this.allRefunds.filter(r => r.status === 'PENDING');
}
get approvedCount(): number {
  return this.allRefunds.filter(r => r.status === 'PROCESSED').length;
}
get rejectedCount(): number {
  return this.allRefunds.filter(r => r.status === 'REJECTED').length;
}
get filteredRefunds(): any[] {
  // ✅ Seulement les remboursements SANS plan échelonné
  let refunds = this.allRefunds.filter(r => !r.planId);
  
  if (this.refundFilterStatus === 'ALL') return refunds;
  return refunds.filter(r => r.status === this.refundFilterStatus);
}

  constructor(private adminPaymentService: AdminPaymentService,
    private cartService: CartService
  ) { }

  ngOnInit(): void {
    this.loadPayments();
    this.loadInvoices();
    this.loadBakchichPending();
    this.loadAllRefunds();
  }

  // ✅ loadDirectInvoices — inchangé mais invoices = uniquement directes
loadDirectInvoices(): void {
  this.loadingInvoices = true;
  this.adminPaymentService.getDirectInvoices().subscribe({
    next: (invoices) => {
      this.invoicesDirect = invoices;
      this.invoices = invoices; // ✅ invoices = direct seulement
      this.loadingInvoices = false;
    },
    error: () => {
      this.loadingInvoices = false;
      this.error = 'Erreur lors du chargement des factures directes';
    }
  });
}


  loadPayments(): void {
    this.loadingPayments = true;
    this.error = null;
    
    // Pour l'instant, on récupère les paiements de tous les learners
    // En attendant l'endpoint /payments/all dans le backend
    // On peut utiliser un learnerId par défaut ou créer un endpoint admin
    this.adminPaymentService.getAllPayments().subscribe({
      next: (payments) => {
        this.payments = payments;
        this.loadingPayments = false;
      },
      error: (error) => {
        console.error('Erreur lors du chargement des paiements:', error);
        this.error = 'Erreur lors du chargement des paiements';
        this.loadingPayments = false;
        // Fallback: essayer de charger avec un learnerId par défaut
        this.loadPaymentsByLearner(1);
      }
    });
  }

  loadPaymentsByLearner(learnerId: number): void {
    this.adminPaymentService.getPaymentsByLearner(learnerId).subscribe({
      next: (payments) => {
        this.payments = payments;
        this.loadingPayments = false;
      },
      error: (error) => {
        console.error('Erreur lors du chargement des paiements:', error);
        this.error = 'Impossible de charger les paiements';
        this.loadingPayments = false;
      }
    });
  }

  loadInvoices(): void {
    this.loadingInvoices = true;
    this.error = null;

    forkJoin({
      direct: this.adminPaymentService.getDirectInvoices().pipe(catchError(() => of([] as Invoice[]))),
      installment: this.adminPaymentService.getInstallmentInvoices().pipe(catchError(() => of([] as Invoice[]))),
      all: this.adminPaymentService.getAllInvoices().pipe(catchError(() => of([] as Invoice[])))
    }).subscribe({
      next: ({ direct, installment, all }) => {
        this.invoicesDirect = direct;
        this.invoicesInstallment = installment;

        const merged = [...direct, ...installment, ...all];
        this.invoices = merged.filter((inv, index, self) =>
          index === self.findIndex(i => i.id === inv.id)
        );

        this.loadingInvoices = false;
      },
      error: (error) => {
        console.error('Erreur lors du chargement des factures:', error);
        this.error = 'Erreur lors du chargement des factures';
        this.loadingInvoices = false;
      }
    });
  }

  loadInvoicesByLearner(learnerId: number): void {
    this.adminPaymentService.getInvoicesByLearner(learnerId).subscribe({
      next: (invoices) => {
        this.invoices = invoices;
        this.loadingInvoices = false;
      },
      error: (error) => {
        console.error('Erreur lors du chargement des factures:', error);
        this.error = 'Impossible de charger les factures';
        this.loadingInvoices = false;
      }
    });
  }
  loadBakchichPending(): void {
    this.loadingBakchich = true;
    this.cartService.getBakchichPending().subscribe({
      next: (data) => {
        this.bakchichPending = data;
        this.loadingBakchich = false;
      },
      error: () => {
        this.loadingBakchich = false;
      }
    });
  }

  confirmBakchich(id: number): void {
    this.confirming = id;
    this.cartService.confirmBakchichPayment(id, 'Agent Admin').subscribe({
      next: () => {
        this.confirming = null;
        alert('✅ Paiement Bakchich confirmé !');
        this.loadBakchichPending();
        this.loadPayments(); // Rafraîchir les paiements aussi
      },
      error: (err) => {
        this.confirming = null;
        alert('Erreur: ' + (err.error || err.message));
      }
    });
  }

  cancelBakchich(id: number): void {
    if (!confirm('Annuler ce paiement Bakchich ?')) return;
    this.cartService.cancelBakchichPayment(id).subscribe({
      next: () => {
        alert('Paiement annulé');
        this.loadBakchichPending();
      },
      error: (err) => {
        alert('Erreur: ' + (err.error || err.message));
      }
    });
  }

  isExpiringSoon(expiresAt: string): boolean {
    if (!expiresAt) return false;
    const diff = new Date(expiresAt).getTime() - new Date().getTime();
    return diff > 0 && diff < 3 * 60 * 60 * 1000; // moins de 3h
  }

  get filteredPayments(): Payment[] {
    let filtered = [...this.payments];

    // Recherche textuelle
    if (this.searchTerm) {
      const term = this.searchTerm.toLowerCase();
      filtered = filtered.filter(p => 
        p.transactionId?.toLowerCase().includes(term) ||
        p.method?.toLowerCase().includes(term) ||
        p.status?.toLowerCase().includes(term) ||
        p.learnerId?.toString().includes(term) ||
        p.id?.toString().includes(term)
      );
    }

    // Filtre par statut
    if (this.filterStatus !== 'all') {
      filtered = filtered.filter(p => p.status?.toLowerCase() === this.filterStatus.toLowerCase());
    }

    // Filtre par méthode
    if (this.filterMethod !== 'all') {
      filtered = filtered.filter(p => p.method?.toLowerCase() === this.filterMethod.toLowerCase());
    }

    // Filtre par date
    if (this.dateFrom) {
      const fromDate = new Date(this.dateFrom);
      filtered = filtered.filter(p => {
        const paymentDate = new Date(p.paymentDate);
        return paymentDate >= fromDate;
      });
    }

    if (this.dateTo) {
      const toDate = new Date(this.dateTo);
      toDate.setHours(23, 59, 59, 999);
      filtered = filtered.filter(p => {
        const paymentDate = new Date(p.paymentDate);
        return paymentDate <= toDate;
      });
    }

    // Tri
    filtered.sort((a, b) => {
      let aValue: any, bValue: any;
      
      switch (this.sortField) {
        case 'id':
          aValue = a.id || 0;
          bValue = b.id || 0;
          break;
        case 'amount':
          aValue = a.amount || 0;
          bValue = b.amount || 0;
          break;
        case 'date':
          aValue = new Date(a.paymentDate).getTime();
          bValue = new Date(b.paymentDate).getTime();
          break;
        case 'status':
          aValue = a.status || '';
          bValue = b.status || '';
          break;
        case 'method':
          aValue = a.method || '';
          bValue = b.method || '';
          break;
        default:
          return 0;
      }

      if (aValue < bValue) return this.sortDirection === 'asc' ? -1 : 1;
      if (aValue > bValue) return this.sortDirection === 'asc' ? 1 : -1;
      return 0;
    });

    return filtered;
  }

  get filteredInvoices(): Invoice[] {
    let filtered = [...this.invoices];

    // Recherche textuelle
    if (this.searchTerm) {
      const term = this.searchTerm.toLowerCase();
      filtered = filtered.filter(i => 
        i.invoiceNumber?.toLowerCase().includes(term) ||
        i.learnerId?.toString().includes(term) ||
        i.status?.toLowerCase().includes(term) ||
        i.id?.toString().includes(term)
      );
    }

    // Filtre par statut
    if (this.filterStatus !== 'all') {
      filtered = filtered.filter(i => i.status?.toLowerCase() === this.filterStatus.toLowerCase());
    }

    // Filtre par date
    if (this.dateFrom) {
      const fromDate = new Date(this.dateFrom);
      filtered = filtered.filter(i => {
        const invoiceDate = new Date(i.issueDate);
        return invoiceDate >= fromDate;
      });
    }

    if (this.dateTo) {
      const toDate = new Date(this.dateTo);
      toDate.setHours(23, 59, 59, 999);
      filtered = filtered.filter(i => {
        const invoiceDate = new Date(i.issueDate);
        return invoiceDate <= toDate;
      });
    }

    // Tri
    filtered.sort((a, b) => {
      let aValue: any, bValue: any;
      
      switch (this.sortField) {
        case 'id':
          aValue = a.id || 0;
          bValue = b.id || 0;
          break;
        case 'amount':
          aValue = a.totalAmount || 0;
          bValue = b.totalAmount || 0;
          break;
        case 'date':
          aValue = new Date(a.issueDate).getTime();
          bValue = new Date(b.issueDate).getTime();
          break;
        case 'status':
          aValue = a.status || '';
          bValue = b.status || '';
          break;
        default:
          return 0;
      }

      if (aValue < bValue) return this.sortDirection === 'asc' ? -1 : 1;
      if (aValue > bValue) return this.sortDirection === 'asc' ? 1 : -1;
      return 0;
    });

    return filtered;
  }

  get paginatedPayments(): Payment[] {
    const start = (this.currentPage - 1) * this.itemsPerPage;
    const end = start + this.itemsPerPage;
    return this.filteredPayments.slice(start, end);
  }

  get paginatedInvoices(): Invoice[] {
    const start = (this.currentPage - 1) * this.itemsPerPage;
    const end = start + this.itemsPerPage;
    return this.filteredInvoices.slice(start, end);
  }

  get totalPages(): number {
    const total = this.selectedTab === 'payments' ? this.filteredPayments.length : this.filteredInvoices.length;
    return Math.ceil(total / this.itemsPerPage);
  }

  getTotalPayments(): number {
    return this.payments.reduce((sum, p) => sum + (p.amount || 0), 0);
  }

  getTotalInvoices(): number {
    return this.invoices.reduce((sum, i) => sum + (i.totalAmount || 0), 0);
  }

  formatDate(dateString: string): string {
    if (!dateString) return '-';
    const date = new Date(dateString);
    return date.toLocaleDateString('fr-FR', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  }

  formatCurrency(amount: number): string {
    return new Intl.NumberFormat('fr-FR', {
      style: 'currency',
      currency: 'TND'
    }).format(amount);
  }

  // Tri
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
    if (this.sortField !== field) return '↕️';
    return this.sortDirection === 'asc' ? '↑' : '↓';
  }

  // Pagination
  goToPage(page: number): void {
    if (page >= 1 && page <= this.totalPages) {
      this.currentPage = page;
      window.scrollTo({ top: 0, behavior: 'smooth' });
    }
  }

  // Export
  exportToCSV(): void {
    const data = this.selectedTab === 'payments' ? this.filteredPayments : this.filteredInvoices;
    const headers = this.selectedTab === 'payments' 
      ? ['ID', 'Apprenant ID', 'Montant', 'Méthode', 'Statut', 'Transaction ID', 'Date']
      : ['Numéro Facture', 'Apprenant ID', 'Montant Total', 'Devise', 'Statut', 'Date d\'émission', 'Cours'];
    
    let csv = headers.join(',') + '\n';
    
    data.forEach(item => {
      if (this.selectedTab === 'payments') {
        const p = item as Payment;
        csv += `${p.id},${p.learnerId},${p.amount},${p.method},${p.status},${p.transactionId},${p.paymentDate}\n`;
      } else {
        const i = item as Invoice;
        csv += `${i.invoiceNumber},${i.learnerId},${i.totalAmount},${i.currency},${i.status},${i.issueDate},"${i.purchasedCourses.join('; ')}"\n`;
      }
    });
    
    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
    const link = document.createElement('a');
    const url = URL.createObjectURL(blob);
    link.setAttribute('href', url);
    link.setAttribute('download', `${this.selectedTab}_${new Date().toISOString().split('T')[0]}.csv`);
    link.style.visibility = 'hidden';
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  }

  // Statistiques
  getPaymentsByMethod(): { method: string; count: number; total: number }[] {
    const methodMap = new Map<string, { count: number; total: number }>();
    
    this.payments.forEach(p => {
      const method = p.method || 'UNKNOWN';
      const existing = methodMap.get(method) || { count: 0, total: 0 };
      methodMap.set(method, {
        count: existing.count + 1,
        total: existing.total + (p.amount || 0)
      });
    });
    
    return Array.from(methodMap.entries()).map(([method, data]) => ({
      method,
      ...data
    }));
  }

  getPaymentsByStatus(): { status: string; count: number; total: number }[] {
    const statusMap = new Map<string, { count: number; total: number }>();
    
    this.payments.forEach(p => {
      const status = p.status || 'UNKNOWN';
      const existing = statusMap.get(status) || { count: 0, total: 0 };
      statusMap.set(status, {
        count: existing.count + 1,
        total: existing.total + (p.amount || 0)
      });
    });
    
    return Array.from(statusMap.entries()).map(([status, data]) => ({
      status,
      ...data
    }));
  }

  // Actions
  viewDetails(item: Payment | Invoice): void {
    if ('transactionId' in item) {
      this.selectedPayment = item as Payment;
      this.showPaymentModal = true;
    } else {
      this.selectedInvoiceModal = item as Invoice;
      this.showInvoiceModal = true;
    }
  }

  closeModal(): void {
    this.showPaymentModal = false;
    this.showInvoiceModal = false;
    this.selectedPayment = null;
    this.selectedInvoiceModal = null;
  }

  downloadInvoice(invoice: Invoice): void {
    // TODO: Implémenter le téléchargement de facture
    window.print();
  }

  // Reset filters
  resetFilters(): void {
    this.searchTerm = '';
    this.filterStatus = 'all';
    this.filterMethod = 'all';
    this.dateFrom = '';
    this.dateTo = '';
    this.currentPage = 1;
  }

  // Helper methods for charts
  getMethodColor(method: string): string {
    const colors: { [key: string]: string } = {
      'FLOUCI': '#8b5cf6',
      'WAFA_CASH': '#10b981',
      'BAKCHICH': '#3b82f6',
      'CARTE': '#f59e0b',
      'PAYPAL': '#06b6d4'
    };
    return colors[method.toUpperCase()] || '#64748b';
  }

  getStatusColor(status: string): string {
    const colors: { [key: string]: string } = {
      'SUCCESS': '#10b981',
      'PAID': '#10b981',
      'PENDING': '#f59e0b',
      'CANCELLED': '#ef4444',
      'FAILED': '#ef4444'
    };
    return colors[status.toUpperCase()] || '#64748b';
  }
  // ✅ Supprimer une facture
deleteInvoice(invoice: Invoice): void {
  if (!confirm(`Supprimer la facture ${invoice.invoiceNumber} ?`)) return;
  this.adminPaymentService.deleteInvoice(invoice.id).subscribe({
    next: () => {
      this.invoices = this.invoices.filter(i => i.id !== invoice.id);
      this.invoicesDirect = this.invoicesDirect.filter(i => i.id !== invoice.id);
    },
    error: () => {
      alert('Erreur lors de la suppression de la facture');
    }
  });
}

// ✅ Générer et ouvrir la facture PDF dans une nouvelle fenêtre
openInvoicePDF(invoice: Invoice): void {
  const doc = new jsPDF()  // ou import jsPDF si déjà importé
  const pageWidth = doc.internal.pageSize.getWidth();

  // Header violet
  doc.setFillColor(139, 92, 246);
  doc.rect(0, 0, pageWidth, 40, 'F');
  doc.setTextColor(255, 255, 255);
  doc.setFontSize(18);
  doc.setFont('helvetica', 'bold');
  doc.text('FORMINI - Facture', 20, 18);
  doc.setFontSize(10);
  doc.setFont('helvetica', 'normal');
  doc.text('N° ' + invoice.invoiceNumber, 20, 30);
  doc.text('Date: ' + new Date(invoice.issueDate).toLocaleDateString('fr-FR'), pageWidth - 20, 30, { align: 'right' });

  let y = 55;

  // Infos facture
  doc.setTextColor(26, 26, 46);
  doc.setFontSize(13);
  doc.setFont('helvetica', 'bold');
  doc.text('Détails de la Facture', 20, y);
  y += 6;
  doc.setDrawColor(139, 92, 246);
  doc.line(20, y, pageWidth - 20, y);
  y += 12;

  const rows = [
    ['Numéro Facture', invoice.invoiceNumber],
    ['Learner ID', '#' + invoice.learnerId],
    ['Montant Total', invoice.totalAmount.toFixed(2) + ' TND'],
    ['Devise', invoice.currency],
    ['Statut', invoice.status],
    ['Date d\'émission', new Date(invoice.issueDate).toLocaleDateString('fr-FR')],
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

  // Cours achetés
  doc.setFontSize(13);
  doc.setFont('helvetica', 'bold');
  doc.setTextColor(26, 26, 46);
  doc.text('Cours Achetés', 20, y);
  y += 6;
  doc.line(20, y, pageWidth - 20, y);
  y += 10;

  invoice.purchasedCourses?.forEach((course, i) => {
    doc.setFillColor(i % 2 === 0 ? 250 : 245, 248, 255);
    doc.rect(20, y - 5, pageWidth - 40, 10, 'F');
    doc.setFont('helvetica', 'normal');
    doc.setFontSize(11);
    doc.setTextColor(80, 80, 80);
    doc.text('• ' + course, 25, y + 1);
    y += 12;
  });

  // Footer
  doc.setFillColor(245, 240, 255);
  doc.rect(0, 275, pageWidth, 22, 'F');
  doc.setTextColor(139, 92, 246);
  doc.setFontSize(8);
  doc.text('Formini - contact@formini.tn', pageWidth / 2, 285, { align: 'center' });

  // ✅ Ouvrir dans une nouvelle fenêtre automatiquement
  const blob = doc.output('blob');
  const url = URL.createObjectURL(blob);
  window.open(url, '_blank');
}
deletePayment(payment: Payment): void {
  if (!confirm(`Supprimer le paiement #${payment.id} de ${this.formatCurrency(payment.amount)} ?`)) return;
  this.adminPaymentService.deletePayment(payment.id).subscribe({
    next: () => {
      this.payments = this.payments.filter(p => p.id !== payment.id);
    },
    error: () => {
      alert('Erreur lors de la suppression du paiement');
    }
  });
}
loadAllRefunds(): void {
  this.loadingRefunds = true;
  this.cartService.getAllRefunds().subscribe({
    next: (refunds: RefundRequest[]) => {
      this.allRefunds = refunds.sort((a, b) =>
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

openApproveModal(refund: RefundRequest): void {
  this.selectedRefund = refund;
  this.showApproveModal = true;
}

openRejectModal(refund: RefundRequest): void {
  this.selectedRefund = refund;
  this.rejectionReason = '';
  this.showRejectModal = true;
}

closeRefundModals(): void {
  this.showApproveModal = false;
  this.showRejectModal = false;
  this.selectedRefund = null;
  this.rejectionReason = '';
}

confirmApprove(): void {
  if (!this.selectedRefund) return;
  this.processing = true;
  this.cartService.approveRefund(this.selectedRefund.id, this.adminName).subscribe({
    next: (updated: RefundRequest) => {
      const i = this.allRefunds.findIndex(r => r.id === updated.id);
      if (i !== -1) this.allRefunds[i] = updated;
      this.processing = false;
      this.closeRefundModals();
      alert('✅ Remboursement approuvé ! Avoir N° ' + updated.creditNoteNumber);
    },
    error: (err: any) => {
      this.processing = false;
      alert('❌ Erreur: ' + (err.error || err.message));
    }
  });
}

confirmReject(): void {
  if (!this.selectedRefund || !this.rejectionReason.trim()) return;
  this.processing = true;
  this.cartService.rejectRefund(
    this.selectedRefund.id, this.adminName, this.rejectionReason.trim()
  ).subscribe({
    next: (updated: RefundRequest) => {
      const i = this.allRefunds.findIndex(r => r.id === updated.id);
      if (i !== -1) this.allRefunds[i] = updated;
      this.processing = false;
      this.closeRefundModals();
      alert('❌ Demande rejetée.');
    },
    error: (err: any) => {
      this.processing = false;
      alert('❌ Erreur: ' + (err.error || err.message));
    }
  });
}
}

