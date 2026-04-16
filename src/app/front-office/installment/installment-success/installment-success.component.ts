import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { InstallmentPlanResponse } from '../../services/cart.service';
import jsPDF from 'jspdf';

@Component({
  selector: 'app-installment-success',
  templateUrl: './installment-success.component.html',
  styleUrls: ['./installment-success.component.scss']
})
export class InstallmentSuccessComponent implements OnInit {
  plan: InstallmentPlanResponse | null = null;

  constructor(private router: Router) {}

  ngOnInit(): void {
    // Récupérer le plan depuis le state
    const state = history.state;
    if (state && state.plan) {
      this.plan = state.plan;
    }

    // Générer et ouvrir le PDF automatiquement
    if (this.plan) {
      setTimeout(() => {
        this.generateAndOpenPDF();
      }, 500);
    }
  }

  generateAndOpenPDF(): void {
    if (!this.plan) return;

    const doc = new jsPDF();
    const pageWidth = doc.internal.pageSize.getWidth();
    let y = 20;

    // ===== EN-TÊTE =====
    doc.setFillColor(124, 58, 237); // violet
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
    doc.text('FACTURE - PAIEMENT ECHELONNE', pageWidth - 20, 20, { align: 'right' });

    doc.setFontSize(10);
    doc.setFont('helvetica', 'normal');
    doc.text('Plan ID: #' + this.plan.planId, pageWidth - 20, 30, { align: 'right' });
    doc.text('Date: ' + new Date().toLocaleDateString('fr-FR'), pageWidth - 20, 38, { align: 'right' });

    y = 60;

    // ===== STATUT =====
    doc.setTextColor(6, 95, 70);
    doc.setFillColor(209, 250, 229);
    doc.roundedRect(20, y, pageWidth - 40, 14, 3, 3, 'F');
    doc.setFontSize(11);
    doc.setFont('helvetica', 'bold');
    doc.text('PLAN ACTIF - Premiere echeance payee avec succes', pageWidth / 2, y + 9, { align: 'center' });

    y += 24;

    // ===== RÉSUMÉ FINANCIER =====
    doc.setTextColor(26, 26, 46);
    doc.setFontSize(13);
    doc.setFont('helvetica', 'bold');
    doc.text('Recapitulatif Financier', 20, y);
    y += 8;

    doc.setDrawColor(124, 58, 237);
    doc.setLineWidth(0.5);
    doc.line(20, y, pageWidth - 20, y);
    y += 10;

    const drawRow = (label: string, value: string, highlight = false) => {
      if (highlight) {
        doc.setFillColor(245, 240, 255);
        doc.rect(20, y - 6, pageWidth - 40, 12, 'F');
      }
      doc.setFont('helvetica', 'normal');
      doc.setFontSize(11);
      doc.setTextColor(100, 100, 120);
      doc.text(label, 25, y);
      doc.setFont('helvetica', 'bold');
      doc.setTextColor(26, 26, 46);
      doc.text(value, pageWidth - 25, y, { align: 'right' });
      y += 12;
    };

    drawRow('Nombre d echeances:', this.plan.numberOfInstallments + 'x');
    drawRow('Montant total des cours:', this.formatAmount(this.plan.totalAmount));

    if (this.plan.feePercentage > 0) {
      const fees = this.plan.amountWithFees - this.plan.totalAmount;
      drawRow('Frais (' + this.plan.feePercentage + '%):', '+ ' + this.formatAmount(fees));
    }

    drawRow('Total avec frais:', this.formatAmount(this.plan.amountWithFees), true);
    drawRow('Montant par echeance:', this.formatAmount(this.plan.installmentAmount), true);

    y += 8;

    // ===== CALENDRIER =====
    doc.setFontSize(13);
    doc.setFont('helvetica', 'bold');
    doc.setTextColor(26, 26, 46);
    doc.text('Calendrier des Paiements', 20, y);
    y += 8;

    doc.setDrawColor(124, 58, 237);
    doc.line(20, y, pageWidth - 20, y);
    y += 8;

    // En-têtes tableau
    doc.setFillColor(124, 58, 237);
    doc.rect(20, y - 5, pageWidth - 40, 12, 'F');
    doc.setTextColor(255, 255, 255);
    doc.setFontSize(10);
    doc.setFont('helvetica', 'bold');
    doc.text('Echeance', 28, y + 3);
    doc.text('Date limite', 75, y + 3);
    doc.text('Montant', 125, y + 3);
    doc.text('Statut', 165, y + 3);
    y += 14;

    // Lignes du tableau
    if (this.plan.installments) {
      this.plan.installments.forEach((inst, index) => {
        const isEven = index % 2 === 0;
        if (isEven) {
          doc.setFillColor(250, 248, 255);
          doc.rect(20, y - 5, pageWidth - 40, 11, 'F');
        }

        doc.setTextColor(124, 58, 237);
        doc.setFont('helvetica', 'bold');
        doc.setFontSize(10);
        doc.text('Echeance ' + inst.installmentNumber, 28, y + 2);

        doc.setTextColor(100, 100, 120);
        doc.setFont('helvetica', 'normal');
        const dueDate = inst.dueDate 
          ? new Date(inst.dueDate).toLocaleDateString('fr-FR') 
          : '-';
        doc.text(dueDate, 75, y + 2);

        doc.setTextColor(26, 26, 46);
        doc.setFont('helvetica', 'bold');
        doc.text(this.formatAmount(inst.amount), 125, y + 2);

        // Badge statut
        if (inst.status === 'PAID') {
          doc.setFillColor(209, 250, 229);
          doc.roundedRect(158, y - 3, 28, 8, 2, 2, 'F');
          doc.setTextColor(6, 95, 70);
          doc.setFontSize(8);
          doc.text('PAYEE', 172, y + 2, { align: 'center' });
        } else if (inst.status === 'PENDING') {
          doc.setFillColor(240, 240, 240);
          doc.roundedRect(158, y - 3, 28, 8, 2, 2, 'F');
          doc.setTextColor(100, 100, 100);
          doc.setFontSize(8);
          doc.text('EN ATTENTE', 172, y + 2, { align: 'center' });
        }

        y += 13;
      });
    }

    y += 10;

    // ===== NOTE IMPORTANTE =====
    doc.setFillColor(255, 251, 235);
    doc.setDrawColor(252, 211, 77);
    doc.roundedRect(20, y, pageWidth - 40, 22, 3, 3, 'FD');
    doc.setTextColor(146, 64, 14);
    doc.setFontSize(9);
    doc.setFont('helvetica', 'bold');
    doc.text('Important:', 28, y + 8);
    doc.setFont('helvetica', 'normal');
    doc.text(
      "En cas d'echeance manquee, votre acces aux cours sera automatiquement suspendu.",
      28, y + 15
    );

    y += 32;

    // ===== PIED DE PAGE =====
    doc.setFillColor(245, 240, 255);
    doc.rect(0, 275, pageWidth, 22, 'F');
    doc.setTextColor(124, 58, 237);
    doc.setFontSize(9);
    doc.setFont('helvetica', 'normal');
    doc.text('Formini - Plateforme de Formation en Ligne', pageWidth / 2, 283, { align: 'center' });
    doc.text('contact@formini.tn | www.formini.tn', pageWidth / 2, 290, { align: 'center' });

    // ===== OUVRIR DANS UN NOUVEL ONGLET =====
    const pdfBlob = doc.output('blob');
    const pdfUrl = URL.createObjectURL(pdfBlob);
    window.open(pdfUrl, '_blank');

    // ===== TÉLÉCHARGER AUSSI =====
    doc.save('facture-echelonnee-' + this.plan.planId + '.pdf');
  }

  private formatAmount(amount: number): string {
    return amount.toFixed(2) + ' TND';
  }

  goToCourses(): void {
    this.router.navigate(['/trainer_course/list']);
  }

  downloadPDF(): void {
    this.generateAndOpenPDF();
  }
}