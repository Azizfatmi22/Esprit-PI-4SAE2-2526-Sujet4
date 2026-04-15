import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TrainerService } from '../../front-office/services/trainer.service';

@Component({
    selector: 'app-hr-analysis',
    standalone: true,
    imports: [CommonModule],
    templateUrl: './hr-analysis.component.html',
    styleUrl: './hr-analysis.component.scss'
})
export class HrAnalysisComponent implements OnInit {
    today = new Date();

    // Hiring Data
    totalHiringTrainers = 0;
    candidateDistribution: any[] = [];
    expertiseRadar: any[] = [];
    chartData: any[] = [];
    maxChartValue = 100;
    topTechInsights: any[] = [];

    constructor(private trainerService: TrainerService) { }

    ngOnInit(): void {
        this.loadHiringStats();
    }

    loadHiringStats(): void {
        this.trainerService.getAllTrainers(0, 500).subscribe({
            next: (res) => {
                const trainers = res.content || [];
                this.generateAdvancedHiringStats(trainers);
            }
        });
    }

    generateAdvancedHiringStats(trainers: any[]): void {
        this.totalHiringTrainers = trainers.length;

        const accepted = trainers.filter(t => t.status === 'ACCEPTED').length;
        const pending = trainers.filter(t => !t.status || t.status === 'PENDING').length;
        const rejected = trainers.filter(t => t.status === 'REJECTED').length;

        this.candidateDistribution = [
            { label: 'Accepted', value: accepted, color: '#10b981' },
            { label: 'Pending', value: pending, color: '#6366f1' },
            { label: 'Rejected', value: rejected, color: '#ef4444' }
        ];

        // Mock radar data based on averages
        const avgSkill = trainers.length ? trainers.reduce((acc, t) => acc + (t.skillSyncScore || 0), 0) / trainers.length : 0;
        const avgTone = trainers.length ? trainers.reduce((acc, t) => acc + (t.toneClarityScore || 0), 0) / trainers.length : 0;
        const avgStability = 75; // Baseline
        const avgGrowth = 82; // Baseline

        this.expertiseRadar = [
            { label: 'Skill Match', value: Math.round(avgSkill) },
            { label: 'Professionalism', value: Math.round(avgTone) },
            { label: 'Experience Alignment', value: Math.round(avgStability) },
            { label: 'Stability Index', value: Math.round(avgGrowth) }
        ];

        // Trends logic (Mocked based on trainers count per month if possible, else mock)
        this.chartData = [
            { label: 'Jan', value: 12 },
            { label: 'Feb', value: 18 },
            { label: 'Mar', value: this.totalHiringTrainers },
            { label: 'Apr', value: 25 },
            { label: 'May', value: 32 }
        ];
        this.maxChartValue = Math.max(...this.chartData.map(d => d.value), 40);

        // Tech insights
        const techCounts: any = {};
        trainers.forEach(t => {
            if (t.technology) techCounts[t.technology] = (techCounts[t.technology] || 0) + 1;
        });

        this.topTechInsights = Object.keys(techCounts).map(tech => ({
            tech: tech,
            count: techCounts[tech],
            trend: '+12%'
        })).sort((a, b) => b.count - a.count).slice(0, 4);
    }

    getDonutOffset(index: number): string {
        let previousSum = 0;
        for (let i = 0; i < index; i++) {
            previousSum += (this.candidateDistribution[i].value / (this.totalHiringTrainers || 1));
        }
        return (-(previousSum * 251)).toString();
    }
}
