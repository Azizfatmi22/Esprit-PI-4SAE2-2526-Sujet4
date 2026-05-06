import { ComponentFixture, TestBed } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { Router } from '@angular/router';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { of, throwError } from 'rxjs';

import { EventsListComponent } from './events-list.component';
import { EventService } from '../services/event.service';
import { Event } from '../models/event.model';

describe('EventsListComponent', () => {
  let component: EventsListComponent;
  let fixture: ComponentFixture<EventsListComponent>;
  let eventServiceSpy: jasmine.SpyObj<EventService>;

  const mockEvents: Event[] = [
    {
      id: 1,
      name: 'Angular Workshop',
      description: 'Learn Angular',
      details: 'Full day workshop',
      location: 'Tunis',
      latitude: 36.8,
      longitude: 10.1,
      date: new Date('2025-06-01'),
      status: 'UPCOMING',
      attendees: 30,
      maxParticipants: 50,
      currentParticipants: 30,
      category: 'TECHNOLOGY',
      createdAt: new Date('2025-01-01')
    },
    {
      id: 2,
      name: 'DevOps Day',
      description: 'CI/CD pipelines',
      details: 'Hands-on DevOps',
      location: 'Sfax',
      latitude: 34.7,
      longitude: 10.7,
      date: new Date('2025-07-01'),
      status: 'UPCOMING',
      attendees: 10,
      maxParticipants: 40,
      currentParticipants: 10,
      category: 'TECHNOLOGY',
      createdAt: new Date('2025-01-01')
    },
    {
      id: 3,
      name: 'Cancelled Event',
      description: 'Should be hidden',
      details: '',
      location: 'Sousse',
      latitude: 35.8,
      longitude: 10.6,
      date: new Date('2025-05-01'),
      status: 'CANCELLED',
      attendees: 0,
      category: 'OTHER',
      createdAt: new Date()
    }
  ];

  beforeEach(async () => {
    const spy = jasmine.createSpyObj('EventService', ['getEvents', 'deleteEvent']);

    await TestBed.configureTestingModule({
      declarations: [EventsListComponent],
      imports: [RouterTestingModule],
      providers: [{ provide: EventService, useValue: spy }],
      schemas: [NO_ERRORS_SCHEMA]
    }).compileComponents();

    eventServiceSpy = TestBed.inject(EventService) as jasmine.SpyObj<EventService>;
    eventServiceSpy.getEvents.and.returnValue(of(mockEvents));

    fixture = TestBed.createComponent(EventsListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should load events on init', () => {
    expect(eventServiceSpy.getEvents).toHaveBeenCalled();
    expect(component.events.length).toBe(3);
    expect(component.loading).toBeFalse();
  });

  it('topEvents should exclude CANCELLED and FINISHED events', () => {
    const top = component.topEvents;
    expect(top.every(e => e.status !== 'CANCELLED' && e.status !== 'FINISHED')).toBeTrue();
  });

  it('topEvents should return at most 3 events sorted by attendees descending', () => {
    const top = component.topEvents;
    expect(top.length).toBeLessThanOrEqual(3);
    if (top.length > 1) {
      expect(top[0].attendees).toBeGreaterThanOrEqual(top[1].attendees);
    }
  });

  it('should set loading=false on error', () => {
    // Re-stub the spy to return an error for this specific test
    eventServiceSpy.getEvents.and.returnValue(throwError(() => new Error('Network error')));
    component.loading = true;
    component.events = [];
    component.loadEvents();
    expect(component.loading).toBeFalse();
    expect(component.events).toEqual([]);
  });

  it('formatDate() should return month and day strings', () => {
    const result = component.formatDate(new Date('2025-06-01'));
    expect(result.month).toBeTruthy();
    expect(result.day).toBeTruthy();
    expect(typeof result.month).toBe('string');
    expect(typeof result.day).toBe('string');
  });

  it('formatFullDate() should return a full date string', () => {
    const result = component.formatFullDate(new Date('2025-06-01'));
    expect(result).toContain('2025');
    expect(typeof result).toBe('string');
  });

  it('deleteEvent() should reload events after successful deletion', () => {
    eventServiceSpy.deleteEvent.and.returnValue(of(undefined));
    spyOn(window, 'confirm').and.returnValue(true);
    const mouseEvent = new MouseEvent('click');
    spyOn(mouseEvent, 'stopPropagation');

    component.deleteEvent(mouseEvent, 1);

    expect(eventServiceSpy.deleteEvent).toHaveBeenCalledWith(1);
    expect(eventServiceSpy.getEvents).toHaveBeenCalledTimes(2); // once on init, once after delete
  });

  it('deleteEvent() should NOT call service when confirm is rejected', () => {
    spyOn(window, 'confirm').and.returnValue(false);
    const mouseEvent = new MouseEvent('click');
    component.deleteEvent(mouseEvent, 1);
    expect(eventServiceSpy.deleteEvent).not.toHaveBeenCalled();
  });

  it('viewEvent() should navigate to event details', () => {
    const router = TestBed.inject(Router);
    spyOn(router, 'navigate');
    component.viewEvent(1);
    expect(router.navigate).toHaveBeenCalledWith(['/events', 1]);
  });

  it('createEvent() should navigate to create page', () => {
    const router = TestBed.inject(Router);
    spyOn(router, 'navigate');
    component.createEvent();
    expect(router.navigate).toHaveBeenCalledWith(['/events/create']);
  });
});
