import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface Holiday {
    date: string;
    localName: string;
    name: string;
    countryCode: string;
    fixed: boolean;
    global: boolean;
}

@Injectable({
    providedIn: 'root'
})
export class HolidayService {
    private apiUrl = 'https://date.nager.at/api/v3';

    constructor(private http: HttpClient) { }

    getHolidays(year: number): Observable<Holiday[]> {
        return this.http.get<Holiday[]>(`${this.apiUrl}/PublicHolidays/${year}/TN`);
    }
}
