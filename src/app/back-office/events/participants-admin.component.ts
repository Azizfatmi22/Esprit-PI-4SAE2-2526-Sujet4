import { Component, Inject, PLATFORM_ID } from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ParticipantsAdminService, AdminEvent, AdminParticipant } from './participants-admin.service';
import { RibbonsComponent } from '../../front-office/events/ribbons/ribbons.component';
import { EventAnalytics } from '../../front-office/events/models/event-analytics.model';

@Component({
  selector: 'app-participants-admin',
  standalone: true,
  imports: [CommonModule, FormsModule, RibbonsComponent],
  templateUrl: './participants-admin.component.html',
  styleUrls: ['./participants-admin.component.scss']
})
export class ParticipantsAdminComponent {
  events: AdminEvent[] = [];
  selectedEventId: number | null = null;
  participants: AdminParticipant[] = [];
  loading = false;
  statsTotal = 0;
  name = '';
  email = '';
  message: string | null = null;
  error: string | null = null;

  analytics: EventAnalytics | null = null;

  // Event Edit State
  editingEvent: Partial<AdminEvent> | null = null;

  constructor(
    private svc: ParticipantsAdminService,
    @Inject(PLATFORM_ID) private platformId: Object
  ) {
    this.loadEvents();
    this.loadAnalytics();
  }

  loadAnalytics() {
    this.svc.getAnalytics().subscribe({
      next: (data) => this.analytics = data,
      error: (err) => console.error('Failed to load analytics', err)
    });
  }

  loadEvents() {
    this.svc.getEvents().subscribe({
      next: (ev) => {
        this.events = ev;
      },
      error: () => { }
    });
  }

  // --- Event Management ---

  startEditEvent(event: AdminEvent) {
    this.editingEvent = { ...event };
    if (isPlatformBrowser(this.platformId)) {
      document.body.style.overflow = 'hidden';
    }
  }

  cancelEdit() {
    this.editingEvent = null;
    if (isPlatformBrowser(this.platformId)) {
      document.body.style.overflow = '';
    }
  }

  saveEvent() {
    if (!this.editingEvent || !this.editingEvent.id) return;
    this.svc.updateEvent(this.editingEvent.id, this.editingEvent).subscribe({
      next: () => {
        this.loadEvents();
        this.cancelEdit();
      },
      error: (err) => console.error('Failed to save event', err)
    });
  }

  deleteEvent(id: number) {
    if (!confirm('Are you sure you want to delete this event? This will also remove all its participants.')) return;
    this.svc.deleteEvent(id).subscribe({
      next: () => {
        this.loadEvents();
        if (this.selectedEventId === id) this.closeEvent();
      },
      error: (err) => console.error('Failed to delete event', err)
    });
  }

  // --- Participants Management ---

  openEvent(eventId: number) {
    this.selectedEventId = eventId;
    this.loadParticipants();
  }

  closeEvent() {
    this.selectedEventId = null;
    this.participants = [];
    this.statsTotal = 0;
    this.message = null;
    this.error = null;
  }

  loadParticipants() {
    if (!this.selectedEventId) return;
    this.loading = true;
    this.svc.getParticipants(this.selectedEventId).subscribe({
      next: (list) => {
        this.participants = list;
        this.statsTotal = list.length;
        this.loading = false;
        this.error = null;
      },
      error: (e) => {
        console.error('Failed to load participants', e);
        this.error = 'Failed to load participants. Please try again.';
        this.loading = false;
      }
    });
  }

  addParticipant() {
    if (!this.selectedEventId || !this.name || !this.email) return;
    this.message = null;
    this.error = null;
    this.svc.addParticipant(this.selectedEventId, { name: this.name, email: this.email }).subscribe({
      next: () => {
        this.message = 'Confirmation email sent.';
        this.name = '';
        this.email = '';
      },
      error: (e) => {
        this.error = e?.error?.message || 'Failed to add participant.';
      }
    });
  }

  deleteParticipant(p: AdminParticipant) {
    if (!this.selectedEventId) return;
    this.svc.deleteParticipant(this.selectedEventId, p.id).subscribe({
      next: () => {
        this.participants = this.participants.filter(x => x.id !== p.id);
        this.statsTotal = this.participants.length;
      },
      error: () => { }
    });
  }
}
