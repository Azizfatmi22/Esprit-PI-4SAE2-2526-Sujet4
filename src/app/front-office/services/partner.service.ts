import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { PartnerHiring } from '../models/partner.model';

@Injectable({
    providedIn: 'root'
})
export class PartnerService {
    private apiUrl = 'http://localhost:8085/api/partners';

    constructor(private http: HttpClient) { }

    createPartner(partner: PartnerHiring, businessRegistration: File, companyProfile: File, logo?: File): Observable<PartnerHiring> {
        const formData = new FormData();

        // Add the partner data as a JSON string
        formData.append('partner', JSON.stringify(partner));

        // Add the files
        formData.append('businessRegistration', businessRegistration);
        formData.append('companyProfile', companyProfile);
        if (logo) {
            formData.append('logo', logo);
        }

        return this.http.post<PartnerHiring>(this.apiUrl, formData);
    }

    updateStatus(id: string, status: string): Observable<PartnerHiring> {
        return this.http.patch<PartnerHiring>(`${this.apiUrl}/${id}/status?status=${status}`, {});
    }

    getAllPartners(page: number = 0, size: number = 10, status?: string): Observable<any> {
        let url = `${this.apiUrl}?page=${page}&size=${size}`;
        if (status) {
            url += `&status=${status}`;
        }
        return this.http.get<any>(url);
    }

    getPartnerById(id: string): Observable<PartnerHiring> {
        return this.http.get<PartnerHiring>(`${this.apiUrl}/${id}`);
    }

    deletePartner(id: string): Observable<void> {
        return this.http.delete<void>(`${this.apiUrl}/${id}`);
    }

    getDocumentUrl(id: string, type: string): string {
        return `${this.apiUrl}/${id}/documents/${type}`;
    }
}
