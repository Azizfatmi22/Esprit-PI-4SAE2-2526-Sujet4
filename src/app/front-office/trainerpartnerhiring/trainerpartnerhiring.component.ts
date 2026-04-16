import { Component, Inject, OnInit, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { PartnerService } from '../services/partner.service';
import { TrainerService } from '../services/trainer.service';
import { JobService, Job } from '../services/job.service';
import { TrainerStatus, Technology, TrainerHiring } from '../models/trainer.model';
import { City, LegalForm, PartnershipType, PartnerHiring } from '../models/partner.model';
import { UserService } from '../services/user.service';

import { CommonModule } from '@angular/common';
import { HttpClientModule } from '@angular/common/http';
import { QuillModule } from 'ngx-quill';

@Component({
  selector: 'app-trainer-partner-hiring',
  templateUrl: './trainerpartnerhiring.component.html',
  styleUrls: ['./trainerpartnerhiring.component.scss'],
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    HttpClientModule,
    QuillModule
  ]
})
export class TrainerPartnerHiringComponent implements OnInit {
  partnerForm!: FormGroup;
  trainerForm!: FormGroup;

  isSubmitting = false;
  showForm = false;
  showTrainerForm = false;
  showJobCatalog = false;

  errorMessage = '';
  successMessage = '';
  showSuccessPopup = false;
  successPopupTitle = '';
  successPopupMessage = '';

  cities = Object.values(City);
  legalForms = Object.values(LegalForm);
  partnershipTypes = Object.values(PartnershipType);

  businessFile: File | null = null;
  profileFile: File | null = null;
  logoFile: File | null = null;
  logoPreview: string | null = null;

  // Trainer variables
  cvFile: File | null = null;
  pictureFile: File | null = null;
  picturePreview: string | null = null;
  acceptedPartners: PartnerHiring[] = [];
  availableJobs: Job[] = [];
  technologies = Object.values(Technology);

  // Analysis result variable
  analysisResult: any = null;

  // Tracking state
  currentUserEmail: string | null = null;
  applicationStates: Map<string, TrainerHiring | null> = new Map(); // JobId -> Application or null

  // Multi-step form logic
  currentStep = 1;
  currentTrainerStep = 1;

  private readonly MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
  private readonly ALLOWED_FILE_TYPE = 'application/pdf';
  private readonly ALLOWED_IMAGE_TYPES = ['image/png', 'image/jpeg', 'image/jpg'];

  constructor(
    private fb: FormBuilder,
    private partnerService: PartnerService,
    private trainerService: TrainerService,
    private jobService: JobService,
    private userService: UserService,
    @Inject(PLATFORM_ID) private platformId: Object
  ) { }

  ngOnInit(): void {
    this.initPartnerForm();
    this.initTrainerForm();
    this.loadAcceptedPartners();
    this.loadAvailableJobs();
    this.identifyUser();
  }

  identifyUser(): void {
    const user = this.userService.getUser();
    this.currentUserEmail = user?.email || null;
    if (this.currentUserEmail) {
      this.trainerForm.patchValue({ email: this.currentUserEmail });
      this.checkExistingApplications();
    }
  }

  checkExistingApplications(): void {
    if (!this.currentUserEmail) return;
    this.availableJobs.forEach(job => {
      this.trainerService.checkApplicationExists(this.currentUserEmail!, job.id!).subscribe({
        next: (app: TrainerHiring) => this.applicationStates.set(job.id!, app),
        error: () => this.applicationStates.set(job.id!, null)
      });
    });
  }

  hasAppliedTo(jobId: string): boolean {
    return !!this.applicationStates.get(jobId);
  }

  getApplicationFor(jobId: string): TrainerHiring | null {
    return this.applicationStates.get(jobId) || null;
  }

  loadAvailableJobs(): void {
    this.jobService.getAllJobs().subscribe({
      next: (jobs: Job[]) => {
        this.availableJobs = jobs;
        this.checkExistingApplications();
      },
      error: (err: any) => console.error('Failed to load jobs', err)
    });
  }


  // Step Navigation
  nextStep(): void {
    if (this.isStepValid()) {
      this.currentStep++;
      this.scrollToFormTop('partner-form-section');
    }
  }

  prevStep(): void {
    if (this.currentStep > 1) {
      this.currentStep--;
      this.scrollToFormTop('partner-form-section');
    }
  }

  nextTrainerStep(): void {
    if (this.isTrainerStepValid()) {
      this.currentTrainerStep++;
      this.scrollToFormTop('trainer-form-section');
    }
  }

  prevTrainerStep(): void {
    if (this.currentTrainerStep > 1) {
      this.currentTrainerStep--;
      this.scrollToFormTop('trainer-form-section');
    }
  }

  private scrollToFormTop(id: string): void {
    if (isPlatformBrowser(this.platformId)) {
      setTimeout(() => {
        document.getElementById(id)?.scrollIntoView({ behavior: 'smooth', block: 'start' });
      }, 100);
    }
  }

  isStepValid(): boolean {
    if (this.currentStep === 1) {
      return this.partnerForm.get('organizationName')!.valid &&
        this.partnerForm.get('legalForm')!.valid &&
        this.partnerForm.get('email')!.valid &&
        this.partnerForm.get('phone')!.valid;
    }
    if (this.currentStep === 2) {
      return this.partnerForm.get('website')!.valid &&
        this.partnerForm.get('city')!.valid &&
        this.partnerForm.get('address')!.valid &&
        this.partnerForm.get('partnershipType')!.valid;
    }
    return true; // Step 3 is validated by submit
  }

  isTrainerStepValid(): boolean {
    if (this.currentTrainerStep === 1) {
      return this.trainerForm.get('name')!.valid &&
        this.trainerForm.get('forename')!.valid &&
        this.trainerForm.get('email')!.valid &&
        this.trainerForm.get('phone')!.valid &&
        this.trainerForm.get('location')!.valid;
    }
    if (this.currentTrainerStep === 2) {
      return this.trainerForm.get('yearsOfExperience')!.valid &&
        this.trainerForm.get('technology')!.valid &&
        this.trainerForm.get('partnerId')!.valid &&
        this.trainerForm.get('motivationLetter')!.valid;
    }
    return true;
  }

  initPartnerForm(): void {
    this.partnerForm = this.fb.group({
      organizationName: ['', [Validators.required, Validators.minLength(3)]],
      legalForm: ['', Validators.required],
      email: ['', [Validators.required, Validators.email]],
      phone: ['', [Validators.required, Validators.pattern('^[0-9+ \\-()]{7,20}$')]],
      website: [''],
      city: ['', Validators.required],
      address: ['', [Validators.required, Validators.minLength(5)]],
      partnershipType: ['', Validators.required],
      businessRegistration: [null],
      companyProfile: [null],
      logo: [null]
    });
  }

  // Domain validator removed — trust engine handles quality assessment


  onFileChange(event: any, field: string): void {
    const file = event.target.files[0];
    this.errorMessage = '';

    if (file) {
      if (field === 'logo') {
        if (!this.ALLOWED_IMAGE_TYPES.includes(file.type)) {
          this.errorMessage = 'Only JPG and PNG images are allowed for the logo.';
          this.resetFileField(field);
          return;
        }
      } else if (file.type !== this.ALLOWED_FILE_TYPE) {
        this.errorMessage = 'Only PDF files are allowed.';
        this.resetFileField(field);
        return;
      }

      if (file.size > this.MAX_FILE_SIZE) {
        this.errorMessage = 'File size exceeds 5MB limit.';
        this.resetFileField(field);
        return;
      }

      if (field === 'businessRegistration') {
        this.businessFile = file;
      } else if (field === 'companyProfile') {
        this.profileFile = file;
      } else if (field === 'logo') {
        this.logoFile = file;
        const reader = new FileReader();
        reader.onload = () => this.logoPreview = reader.result as string;
        reader.readAsDataURL(file);
      }
      this.partnerForm.patchValue({ [field]: file });
      this.partnerForm.get(field)?.updateValueAndValidity();
    }
  }

  public resetFileField(field: string): void {
    if (field === 'businessRegistration') {
      this.businessFile = null;
    } else if (field === 'companyProfile') {
      this.profileFile = null;
    } else if (field === 'logo') {
      this.logoFile = null;
      this.logoPreview = null;
    }
    this.partnerForm.patchValue({ [field]: null });
    this.partnerForm.get(field)?.markAsTouched();
  }

  toggleForm(): void {
    this.showForm = !this.showForm;
    if (this.showForm && isPlatformBrowser(this.platformId)) {
      setTimeout(() => {
        document.getElementById('partner-form-section')?.scrollIntoView({ behavior: 'smooth' });
      }, 100);
    }
  }

  initTrainerForm(): void {
    this.trainerForm = this.fb.group({
      name: ['', Validators.required],
      forename: ['', Validators.required],
      email: ['', [Validators.required, Validators.email]],
      phone: ['', [Validators.required, Validators.pattern('^[0-9+ \\-()]{7,20}$')]],
      location: ['', Validators.required],
      partnerId: ['', Validators.required],
      jobId: ['', Validators.required],
      motivationLetter: ['', Validators.required],
      cv: [null, Validators.required],
      picture: [null, Validators.required],
      yearsOfExperience: ['', [Validators.required, Validators.min(0), Validators.max(50), Validators.pattern('^[0-9]*$')]],
      technology: ['', Validators.required]
    });
  }

  loadAcceptedPartners(): void {
    this.partnerService.getAllPartners(0, 100, 'ACCEPTED').subscribe({
      next: (response: any) => {
        this.acceptedPartners = response.content || [];
      },
      error: (err: any) => console.error('Failed to load partners', err)
    });
  }

  toggleTrainerForm(): void {
    if (!this.showTrainerForm && !this.showJobCatalog) {
      this.showJobCatalog = true;
      if (isPlatformBrowser(this.platformId)) {
        setTimeout(() => {
          document.getElementById('job-catalog-section')?.scrollIntoView({ behavior: 'smooth' });
        }, 100);
      }
    } else {
      this.showTrainerForm = false;
      this.showJobCatalog = false;
      this.currentTrainerStep = 1;
    }
  }

  selectJob(job: Job): void {
    const existingApp = this.getApplicationFor(job.id!);

    if (existingApp) {
      this.analysisResult = existingApp;
      this.showJobCatalog = false;
      this.showTrainerForm = true;
    } else {
      this.trainerForm.reset();
      this.analysisResult = null;
      this.trainerForm.patchValue({
        jobId: job.id,
        partnerId: job.partnerId,
        email: this.currentUserEmail,
        technology: job.technology
      });
      this.showJobCatalog = false;
      this.showTrainerForm = true;
      this.currentTrainerStep = 1;
    }

    // Scroll to form or dashboard
    if (isPlatformBrowser(this.platformId)) {
      setTimeout(() => {
        const element = document.getElementById('trainer-form-section');
        if (element) {
          element.scrollIntoView({ behavior: 'smooth' });
        }
      }, 100);
    }
  }

  onTrainerFileChange(event: any): void {
    const file = event.target.files[0];
    this.errorMessage = '';
    if (file) {
      if (file.type !== this.ALLOWED_FILE_TYPE) {
        this.errorMessage = 'Only PDF files are allowed for CV.';
        this.cvFile = null;
        return;
      }
      if (file.size > this.MAX_FILE_SIZE) {
        this.errorMessage = 'File size exceeds 5MB limit.';
        this.cvFile = null;
        return;
      }
      this.cvFile = file;
      this.trainerForm.patchValue({ cv: file });
    }
  }

  onPictureChange(event: any): void {
    const file = event.target.files[0];
    this.errorMessage = '';
    if (file) {
      if (!this.ALLOWED_IMAGE_TYPES.includes(file.type)) {
        this.errorMessage = 'Only JPG and PNG images are allowed.';
        this.pictureFile = null;
        return;
      }
      if (file.size > this.MAX_FILE_SIZE) {
        this.errorMessage = 'File size exceeds 5MB limit.';
        this.pictureFile = null;
        return;
      }
      this.pictureFile = file;
      this.trainerForm.patchValue({ picture: file });
      const reader = new FileReader();
      reader.onload = () => this.picturePreview = reader.result as string;
      reader.readAsDataURL(file);
    }
  }

  resetPicture(): void {
    this.pictureFile = null;
    this.picturePreview = null;
    this.trainerForm.patchValue({ picture: null });
  }

  onTrainerSubmit(): void {
    if (this.trainerForm.valid && this.cvFile && this.pictureFile) {
      this.isSubmitting = true;
      this.errorMessage = '';
      this.successMessage = '';
      this.analysisResult = null;

      const trainerData = { ...this.trainerForm.value };
      delete trainerData.cv;
      delete trainerData.picture;

      this.trainerService.createTrainer(trainerData, this.cvFile, this.pictureFile || undefined)
        .subscribe({
          next: (response: any) => {
            console.log('Trainer application submitted with analysis', response);
            this.analysisResult = response;

            // Update tracking state immediately for this job
            if (response.jobId) {
              this.applicationStates.set(response.jobId, response);
            } else if (trainerData.jobId) {
              this.applicationStates.set(trainerData.jobId, response);
            }

            this.successMessage = 'Your application has been analyzed and submitted!';
            this.isSubmitting = false;

            this.successPopupTitle = 'Application Analyzed!';
            this.successPopupMessage = `Intelligent verification complete. Skill Match: ${response.skillSyncScore}%. Probability of success: ${response.acceptanceProbability}%.`;
            this.showSuccessPopup = true;
          },
          error: (err: any) => {
            console.error('Error submitting trainer application', err);
            this.errorMessage = err.error?.message || 'An error occurred. Please try again.';
            this.isSubmitting = false;
          }
        });
    } else {
      this.markFormGroupTouched(this.trainerForm);
    }
  }

  onSubmit(): void {
    if (this.partnerForm.valid) {
      // Guard: ensure required files are selected
      if (!this.businessFile || !this.profileFile) {
        this.errorMessage = 'Please upload both the Business Registration and Company Profile PDF documents before submitting.';
        return;
      }

      this.isSubmitting = true;
      this.errorMessage = '';
      this.successMessage = '';

      const partnerData = { ...this.partnerForm.value };
      delete partnerData.businessRegistration;
      delete partnerData.companyProfile;
      delete partnerData.logo;

      this.partnerService.createPartner(partnerData, this.businessFile, this.profileFile, this.logoFile || undefined)
        .subscribe({
          next: (response: any) => {
            console.log('Partner created successfully', response);
            this.successMessage = 'Your application has been submitted successfully! We will contact you soon.';
            this.isSubmitting = false;
            this.showSuccessPopup = true;
            this.successPopupTitle = 'Application Received!';
            this.successPopupMessage = 'Thank you for your interest in joining Formini. Your application has been submitted successfully. Our team will review your details and get back to you shortly.';
            this.partnerForm.reset();
            this.businessFile = null;
            this.profileFile = null;
            this.logoFile = null;
            this.logoPreview = null;
            // Scroll to the top of the form or success message
            if (isPlatformBrowser(this.platformId)) {
              document.getElementById('partner-form-section')?.scrollIntoView({ behavior: 'smooth' });
            }
            setTimeout(() => this.showForm = false, 5000);
          },
          error: (err: any) => {
            console.error('Error creating partner', err);
            this.errorMessage = err.error?.message || 'An error occurred while submitting your application. Please try again.';
            this.isSubmitting = false;
            if (isPlatformBrowser(this.platformId)) {
              document.getElementById('partner-form-section')?.scrollIntoView({ behavior: 'smooth' });
            }
          }
        });
    } else {
      this.markFormGroupTouched(this.partnerForm);
    }
  }

  closePopup(): void {
    this.showSuccessPopup = false;
    this.showForm = false;
    this.showTrainerForm = false;
  }

  private markFormGroupTouched(formGroup: FormGroup) {
    Object.values(formGroup.controls).forEach(control => {
      control.markAsTouched();
      if ((control as any).controls) {
        this.markFormGroupTouched(control as FormGroup);
      }
    });
  }
}
