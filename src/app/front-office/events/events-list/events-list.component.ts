import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { Event } from '../models/event.model';
import { EventService } from '../services/event.service';

@Component({
    selector: 'app-events-list',
    templateUrl: './events-list.component.html',
    styleUrls: ['./events-list.component.scss']
})
export class EventsListComponent implements OnInit {
    events: Event[] = [];
    loading = true;

    constructor(
        private eventService: EventService,
        private router: Router
    ) { }

    ngOnInit(): void {
        this.loadEvents();
    }

    loadEvents(): void {
        this.loading = true;
        this.eventService.getEvents().subscribe({
            next: (events) => {
                this.events = events;
                this.loading = false;
            },
            error: (error) => {
                console.error('Error loading events:', error);
                this.loading = false;
            }
        });
    }

    get topEvents(): Event[] {
        return [...this.events]
            .filter(e => e.status !== 'CANCELLED' && e.status !== 'FINISHED')
            .sort((a, b) => (b.attendees || 0) - (a.attendees || 0))
            .slice(0, 3);
    }

    viewEvent(id: number | undefined): void {
        if (id) {
            this.router.navigate(['/events', id]);
        }
    }

    createEvent(): void {
        this.router.navigate(['/events/create']);
    }

    deleteEvent(event: MouseEvent, id: number | undefined): void {
        event.stopPropagation();
        if (id && confirm('Are you sure you want to delete this event?')) {
            this.eventService.deleteEvent(id).subscribe({
                next: () => {
                    this.loadEvents();
                },
                error: (error) => {
                    console.error('Error deleting event:', error);
                }
            });
        }
    }

    formatDate(date: Date): { month: string; day: string } {
        const eventDate = new Date(date);
        return {
            month: eventDate.toLocaleDateString('en-US', { month: 'short' }).toUpperCase(),
            day: eventDate.getDate().toString()
        };
    }

    formatFullDate(date: Date): string {
        const eventDate = new Date(date);
        return eventDate.toLocaleDateString('en-US', {
            weekday: 'long',
            year: 'numeric',
            month: 'long',
            day: 'numeric',
            hour: '2-digit',
            minute: '2-digit'
        });
    }
}
