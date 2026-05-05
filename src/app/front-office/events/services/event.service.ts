import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Event } from '../models/event.model';

import { environment } from '../../environment/envirement';

@Injectable({
    providedIn: 'root'
})
export class EventService {
    // Temporarily call the forum-service directly until the Gateway route is configured
    private apiUrl = `${environment.apiGatewayUrl}/api/events`;

    constructor(private http: HttpClient) { }

    /**
     * Get all events from the backend
     */
    getEvents(): Observable<Event[]> {
        return this.http.get<Event[]>(this.apiUrl);
    }

    /**
     * Get a single event by ID
     */
    getEventById(id: number): Observable<Event> {
        return this.http.get<Event>(`${this.apiUrl}/${id}`);
    }

    /**
     * Create a new event
     * Handles Base64 image data in imageUrl field
     */
    createEvent(event: Event): Observable<Event> {
        return this.http.post<Event>(this.apiUrl, event);
    }

    /**
     * Update an existing event
     * Handles Base64 image data in imageUrl field
     */
    updateEvent(id: number, event: Event): Observable<Event> {
        return this.http.put<Event>(`${this.apiUrl}/${id}`, event);
    }

    /**
     * Delete an event by ID
     */
    deleteEvent(id: number): Observable<void> {
        return this.http.delete<void>(`${this.apiUrl}/${id}`);
    }
}
