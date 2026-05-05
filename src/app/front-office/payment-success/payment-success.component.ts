import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { CartService, Invoice } from '../services/cart.service';
import jsPDF from 'jspdf';
import { UserService } from '../services/user.service';
import { User } from '../../user';

@Component({
  selector: 'app-payment-success',
  templateUrl: './payment-success.component.html',
  styleUrls: ['./payment-success.component.scss']
})
export class PaymentSuccessComponent implements OnInit {
  invoiceNumber: string | null = null;
  invoice: Invoice | null = null;
  loading = true;
  currentUser: User | null = null;
  private emailTriggered = false;
  emailStatus: 'idle' | 'sending' | 'sent' | 'failed' = 'idle';
  emailErrorMessage = '';
  emailDebug = {
    endpoint: 'http://localhost:8085/msenrollment/invoices/send-email',
    payload: '' as string,
    status: '' as string,
    response: '' as string,
  };

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private cartService: CartService,
    private userService: UserService,
  ) {}

  async ngOnInit(): Promise<void> {
    this.currentUser = this.userService.getUser() || null;
    if (!this.currentUser) {
      this.currentUser = (await this.userService.loadUser()) || null;
    }

    this.invoiceNumber = this.route.snapshot.queryParams['invoiceNumber'];

    if (this.invoiceNumber) {
      this.loadInvoice();
      return;
    }

    if (this.currentUser?.id) {
      this.loadLatestInvoiceForLearner(this.currentUser.id);
    } else {
      this.loading = false;
    }
  }

  loadLatestInvoiceForLearner(learnerId: string): void {
    this.cartService.getAllInvoices(learnerId).subscribe({
      next: (invoices) => {
        if (invoices?.length) {
          const sortedInvoices = [...invoices].sort((a, b) =>
            new Date(b.issueDate).getTime() - new Date(a.issueDate).getTime()
          );
          this.invoice = sortedInvoices[0];
          this.invoiceNumber = this.invoice.invoiceNumber;
          this.sendInvoiceToCurrentUser();
        }
        this.loading = false;
      },
      error: (error) => {
        console.error('Erreur lors du chargement des factures du learner:', error);
        this.loading = false;
      }
    });
  }

  loadInvoice(): void {
    if (!this.invoiceNumber) return;

    this.cartService.getInvoiceByNumber(this.invoiceNumber).subscribe({
      next: (invoice) => {
        this.invoice = invoice;
        this.sendInvoiceToCurrentUser();
        this.loading = false;
      },
      error: (error) => {
        console.error('Erreur lors du chargement de la facture:', error);
        this.loading = false;
      }
    });
  }

  goToCourses(): void {
    this.router.navigate(['/learner/courses']);
  }

  sendInvoiceToCurrentUser(): void {
    if (this.emailTriggered) return;
    const invoiceNumber = this.invoice?.invoiceNumber;
    const invoiceId = this.invoice?.id;
    const learnerId = this.invoice?.learnerId || this.currentUser?.id;
    const email = this.currentUser?.email;

    if (!invoiceNumber || !email) {
      this.emailStatus = 'failed';
      this.emailErrorMessage = 'Email utilisateur ou numéro de facture manquant.';
      this.emailDebug.status = 'VALIDATION_ERROR';
      return;
    }

    this.emailStatus = 'sending';
    this.emailErrorMessage = '';
    this.emailDebug.payload = JSON.stringify({ invoiceNumber, invoiceId, learnerId, email });
    this.emailDebug.status = 'REQUEST_SENT';
    this.emailDebug.response = '';

    this.cartService.sendInvoiceToEmail({ invoiceNumber, invoiceId, learnerId, email }).subscribe({
      next: (response) => {
        this.emailTriggered = true;
        this.emailStatus = 'sent';
        this.emailDebug.status = `HTTP ${response.status}`;
        this.emailDebug.response = JSON.stringify(response.body ?? {});
        console.log(`Facture ${invoiceNumber} envoyée à ${email}`);
      },
      error: (error) => {
        this.emailStatus = 'failed';
        const backendBody = typeof error?.error === 'string' ? error.error : JSON.stringify(error?.error ?? {});
        this.emailErrorMessage = error?.error?.message || error?.message || 'Erreur envoi email.';
        this.emailDebug.status = `HTTP ${error?.status ?? 'N/A'}`;
        this.emailDebug.response = backendBody;
        console.error('Erreur lors de l\'envoi email de facture:', error);
      }
    });
  }

  resendInvoiceEmail(): void {
    this.emailTriggered = false;
    this.sendInvoiceToCurrentUser();
  }

  downloadInvoice(): void {
  if (!this.invoice) return;

  const doc = new jsPDF();
  const pageWidth = doc.internal.pageSize.getWidth();
  let y = 20;

  // ===== EN-TÊTE =====
  doc.setFillColor(124, 58, 237);
  doc.rect(0, 0, pageWidth, 45, 'F');
  doc.setTextColor(255, 255, 255);
  doc.setFontSize(22);
  doc.setFont('helvetica', 'bold');
  doc.text('FORMINI', 20, 20);
  doc.setFontSize(11);
  doc.setFont('helvetica', 'normal');
  doc.text('Plateforme de Formation en Ligne', 20, 30);
  doc.setFontSize(16);
  doc.setFont('helvetica', 'bold');
  doc.text('FACTURE DE PAIEMENT', pageWidth - 20, 20, { align: 'right' });
  doc.setFontSize(10);
  doc.setFont('helvetica', 'normal');
  doc.text('N°: ' + this.invoice.invoiceNumber, pageWidth - 20, 30, { align: 'right' });
  doc.text('Date: ' + new Date().toLocaleDateString('fr-FR'), pageWidth - 20, 38, { align: 'right' });

  y = 60;

  // ===== STATUT =====
  doc.setTextColor(6, 95, 70);
  doc.setFillColor(209, 250, 229);
  doc.roundedRect(20, y, pageWidth - 40, 14, 3, 3, 'F');
  doc.setFontSize(11);
  doc.setFont('helvetica', 'bold');
  doc.text('PAIEMENT CONFIRME - Merci pour votre achat !', pageWidth / 2, y + 9, { align: 'center' });

  y += 24;

  // ===== DÉTAILS =====
  doc.setTextColor(26, 26, 46);
  doc.setFontSize(13);
  doc.setFont('helvetica', 'bold');
  doc.text('Détails de la Commande', 20, y);
  y += 8;
  doc.setDrawColor(124, 58, 237);
  doc.setLineWidth(0.5);
  doc.line(20, y, pageWidth - 20, y);
  y += 12;

  // ===== COURS =====
  if (this.invoice.purchasedCourses) {
    this.invoice.purchasedCourses.forEach((course, index) => {
      const isEven = index % 2 === 0;
      if (isEven) {
        doc.setFillColor(250, 248, 255);
        doc.rect(20, y - 6, pageWidth - 40, 12, 'F');
      }
      doc.setTextColor(100, 100, 120);
      doc.setFont('helvetica', 'normal');
      doc.setFontSize(11);
      doc.text('• ' + course, 25, y);
      const price = this.invoice!.totalAmount / this.invoice!.purchasedCourses.length;
      doc.setFont('helvetica', 'bold');
      doc.setTextColor(26, 26, 46);
      doc.text(price.toFixed(2) + ' TND', pageWidth - 25, y, { align: 'right' });
      y += 14;
    });
  }

  y += 5;
  doc.setDrawColor(200, 200, 200);
  doc.line(20, y, pageWidth - 20, y);
  y += 12;

  // ===== TOTAL =====
  doc.setFillColor(245, 240, 255);
  doc.rect(20, y - 6, pageWidth - 40, 14, 'F');
  doc.setFontSize(13);
  doc.setFont('helvetica', 'bold');
  doc.setTextColor(124, 58, 237);
  doc.text('TOTAL', 25, y + 2);
  doc.text(this.invoice.totalAmount.toFixed(2) + ' TND', pageWidth - 25, y + 2, { align: 'right' });

  y += 25;

  // ===== PIED DE PAGE =====
  doc.setFillColor(245, 240, 255);
  doc.rect(0, 275, pageWidth, 22, 'F');
  doc.setTextColor(124, 58, 237);
  doc.setFontSize(9);
  doc.setFont('helvetica', 'normal');
  doc.text('Formini - Plateforme de Formation en Ligne', pageWidth / 2, 283, { align: 'center' });
  doc.text('contact@formini.tn | www.formini.tn', pageWidth / 2, 290, { align: 'center' });

  // ===== OUVRIR DANS NOUVEL ONGLET =====
  const pdfBlob = doc.output('blob');
  const pdfUrl = URL.createObjectURL(pdfBlob);
  window.open(pdfUrl, '_blank'); // ✅ Ouvre dans un nouvel onglet

  // ===== TÉLÉCHARGER AUSSI =====
  doc.save('facture-' + this.invoice.invoiceNumber + '.pdf');
}

printInvoice(): void {
  this.downloadInvoice(); // ✅ Même fonction
}
}
