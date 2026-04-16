import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface ImageUploadResponse {
    url: string;
    fileName: string;
    contentType: string;
    size: number;
    error?: string;
}

import { environment } from '../../environment/envirement';

@Injectable({
    providedIn: 'root'
})
export class ImageUploadService {
    private apiUrl = `${environment.apiGatewayUrl}/api/images`;

    constructor(private http: HttpClient) { }

    uploadImage(file: File): Observable<ImageUploadResponse> {
        const formData = new FormData();
        formData.append('file', file);
        return this.http.post<ImageUploadResponse>(`${this.apiUrl}/upload`, formData);
    }
}
