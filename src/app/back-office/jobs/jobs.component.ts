import { Component, Inject, OnInit, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser, CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { JobService, Job } from '../../front-office/services/job.service';
import { PartnerService } from '../../front-office/services/partner.service';
import { PartnerHiring } from '../../front-office/models/partner.model';
import { Technology } from '../../front-office/models/trainer.model';
import { City } from '../../front-office/models/partner.model';

@Component({
    selector: 'app-jobs',
    standalone: true,
    imports: [CommonModule, ReactiveFormsModule],
    templateUrl: './jobs.component.html',
    styleUrl: './jobs.component.scss'
})
export class JobsComponent implements OnInit {
    jobs: Job[] = [];
    acceptedPartners: PartnerHiring[] = [];
    technologies = Object.values(Technology);
    cities = Object.values(City);

    isLoading = true;
    isSubmitting = false;
    showCreateForm = false;
    errorMessage = '';
    successMessage = '';
    selectedJob: Job | null = null;

    jobForm!: FormGroup;

    constructor(
        private jobService: JobService,
        private partnerService: PartnerService,
        private fb: FormBuilder,
        @Inject(PLATFORM_ID) private platformId: Object
    ) { }

    ngOnInit(): void {
        this.initForm();
        this.loadJobs();
        this.loadPartners();
    }

    initForm(): void {
        this.jobForm = this.fb.group({
            partnerId: ['', Validators.required],
            title: ['', [Validators.required, Validators.minLength(5)]],
            description: ['', [Validators.required, Validators.minLength(20)]],
            technology: ['', Validators.required],
            location: ['', Validators.required],
            minExperience: [0, [Validators.required, Validators.min(0)]],
            maxExperience: [0],
            salaryRange: ['']
        });
    }

    loadJobs(): void {
        this.isLoading = true;
        this.jobService.getAllJobs().subscribe({
            next: (jobs) => {
                this.jobs = jobs;
                this.isLoading = false;
            },
            error: (err) => {
                this.errorMessage = 'Failed to load jobs.';
                this.isLoading = false;
                console.error(err);
            }
        });
    }

    loadPartners(): void {
        this.partnerService.getAllPartners(0, 50, 'ACCEPTED').subscribe({
            next: (res) => {
                this.acceptedPartners = res.content || [];
            },
            error: (err) => console.error('Failed to load partners', err)
        });
    }

    toggleCreateForm(): void {
        this.showCreateForm = !this.showCreateForm;
        if (!this.showCreateForm) {
            this.jobForm.reset();
        }
    }

    onJobSubmit(): void {
        if (this.jobForm.valid) {
            this.isSubmitting = true;
            this.errorMessage = '';
            const partnerId = this.jobForm.value.partnerId;

            this.jobService.createJob(partnerId, this.jobForm.value).subscribe({
                next: (job) => {
                    this.jobs.unshift(job);
                    this.successMessage = 'Job posted successfully!';
                    this.isSubmitting = false;
                    this.showCreateForm = false;
                    this.jobForm.reset();
                    setTimeout(() => this.successMessage = '', 5000);
                },
                error: (err) => {
                    this.errorMessage = 'Failed to create job.';
                    this.isSubmitting = false;
                    console.error(err);
                }
            });
        }
    }

    selectJob(job: Job): void {
        this.selectedJob = job;
    }

    closeDetails(): void {
        this.selectedJob = null;
    }

    deleteJob(id: string): void {
        if (isPlatformBrowser(this.platformId)) {
            if (confirm('Are you sure you want to delete this job posting? This will also affect applications linked to it.')) {
                this.jobService.deleteJob(id).subscribe({
                    next: () => {
                        this.jobs = this.jobs.filter(j => j.id !== id);
                        if (this.selectedJob?.id === id) {
                            this.closeDetails();
                        }
                    },
                    error: (err) => {
                        console.error('Error deleting job:', err);
                        alert('Failed to delete job. Please try again.');
                    }
                });
            }
        }
    }
}
