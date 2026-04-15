import { Component, OnInit } from '@angular/core';
import { TrainerService } from '../services/trainer.service';
import { TrainerHiring, TrainerStatus } from '../models/trainer.model';

@Component({
    selector: 'app-public-trainers',
    templateUrl: './public-trainers.component.html',
    styleUrl: './public-trainers.component.scss'
})
export class PublicTrainersComponent implements OnInit {
    trainers: TrainerHiring[] = [];
    isLoading = true;
    errorMessage = '';

    constructor(private trainerService: TrainerService) { }

    ngOnInit(): void {
        this.loadAcceptedTrainers();
    }

    loadAcceptedTrainers(): void {
        this.isLoading = true;
        this.trainerService.getAllTrainers(0, 100, TrainerStatus.ACCEPTED).subscribe({
            next: (response: any) => {
                this.trainers = response.content;
                this.isLoading = false;
            },
            error: (err: any) => {
                console.error('Error fetching trainers:', err);
                this.errorMessage = 'Failed to load trainers. Please try again later.';
                this.isLoading = false;
            }
        });
    }

    getPictureUrl(trainer: TrainerHiring): string {
        return this.trainerService.getPictureUrl(trainer.id!);
    }
}
