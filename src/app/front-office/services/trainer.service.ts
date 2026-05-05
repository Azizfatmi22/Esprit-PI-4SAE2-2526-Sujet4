import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { TrainerHiring, TrainerStatus } from '../models/trainer.model';

@Injectable({
  providedIn: 'root'
})
export class TrainerService {
  private apiUrl = 'http://localhost:8085/api/trainers';

  constructor(private http: HttpClient) { }

  createTrainer(trainer: TrainerHiring, cvFile: File, pictureFile?: File): Observable<TrainerHiring> {
    const formData = new FormData();
    formData.append('trainer', JSON.stringify(trainer));
    formData.append('cv', cvFile);
    if (pictureFile) {
      formData.append('picture', pictureFile);
    }
    return this.http.post<TrainerHiring>(this.apiUrl, formData);
  }

  getAllTrainers(page: number = 0, size: number = 10, status?: string, keyword?: string, technology?: string): Observable<any> {
    let url = `${this.apiUrl}?page=${page}&size=${size}`;
    if (status) {
      url += `&status=${status}`;
    }
    if (keyword) {
      url += `&keyword=${encodeURIComponent(keyword)}`;
    }
    if (technology) {
      url += `&technology=${technology}`;
    }
    return this.http.get<any>(url);
  }

  getTrainerById(id: string): Observable<TrainerHiring> {
    return this.http.get<TrainerHiring>(`${this.apiUrl}/${id}`);
  }

  updateStatus(id: string, status: string): Observable<TrainerHiring> {
    return this.http.patch<TrainerHiring>(`${this.apiUrl}/${id}/status?status=${status}`, {});
  }

  deleteTrainer(id: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  checkApplicationExists(email: string, jobId: string): Observable<TrainerHiring> {
    return this.http.get<TrainerHiring>(`${this.apiUrl}/check?email=${email}&jobId=${jobId}`);
  }

  getCVUrl(id: string): string {
    return `${this.apiUrl}/${id}/documents/CV`;
  }

  getPictureUrl(id: string): string {
    return `${this.apiUrl}/${id}/documents/PICTURE`;
  }

  getTopCandidate(jobId: string): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/jobs/${jobId}/top-candidate`);
  }

  downloadContract(id: string): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/${id}/contract/download`, { responseType: 'blob' });
  }
}

