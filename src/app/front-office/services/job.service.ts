import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface Job {
    id?: string;
    title: string;
    description: string;
    technology: string;
    location: string;
    minExperience?: number;
    maxExperience?: number;
    salaryRange?: string;
    partnerId: string;
    partnerName?: string;
    createdAt?: string;
}

@Injectable({
    providedIn: 'root'
})
export class JobService {
    private apiUrl = 'http://localhost:8085/api/jobs';

    constructor(private http: HttpClient) { }

    createJob(partnerId: string, job: Job): Observable<Job> {
        return this.http.post<Job>(`${this.apiUrl}/${partnerId}`, job);
    }

    getAllJobs(): Observable<Job[]> {
        return this.http.get<Job[]>(this.apiUrl);
    }

    getJobById(id: string): Observable<Job> {
        return this.http.get<Job>(`${this.apiUrl}/${id}`);
    }

    getJobsByPartner(partnerId: string): Observable<Job[]> {
        return this.http.get<Job[]>(`${this.apiUrl}/partner/${partnerId}`);
    }

    deleteJob(id: string): Observable<void> {
        return this.http.delete<void>(`${this.apiUrl}/${id}`);
    }

    getJobTemplate(technology: string): Observable<Job> {
        return this.http.get<Job>(`${this.apiUrl}/template?technology=${technology}`);
    }

    getMarketSync(technology: string): Observable<{ averageExperience: number }> {
        return this.http.get<{ averageExperience: number }>(`${this.apiUrl}/market-sync?technology=${technology}`);
    }
}
