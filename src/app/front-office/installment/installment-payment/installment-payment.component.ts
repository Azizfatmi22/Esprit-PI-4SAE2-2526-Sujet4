import { Component, OnInit, OnDestroy } from '@angular/core';
import { Router } from '@angular/router';
import { CartService, InstallmentPlanRequest } from '../../services/cart.service';
import { UserService } from '../../services/user.service';
import { User } from '../../../user';


@Component({
  selector: 'app-installment-payment',
  templateUrl: './installment-payment.component.html',
  styleUrls: ['./installment-payment.component.scss']
})
export class InstallmentPaymentComponent implements OnInit, OnDestroy {
  Currentuser: User | null = null;
  totalAmount = 0;
  selectedPlan: 3 | 6 = 3;
  selectedMethod: 'FLOUCI' | 'WAFA_CASH' | 'BAKCHICH' = 'FLOUCI';
  phoneNumber = '';
  processing = false;
  planPreview: any = null;
  installmentRange: number[] = [];

  // ===== FLOUCI OTP =====
  showOtpScreen = false;
  transactionRef = '';
  otpCode = '';
  otpTimer = 120;
  canResend = false;
  timerInterval: any;

  // ===== WAFA CASH =====
  wafaBalance: number | null = null;
  wafaChecking = false;

  // ===== BAKCHICH =====
  showBakchichScreen = false;
  bakchichCode = '';
  bakchichQrData = '';
  bakchichExpiresAt = '';
  bakchichAmount = 0;

  constructor(
    private cartService: CartService,
    public router: Router,
    private userService: UserService
  ) {}
  savedCourseTitles: string[] = [];

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

  async ngOnInit(): Promise<void> {
    this.Currentuser = this.userService.getUser() || null;
    if (!this.Currentuser) {
      this.Currentuser = (await this.userService.loadUser()) || null;
    }

    const learnerId = this.getLearnerIdOrWarn();
    if (!learnerId) {
      return;
    }

    this.cartService.getCart(learnerId).subscribe({
      next: (cart) => {
        this.totalAmount = this.cartService.getCartTotal(cart);
        if (cart && cart.items) {
          this.savedCourseTitles = cart.items.map(item => item.courseTitle);
        }
        this.updatePreview();
      },
      error: (err) => {
        alert('Impossible de charger le panier : ' + (err?.error || err?.message || 'Erreur inconnue'));
      }
    });
  }

  ngOnDestroy(): void {
    if (this.timerInterval) clearInterval(this.timerInterval);
  }

  updatePreview(): void {
    const fee = this.selectedPlan === 6 ? 0.05 : 0;
    const totalWithFee = this.totalAmount * (1 + fee);
    const perInstallment = totalWithFee / this.selectedPlan;

    this.planPreview = {
      totalAmount: this.totalAmount,
      feePercentage: fee * 100,
      feeAmount: this.totalAmount * fee,
      totalWithFee,
      perInstallment: Math.round(perInstallment * 100) / 100,
      numberOfInstallments: this.selectedPlan
    };

    this.installmentRange = Array.from(
      { length: this.selectedPlan }, (_, i) => i
    );
  }

  selectPlan(plan: 3 | 6): void {
    this.selectedPlan = plan;
    this.updatePreview();
  }

  selectMethod(method: 'FLOUCI' | 'WAFA_CASH' | 'BAKCHICH'): void {
    this.selectedMethod = method;
    // Réinitialiser les états
    this.wafaBalance = null;
    this.showOtpScreen = false;
    this.showBakchichScreen = false;
  }

  // ===== POINT D'ENTRÉE PRINCIPAL =====
  confirmInstallmentPayment(): void {
    if (!this.phoneNumber.trim()) {
      alert('Veuillez entrer votre numéro de téléphone');
      return;
    }

    if (this.selectedMethod === 'FLOUCI') {
      this.processInstallmentFlouci();
      return;
    }

    if (this.selectedMethod === 'WAFA_CASH') {
      this.processInstallmentWafa();
      return;
    }

    if (this.selectedMethod === 'BAKCHICH') {
      this.processInstallmentBakchich();
      return;
    }
  }

  // ===== FLOUCI =====
  processInstallmentFlouci(): void {
    const learnerId = this.getLearnerIdOrWarn();
    if (!learnerId) {
      return;
    }

    this.processing = true;
    this.cartService.initiateFlouciPayment(
      learnerId,
      this.phoneNumber,
      this.planPreview.totalWithFee
    ).subscribe({
      next: (response) => {
        this.processing = false;
        this.transactionRef = response.transactionRef;
        this.showOtpScreen = true;
        this.startTimer();
      },
      error: (err) => {
        this.processing = false;
        alert('Erreur Flouci : ' + (err.error || err.message));
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

  // ✅ D'abord vérifier l'OTP, PUIS créer le plan
  this.cartService.verifyFlouciOtp(
    learnerId,
    this.transactionRef,
    this.otpCode
  ).subscribe({
    next: () => {
      // OTP OK → créer le plan
      const request: InstallmentPlanRequest = {
        numberOfInstallments: this.selectedPlan,
        paymentMethod: this.selectedMethod,
        phoneNumber: this.phoneNumber,
        totalAmount: this.totalAmount,
        courseTitles: this.savedCourseTitles
      };

      this.cartService.createInstallmentPlan(learnerId, request).subscribe({
        next: (planResponse) => {
          this.processing = false;
          clearInterval(this.timerInterval);
          this.router.navigate(['/cart/installment-success'], {
            state: { plan: planResponse }
          });
        },
        error: (error) => {
          this.processing = false;
          alert('Erreur création plan : ' + (error.error || error.message));
        }
      });
    },
    error: () => {
      this.processing = false;
      alert('Code incorrect ou expiré. Réessayez.');
    }
  });
}

  resendOtp(): void {
    this.cartService.resendFlouciOtp(this.transactionRef).subscribe({
      next: () => {
        this.startTimer();
        alert('Nouveau code envoyé !');
      },
      error: (err) => alert('Erreur: ' + err.error)
    });
  }

  // ===== WAFA CASH — ajouter ces propriétés =====
  // ===== WAFA CASH — ajouter ces propriétés =====
wafaBalanceChecked = false;
wafaBalanceSufficient = false;
showWafaConfirmScreen = false;  // ✅ nouvel écran de confirmation

// ===== WAFA CASH — remplacer processInstallmentWafa =====
processInstallmentWafa(): void {
  this.wafaChecking = true;
  this.processing = true;

  this.cartService.checkWafaBalance(
    this.phoneNumber,
    this.planPreview.totalWithFee
  ).subscribe({
    next: (result) => {
      this.wafaChecking = false;
      this.processing = false;
      this.wafaBalance = result.balance;
      this.wafaBalanceChecked = true;
      this.wafaBalanceSufficient = result.sufficient;

      if (!result.sufficient) {
        alert(
          '💚 Wafa Cash\n\n' +
          '❌ Solde insuffisant !\n' +
          'Solde disponible : ' + result.balance + ' TND\n' +
          'Montant requis : ' + this.planPreview.totalWithFee + ' TND'
        );
        return;
      }

      // ✅ Afficher l'écran de confirmation comme dans payment
      this.showWafaConfirmScreen = true;
    },
    error: (err) => {
      this.wafaChecking = false;
      this.processing = false;
      alert('Impossible de vérifier le solde : ' + (err.error || err.message));
    }
  });
}

// ✅ Confirmation finale Wafa — appelée depuis le bouton "Confirmer"
confirmWafaInstallment(): void {
  const learnerId = this.getLearnerIdOrWarn();
  if (!learnerId) {
    return;
  }

  this.processing = true;
  this.showWafaConfirmScreen = false;

  // Créer le plan (pendant que le panier existe encore)
  const request: InstallmentPlanRequest = {
    numberOfInstallments: this.selectedPlan,
    paymentMethod: this.selectedMethod,
    phoneNumber: this.phoneNumber,
    totalAmount: this.totalAmount,
    courseTitles: this.savedCourseTitles
  };

  this.cartService.createInstallmentPlan(learnerId, request).subscribe({
    next: (planResponse) => {
      // Plan créé → payer avec Wafa la 1ère mensualité
      this.cartService.payWithWafa(
        learnerId,
        this.phoneNumber,
        this.planPreview.perInstallment
      ).subscribe({
        next: () => {
          this.processing = false;
          this.router.navigate(['/cart/installment-success'], {
            state: { plan: planResponse }
          });
        },
        error: (err) => {
          this.processing = false;
          // ✅ Plan créé mais paiement échoué — naviguer quand même
          console.error('Wafa pay error:', err);
          this.router.navigate(['/cart/installment-success'], {
            state: { plan: planResponse }
          });
        }
      });
    },
    error: (error) => {
      this.processing = false;
      alert('Erreur création plan : ' + (error.error || error.message));
    }
  });
}
  onPhoneNumberChange(): void {
    if (
      this.selectedMethod === 'WAFA_CASH' &&
      this.phoneNumber.length >= 8
    ) {
      this.cartService.getWafaBalance(this.phoneNumber).subscribe({
        next: (result) => { this.wafaBalance = result.balance; },
        error: () => { this.wafaBalance = null; }
      });
    }
  }

  // ===== BAKCHICH =====
  processInstallmentBakchich(): void {
  const learnerId = this.getLearnerIdOrWarn();
  if (!learnerId) {
    return;
  }

  this.processing = true;

  const request: InstallmentPlanRequest = {
    numberOfInstallments: this.selectedPlan,
    paymentMethod: this.selectedMethod,
    phoneNumber: this.phoneNumber,
    totalAmount: this.totalAmount,
    courseTitles: this.savedCourseTitles
  };

  this.cartService.createInstallmentPlan(learnerId, request).subscribe({
    next: (planResponse) => {
      console.log('Plan créé, planId:', planResponse.planId); // ← vérifier ici

      // ✅ generateBakchichINSTALLMENTCode — pas generateBakchichCode
      this.cartService.generateBakchichInstallmentCode(
        learnerId,
        this.phoneNumber,
        this.planPreview.totalWithFee,
        planResponse.planId  // ← planId doit être non-null
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
          alert('Erreur Bakchich : ' + (err.error || err.message));
        }
      });
    },
    error: (error) => {
      this.processing = false;
      alert('Erreur création plan : ' + (error.error || error.message));
    }
  });
}

  // ===== CRÉER LE PLAN (commun aux 3 méthodes) =====
  createInstallmentPlan(): void {
    const learnerId = this.getLearnerIdOrWarn();
    if (!learnerId) {
      return;
    }

    const request: InstallmentPlanRequest = {
      numberOfInstallments: this.selectedPlan,
      paymentMethod: this.selectedMethod,
      phoneNumber: this.phoneNumber,
      totalAmount: this.totalAmount,courseTitles: this.savedCourseTitles
    };

    this.cartService.createInstallmentPlan(learnerId, request).subscribe({
      next: (response) => {
        this.processing = false;
        clearInterval(this.timerInterval);

        if (this.selectedMethod === 'BAKCHICH') {
          // Pour Bakchich on reste sur l'écran du code
          return;
        }

        this.router.navigate(['/cart/installment-success'], {
          state: { plan: response }
        });
      },
      error: (error) => {
        this.processing = false;
        alert('Erreur création plan : ' + (error.error || error.message));
      }
    });
  }

  goBack(): void {
    this.router.navigate(['/cart/payment']);
  }
}