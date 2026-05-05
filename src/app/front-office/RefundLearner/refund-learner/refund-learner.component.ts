import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { CartService, Invoice, RefundRequest } from '../../services/cart.service';
import { UserService } from '../../services/user.service';
import { User } from '../../../user';

@Component({
  selector: 'app-refund-learner',
  templateUrl: './refund-learner.component.html',
  styleUrls: ['./refund-learner.component.scss']
})
export class RefundLearnerComponent implements OnInit {
  currentUser: User | null = null;
  learnerId: string | null = null;
  activeTab: 'invoices' | 'refunds' = 'invoices';

  invoices: Invoice[] = [];
  myRefunds: RefundRequest[] = [];
  invoicesLoadError = '';

  loadingInvoices = true;
  loadingRefunds = false;

  showRefundModal = false;
  selectedInvoice: Invoice | null = null;
  refundReason = '';
  submitting = false;

  get pendingCount(): number {
    return this.myRefunds.filter(r => r.status === 'PENDING').length;
  }

  constructor(
    private cartService: CartService,
    private router: Router,
    private userService: UserService
  ) {}

  async ngOnInit(): Promise<void> {
    this.currentUser = this.userService.getUser() || null;
    if (!this.currentUser) {
      this.currentUser = (await this.userService.loadUser()) || null;
    }

    this.learnerId = this.currentUser?.id ?? null;
    if (!this.learnerId) {
      alert('Utilisateur non connecté. Veuillez vous reconnecter.');
      this.router.navigate(['/login']);
      return;
    }

    this.loadInvoices();
    this.loadMyRefunds();
  }

  private getCandidateIds(): string[] {
    return Array.from(new Set([
      this.learnerId,
      this.currentUser?.username,
      this.currentUser?.email
    ].filter((value): value is string => !!value && value.trim().length > 0)));
  }

  private dedupeInvoices(items: Invoice[]): Invoice[] {
    return items.filter((invoice, index, self) =>
      index === self.findIndex(i => i.id === invoice.id)
    );
  }

  private dedupeRefunds(items: RefundRequest[]): RefundRequest[] {
    return items.filter((refund, index, self) =>
      index === self.findIndex(r => r.id === refund.id)
    );
  }

  loadInvoices(): void {
    if (!this.learnerId) {
      return;
    }

    const candidateIds = this.getCandidateIds();
    this.loadingInvoices = true;
    this.invoicesLoadError = '';

    const requests = candidateIds.map(id =>
      forkJoin({
        all: this.cartService.getAllInvoices(id).pipe(catchError(() => of([] as Invoice[]))),
        byLearner: this.cartService.getInvoicesByLearner(id).pipe(catchError(() => of([] as Invoice[]))),
        direct: this.cartService.getDirectInvoices(id).pipe(catchError(() => of([] as Invoice[]))),
        installment: this.cartService.getInstallmentInvoices(id).pipe(catchError(() => of([] as Invoice[])))
      })
    );

    forkJoin(requests).subscribe({
      next: (results) => {
        const primary = results.flatMap(bundle => bundle.all);
        const fallback = results.flatMap(bundle => [
          ...bundle.byLearner,
          ...bundle.direct,
          ...bundle.installment
        ]);

        const merged = primary.length > 0 ? primary : fallback;

        this.invoices = this.dedupeInvoices(merged)
          .sort((a, b) => new Date(b.issueDate).getTime() - new Date(a.issueDate).getTime());

        if (this.invoices.length === 0) {
          this.invoicesLoadError = 'Aucune facture trouvée pour votre compte.';
        }

        this.loadingInvoices = false;
      },
      error: () => {
        this.invoicesLoadError = 'Impossible de charger les factures pour le moment.';
        this.loadingInvoices = false;
      }
    });
  }

  loadMyRefunds(): void {
    if (!this.learnerId) {
      return;
    }

    const candidateIds = this.getCandidateIds();
    this.loadingRefunds = true;

    const requests = candidateIds.map(id =>
      this.cartService.getMyRefunds(id).pipe(catchError(() => of([] as RefundRequest[])))
    );

    forkJoin(requests).subscribe({
      next: (results) => {
        this.myRefunds = this.dedupeRefunds(results.flatMap(items => items));
        this.loadingRefunds = false;
      },
      error: () => {
        this.loadingRefunds = false;
      }
    });
  }

  isDeadlineExpired(issueDate: string): boolean {
    const deadline = new Date(issueDate);
    deadline.setDate(deadline.getDate() + 14);
    return new Date() > deadline;
  }

  getDeadline(issueDate: string): Date {
    const deadline = new Date(issueDate);
    deadline.setDate(deadline.getDate() + 14);
    return deadline;
  }

  hasExistingRefund(invoiceId: number): boolean {
    return this.myRefunds.some(
      r => r.invoiceId === invoiceId && r.status === 'PENDING'
    );
  }

  openRefundModal(invoice: Invoice): void {
    this.selectedInvoice = invoice;
    this.refundReason = '';
    this.showRefundModal = true;
  }

  closeModal(): void {
    this.showRefundModal = false;
    this.selectedInvoice = null;
    this.refundReason = '';
  }

  submitRefundRequest(): void {
    if (!this.refundReason.trim() || !this.selectedInvoice) {
      return;
    }

    const candidateIds = this.getCandidateIds();
    if (candidateIds.length === 0) {
      alert('Utilisateur non connecté.');
      return;
    }

    this.submitting = true;

    const invoiceId = this.selectedInvoice.id;
    const reason = this.refundReason.trim();

    const tryRequest = (index: number): void => {
      const id = candidateIds[index];
      this.cartService.requestRefund(id, invoiceId, reason).subscribe({
        next: (refund: RefundRequest) => {
          this.submitting = false;
          this.myRefunds = this.dedupeRefunds([...this.myRefunds, refund]);
          this.closeModal();
          alert('✅ Demande envoyée ! Vous recevrez une réponse sous 48h.');
          this.activeTab = 'refunds';
        },
        error: (err: any) => {
          if (index < candidateIds.length - 1) {
            tryRequest(index + 1);
            return;
          }

          this.submitting = false;
          alert('❌ Erreur: ' + (err?.error?.message || err?.error || err?.message || 'Erreur inconnue'));
        }
      });
    };

    tryRequest(0);
  }

  goBack(): void {
    this.router.navigate(['/cart/success']);
  }
}
