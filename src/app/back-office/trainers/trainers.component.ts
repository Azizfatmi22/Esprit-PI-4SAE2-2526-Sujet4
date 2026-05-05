import { Component, Inject, OnInit, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser, CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { TrainerService } from '../../front-office/services/trainer.service';
import { TrainerHiring, TrainerStatus, Technology } from '../../front-office/models/trainer.model';

@Component({
    selector: 'app-trainers',
    standalone: true,
    imports: [CommonModule, FormsModule],
    templateUrl: './trainers.component.html',
    styleUrl: './trainers.component.scss'
})
export class TrainersComponent implements OnInit {
    trainers: TrainerHiring[] = [];
    isLoading = true;
    errorMessage = '';
    selectedTrainer: TrainerHiring | null = null;

    // Filter properties
    filterKeyword = '';
    selectedTech = '';
    technologies = Object.values(Technology);

    constructor(
        private trainerService: TrainerService,
        @Inject(PLATFORM_ID) private platformId: Object
    ) { }

    ngOnInit(): void {
        this.loadTrainers();
    }

    loadTrainers(): void {
        this.isLoading = true;
        this.trainerService.getAllTrainers(0, 50, undefined, this.filterKeyword, this.selectedTech).subscribe({
            next: (response: any) => {
                this.trainers = response.content;
                this.isLoading = false;
            },
            error: (err) => {
                this.errorMessage = 'Failed to load trainers.';
                this.isLoading = false;
                console.error(err);
            }
        });
    }

    onFilterChange(): void {
        this.loadTrainers();
    }

    clearFilters(): void {
        this.filterKeyword = '';
        this.selectedTech = '';
        this.loadTrainers();
    }

    updateStatus(id: string, status: string): void {
        const newStatus = status as TrainerStatus;

        // Optimistic update
        const previousTrainers = [...this.trainers];
        const trainerIndex = this.trainers.findIndex(t => t.id === id);
        if (trainerIndex !== -1) {
            this.trainers[trainerIndex] = { ...this.trainers[trainerIndex], status: newStatus };
        }
        if (this.selectedTrainer && this.selectedTrainer.id === id) {
            this.selectedTrainer = { ...this.selectedTrainer, status: newStatus };
        }

        this.trainerService.updateStatus(id, status).subscribe({
            next: (updatedTrainer) => {
                const confirmedStatus = (updatedTrainer && updatedTrainer.status)
                    ? updatedTrainer.status
                    : newStatus;

                if (trainerIndex !== -1) {
                    this.trainers[trainerIndex] = { ...this.trainers[trainerIndex], status: confirmedStatus };
                    this.trainers = [...this.trainers];
                }
                if (this.selectedTrainer && this.selectedTrainer.id === id) {
                    this.selectedTrainer = { ...this.selectedTrainer, status: confirmedStatus };
                }

                if (isPlatformBrowser(this.platformId)) {
                    alert(`Trainer status successfully updated to ${confirmedStatus}!`);
                }
            },
            error: (error) => {
                console.error('Error updating trainer status:', error);
                this.trainers = previousTrainers;
                if (this.selectedTrainer && this.selectedTrainer.id === id) {
                    if (isPlatformBrowser(this.platformId)) {
                        alert('Failed to update status. Reverting changes.');
                    }
                    this.loadTrainers();
                }
            }
        });
    }

    approveTrainer(id: string): void {
        this.updateStatus(id, TrainerStatus.ACCEPTED);
    }

    rejectTrainer(id: string): void {
        if (isPlatformBrowser(this.platformId)) {
            if (confirm('Are you sure you want to reject this application?')) {
                this.updateStatus(id, TrainerStatus.REJECTED);
            }
        }
    }

    getCVUrl(trainer: TrainerHiring): string {
        return this.trainerService.getCVUrl(trainer.id!);
    }

    viewCV(trainerId: string): void {
        if (isPlatformBrowser(this.platformId)) {
            window.open(this.trainerService.getCVUrl(trainerId), '_blank');
        }
    }

    downloadContract(trainer: TrainerHiring): void {
        if (isPlatformBrowser(this.platformId) && trainer.id) {
            this.trainerService.downloadContract(trainer.id).subscribe({
                next: (blob) => {
                    const downloadUrl = window.URL.createObjectURL(blob);
                    const link = document.createElement('a'); // Create an anchor element
                    link.setAttribute('href', downloadUrl);
                    link.setAttribute('download', `Contract_${trainer.name}.pdf`);
                    document.body.appendChild(link);
                    link.click();
                    link.remove();
                    window.URL.revokeObjectURL(downloadUrl);
                },
                error: (err) => {
                    console.error('Failed to download contract', err);
                    alert('Error generating contract. Please ensure the backend is running.');
                }
            });
        }
    }


    selectTrainer(trainer: TrainerHiring): void {
        this.selectedTrainer = trainer;
    }

    closeDetails(): void {
        this.selectedTrainer = null;
    }

    deleteTrainer(id: string): void {
        if (isPlatformBrowser(this.platformId)) {
            if (confirm('Are you sure you want to delete this application?')) {
                this.trainerService.deleteTrainer(id).subscribe({
                    next: () => {
                        this.trainers = this.trainers.filter(t => t.id !== id);
                        if (this.selectedTrainer && this.selectedTrainer.id === id) {
                            this.selectedTrainer = null;
                        }
                    },
                    error: (err) => {
                        console.error('Error deleting trainer:', err);
                        alert('Failed to delete application. Please try again.');
                    }
                });
            }
        }
    }

    getPictureUrl(trainer: TrainerHiring): string {
        return this.trainerService.getPictureUrl(trainer.id!);
    }

    getStatusClass(status: string | undefined): string {
        if (!status) return 'pending';
        return status.toLowerCase();
    }

    // Stats calculations
    get totalTrainers(): number {
        return this.trainers.length;
    }

    get acceptedCount(): number {
        return this.trainers.filter(t => t.status === TrainerStatus.ACCEPTED).length;
    }

    get pendingCount(): number {
        return this.trainers.filter(t => !t.status || t.status === TrainerStatus.PENDING).length;
    }

    get rejectedCount(): number {
        return this.trainers.filter(t => t.status === TrainerStatus.REJECTED).length;
    }

    getStatsPercentage(count: number): number {
        if (this.totalTrainers === 0) return 0;
        return Math.round((count / this.totalTrainers) * 100);
    }
}
