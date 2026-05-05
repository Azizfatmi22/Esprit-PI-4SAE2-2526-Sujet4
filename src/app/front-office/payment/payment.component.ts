import { Component, OnInit } from '@angular/core';
import { Router, ActivatedRoute } from '@angular/router';
import { CartService, PaymentRequest, Invoice } from '../services/cart.service';
import { HttpClient } from '@angular/common/http';
import { User } from '../../user';
import { UserService } from '../services/user.service';

@Component({
  selector: 'app-payment',
  templateUrl: './payment.component.html',
  styleUrls: ['./payment.component.scss']
})
export class PaymentComponent implements OnInit {
  Currentuser: User | null = null;
  totalAmount = 0;
  selectedMethod: 'FLOUCI' | 'WAFA_CASH' | 'BAKCHICH' | null = null;
  loading = false;
  processing = false;

  tunisianPaymentForm = {
    phoneNumber: '',
    otp: ''
  };
  // ✅ NOUVELLES PROPRIÉTÉS OTP — ajouter seulement ces lignes
  showOtpScreen = false;
  transactionRef = '';
  flouciDebugStatus = '';
  flouciDebugMessage = '';
  flouciDebugRef = '';
  otpCode = '';
  otpTimer = 120;
  canResend = false;
  timerInterval: any;

  wafaBalance: number | null = null;
  wafaBalanceChecked = false;
  wafaBalanceSufficient = false;
  wafaChecking = false;

  showBakchichScreen = false;
  bakchichCode = '';
  bakchichQrData = '';
  bakchichExpiresAt = '';
  bakchichAmount = 0;

  couponCode = '';
  couponResult: any = null;
  couponLoading = false;

  constructor(
    private cartService: CartService,
    private http: HttpClient,
    public router: Router,
    private route: ActivatedRoute,
    private userService: UserService,
    
  ) {}

  ngOnInit(): void {
    this.Currentuser = this.userService.getUser() || null;
    this.loadCartTotal();
  }
    get learnerId(): string | null {
      return this.Currentuser?.id ?? null;
    }

    private getLearnerIdOrWarn(): string | null {
      const learnerId = this.learnerId;
      if (!learnerId) {
        alert('Utilisateur non connecté. Veuillez vous reconnecter.');
        return null;
      }
      return learnerId;
    }

   // ✅ AJOUTER ngOnDestroy pour éviter les fuites mémoire
  ngOnDestroy(): void {
    if (this.timerInterval) clearInterval(this.timerInterval);
  }


  loadCartTotal(): void {
    const learnerId = this.getLearnerIdOrWarn();
    if (!learnerId) {
      return;
    }

    this.cartService.getCart(learnerId).subscribe({
      next: (cart) => {
        this.totalAmount = this.cartService.getCartTotal(cart);
        if (!cart || !cart.items || cart.items.length === 0) {
          alert('Votre panier est vide. Veuillez ajouter des cours avant de payer.');
          this.router.navigate(['/cart']);
        }
      },
      error: (error) => {
        console.error('Erreur lors du chargement du panier:', error);
        alert('Impossible de charger le panier. Veuillez réessayer.');
      }
    });
  }

  selectPaymentMethod(method: 'FLOUCI' | 'WAFA_CASH' | 'BAKCHICH'): void {
    this.selectedMethod = method;
  }
  get finalAmount(): number {
    return this.couponResult?.valid ? this.couponResult.finalAmount : this.totalAmount;
  }

  // ✅ Appliquer le coupon
  applyCoupon(): void {
    if (!this.couponCode.trim()) return;
    const learnerId = this.getLearnerIdOrWarn();
    if (!learnerId) {
      return;
    }
    this.couponLoading = true;

    this.cartService.validateCoupon(
      this.couponCode.trim(), learnerId, this.totalAmount
    ).subscribe({
      next: (result) => {
        this.couponLoading = false;
        this.couponResult = result;
      },
      error: () => {
        this.couponLoading = false;
        this.couponResult = { valid: false, message: 'Erreur serveur' };
      }
    });
  }

  // ✅ Retirer le coupon
  removeCoupon(): void {
    this.couponResult = null;
    this.couponCode = '';
  }

  processPayment(): void {
    const learnerId = this.getLearnerIdOrWarn();
    if (!learnerId) {
      return;
    }

    if (!this.selectedMethod) {
      alert('Veuillez sélectionner une méthode de paiement');
      return;
    }

    if (!this.isTunisianPaymentFormValid()) {
      alert('Veuillez remplir votre numéro de téléphone');
      return;
    }

    if (this.totalAmount <= 0) {
      alert('Votre panier est vide. Veuillez ajouter des cours avant de payer.');
      this.router.navigate(['/cart']);
      return;
    }
    if (this.selectedMethod === 'FLOUCI') {
      this.initiateFlouciPayment();
      return;
    }
    if (this.selectedMethod === 'WAFA_CASH') {
      this.processWafaPayment();
      return;
    }
    if (this.selectedMethod === 'BAKCHICH') {
  this.processBakchichPayment();
  return;
}

    this.processing = true;

    const paymentRequest: PaymentRequest = {
      amount: this.totalAmount,
      method: this.selectedMethod!,
      phoneNumber: this.tunisianPaymentForm.phoneNumber,
      couponCode: this.couponResult?.valid ? this.couponCode.trim().toUpperCase() : undefined
    };


    this.cartService.confirmPayment(learnerId, paymentRequest).subscribe({
      next: (response) => {
        this.processing = false;
        this.router.navigate(['/cart/success'], {
          queryParams: { invoiceNumber: response.invoiceNumber }
        });
      },
      error: (error) => {
        this.processing = false;
        let errorMessage = 'Erreur lors du traitement du paiement.';
        if (error.error) {
          if (typeof error.error === 'string') {
            errorMessage = error.error;
          } else if (error.error.message) {
            errorMessage = error.error.message;
          }
        }
        alert('Erreur: ' + errorMessage);
      }
    });
  }

  processWafaPayment(): void {
  const learnerId = this.getLearnerIdOrWarn();
  if (!learnerId) {
    return;
  }

  this.processing = true;

  // Étape 1 : vérifier le solde
  this.cartService.checkWafaBalance(
      this.tunisianPaymentForm.phoneNumber,
      this.totalAmount
    ).subscribe({
      next: (result) => {
        this.wafaChecking = false;
        this.wafaBalance = result.balance;
        this.wafaBalanceChecked = true;
        this.wafaBalanceSufficient = result.sufficient;

        if (!result.sufficient) {
          this.processing = false;
          // Solde insuffisant → afficher message et arrêter
          alert(
            '💚 Wafa Cash\n\n' +
            '❌ Solde insuffisant !\n' +
            'Solde disponible : ' + result.balance + ' TND\n' +
            'Montant requis : ' + this.totalAmount + ' TND'
          );
          return;
        }

        // Étape 2 : solde OK → confirmer le paiement
        this.processing = true;
        this.cartService.payWithWafa(
          learnerId,
          this.tunisianPaymentForm.phoneNumber,
          this.totalAmount
        ).subscribe({
          next: (response) => {
            this.processing = false;
            const invoiceNumber = response?.invoiceNumber;
            if (invoiceNumber) {
              this.router.navigate(['/cart/success'], {
                queryParams: { invoiceNumber }
              });
              return;
            }
            this.router.navigate(['/cart/success']);
          },
          error: (err) => {
            this.processing = false;
            const backendMessage = err?.error?.message || err?.error?.text || err?.error || err?.message;
            alert('Erreur Wafa Cash : ' + backendMessage);
          }
        });
      },
      error: (err) => {
        this.wafaChecking = false;
        alert('Impossible de vérifier le solde : ' + (err.error || err.message));
      }
    });
  }
  

  // ✅ AJOUTER — afficher le solde quand on saisit le numéro
  onPhoneNumberChange(): void {
    if (
      this.selectedMethod === 'WAFA_CASH' &&
      this.tunisianPaymentForm.phoneNumber.length >= 8
    ) {
      this.cartService.getWafaBalance(
        this.tunisianPaymentForm.phoneNumber
      ).subscribe({
        next: (result) => {
          this.wafaBalance = result.balance;
        },
        error: () => {
          this.wafaBalance = null;
        }
      });
    }
  }
  processBakchichPayment(): void {
    const learnerId = this.getLearnerIdOrWarn();
    if (!learnerId) {
      return;
    }

    this.processing = true;
    this.cartService.generateBakchichCode(
      learnerId,
      this.tunisianPaymentForm.phoneNumber,
      this.totalAmount
    ).subscribe({
      next: (response) => {
        this.processing = false;
        this.bakchichCode = response.paymentCode;
        this.bakchichQrData = response.qrCodeData;
        this.bakchichExpiresAt = response.expiresAt;
        this.bakchichAmount = response.amount;
        this.showBakchichScreen = true;
      },
      error: (err) => {
        this.processing = false;
        alert('Erreur: ' + (err.error || err.message));
      }
    });
  }


  // ✅ NOUVELLES MÉTHODES OTP — ajouter à la fin
  initiateFlouciPayment(): void {
    const learnerId = this.getLearnerIdOrWarn();
    if (!learnerId) {
      return;
    }

    this.processing = true;
    const phoneNumber = this.tunisianPaymentForm.phoneNumber.trim();
    this.flouciDebugStatus = 'INIT_REQUEST';
    this.flouciDebugMessage = 'Initiation OTP en cours...';
    this.flouciDebugRef = '';

    this.cartService.initiateFlouciPayment(
      learnerId,
      phoneNumber,
      this.finalAmount
    ).subscribe({
      next: (response) => {
        this.processing = false;

        this.flouciDebugStatus = response?.status || 'INIT_RESPONSE';
        this.flouciDebugMessage = response?.message || 'Réponse reçue depuis le backend.';

        const transactionRef =
          response?.transactionRef ||
          response?.transactionId ||
          response?.reference ||
          '';

        this.flouciDebugRef = transactionRef || '';

        if (!transactionRef) {
          const backendMessage = response?.message || 'Aucune référence de transaction reçue.';
          alert(`OTP non envoyé: ${backendMessage}`);
          return;
        }

        if (response?.otpSent === false || response?.status === 'FAILED') {
          const backendMessage = response?.message || 'Échec d\'envoi du code OTP.';
          alert(`OTP non envoyé: ${backendMessage}`);
          return;
        }

        this.transactionRef = transactionRef;
        this.showOtpScreen = true;
        this.startTimer();
      },
      error: (err) => {
        this.processing = false;
        const errorMessage = err?.error?.message || err?.error || err?.message || 'Erreur inconnue';
        this.flouciDebugStatus = 'INIT_ERROR';
        this.flouciDebugMessage = errorMessage;
        this.flouciDebugRef = '';
        alert('Erreur envoi OTP: ' + errorMessage);
      }
    });
  }

  startTimer(): void {
    this.otpTimer = 120;
    this.canResend = false;
    this.timerInterval = setInterval(() => {
      this.otpTimer--;
      if (this.otpTimer <= 0) {
        clearInterval(this.timerInterval);
        this.canResend = true;
      }
    }, 1000);
  }

  get timerDisplay(): string {
    const m = Math.floor(this.otpTimer / 60);
    const s = this.otpTimer % 60;
    return `${m}:${s.toString().padStart(2, '0')}`;
  }

  confirmOtp(): void {
  const learnerId = this.getLearnerIdOrWarn();
  if (!learnerId) {
    return;
  }

  if (this.otpCode.length !== 6) {
    alert('Veuillez entrer un code à 6 chiffres');
    return;
  }
  this.processing = true;
  this.flouciDebugStatus = 'VERIFY_REQUEST';
  this.flouciDebugMessage = 'Vérification OTP en cours...';

  this.cartService.verifyFlouciOtp(
    learnerId,
    this.transactionRef,
    this.otpCode,
    this.couponResult?.valid ? this.couponCode.trim().toUpperCase() : undefined  // ✅
  ).subscribe({
    next: (response) => {
      this.processing = false;
      this.flouciDebugStatus = response?.paymentStatus || 'VERIFY_OK';
      this.flouciDebugMessage = response?.message || 'OTP validé avec succès.';
      clearInterval(this.timerInterval);
      this.router.navigate(['/cart/success'], {
        queryParams: { invoiceNumber: response.invoiceNumber }
      });
    },
    error: (err) => {
      this.processing = false;
      this.flouciDebugStatus = 'VERIFY_ERROR';
      this.flouciDebugMessage = err?.error?.message || 'Code incorrect ou expiré.';
      alert('Code incorrect ou expiré. Réessayez.');
    }
  });
}
  resendOtp(): void {
    if (!this.transactionRef) {
      alert('Référence de transaction manquante. Relancez le paiement.');
      return;
    }

    this.cartService.resendFlouciOtp(this.transactionRef).subscribe({
      next: () => {
        this.flouciDebugStatus = 'RESEND_OK';
        this.flouciDebugMessage = 'Renvoi OTP confirmé par le backend.';
        this.flouciDebugRef = this.transactionRef;
        this.startTimer();
        alert('Nouveau code envoyé !');
      },
      error: (err) => {
        const resendMessage = err?.error?.message || err?.error || err?.message;
        this.flouciDebugStatus = 'RESEND_ERROR';
        this.flouciDebugMessage = resendMessage;
        this.flouciDebugRef = this.transactionRef;
        alert('Erreur renvoi OTP: ' + resendMessage);
      }
    });
  }


  // ✅ NOUVEAU : Redirection vers le paiement en plusieurs fois
  goToInstallment(): void {
    if (this.totalAmount <= 0) {
      alert('Votre panier est vide.');
      return;
    }
    this.router.navigate(['/cart/installment']);
  }

  isTunisianPaymentFormValid(): boolean {
    return !!this.tunisianPaymentForm.phoneNumber &&
           this.tunisianPaymentForm.phoneNumber.trim().length > 0;
  }

  goBack(): void {
    this.router.navigate(['/cart']);
  }
  createTestCoupon(): void {
  this.http.post('http://localhost:8085/msenrollment/coupons/create', {
    code: 'PROMO20',
    description: 'Test -20%',
    discountType: 'PERCENTAGE',
    discountValue: 20,
    maxUsages: 999,
    currentUsages: 0,
    isActive: true,
    type: 'GENERAL',
    minOrderAmount: 0,
    validUntil: '2027-12-31T23:59:59'
  }).subscribe({
    next: (data: any) => {
      alert('✅ Coupon créé ! Code: PROMO20\nMaintenant tapez PROMO20 dans le champ code promo');
    },
    error: (err) => {
      if (err.status === 500 && err.error?.includes('Duplicate')) {
        alert('✅ Coupon PROMO20 existe déjà !\nTapez PROMO20 dans le champ code promo');
      } else {
        alert('❌ Erreur: ' + JSON.stringify(err.error));
      }
    }
  });
}

checkCoupons(): void {
  this.http.get('http://localhost:8085/msenrollment/coupons/all').subscribe({
    next: (data: any) => {
      if (data.length === 0) {
        alert('❌ Aucun coupon en base !\nCliquez sur "Créer Coupon Test" d\'abord');
      } else {
        const codes = data.map((c: any) => 
          `• ${c.code} — ${c.discountType === 'PERCENTAGE' ? c.discountValue + '%' : c.discountValue + ' TND'} (actif: ${c.isActive})`
        ).join('\n');
        alert('✅ Coupons disponibles:\n\n' + codes);
      }
    },
    error: () => alert('❌ Impossible de charger les coupons')
  });
}
}