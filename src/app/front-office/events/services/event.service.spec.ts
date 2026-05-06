import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { EventService } from './event.service';
import { Event } from '../models/event.model';
import { environment } from '../../environment/envirement';

describe('EventService', () => {
  let service: EventService;
  let httpMock: HttpTestingController;

  const apiUrl = `${environment.apiGatewayUrl}/api/events`;

  const mockEvent: Event = {
    id: 1,
    name: 'Angular Workshop',
    description: 'Learn Angular',
    details: 'Full day workshop',
    location: 'Tunis, Tunisia',
    latitude: 36.8,
    longitude: 10.1,
    date: new Date('2025-06-01'),
    status: 'UPCOMING',
    attendees: 20,
    maxParticipants: 50,
    currentParticipants: 20,
    category: 'TECHNOLOGY',
    createdAt: new Date('2025-01-01')
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [EventService]
    });
    service = TestBed.inject(EventService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('getEvents() should return a list of events via GET', () => {
    service.getEvents().subscribe(events => {
      expect(events.length).toBe(1);
      expect(events[0].name).toBe('Angular Workshop');
      expect(events[0].status).toBe('UPCOMING');
    });

    const req = httpMock.expectOne(apiUrl);
    expect(req.request.method).toBe('GET');
    req.flush([mockEvent]);
  });

  it('getEventById() should return a single event by id via GET', () => {
    service.getEventById(1).subscribe(event => {
      expect(event.id).toBe(1);
      expect(event.name).toBe('Angular Workshop');
      expect(event.location).toBe('Tunis, Tunisia');
    });

    const req = httpMock.expectOne(`${apiUrl}/1`);
    expect(req.request.method).toBe('GET');
    req.flush(mockEvent);
  });

  it('createEvent() should POST and return created event', () => {
    const newEvent: Event = {
      name: 'DevOps Day',
      description: 'CI/CD Workshop',
      details: 'Learn CI/CD pipelines',
      location: 'Sfax, Tunisia',
      latitude: 34.7,
      longitude: 10.7,
      date: new Date('2025-07-01'),
      attendees: 0,
      category: 'TECHNOLOGY',
      createdAt: new Date()
    };

    service.createEvent(newEvent).subscribe(event => {
      expect(event.id).toBe(1);
      expect(event.name).toBe('Angular Workshop');
    });

    const req = httpMock.expectOne(apiUrl);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(newEvent);
    req.flush(mockEvent);
  });

  it('updateEvent() should PUT updated event data', () => {
    const updatedEvent = { ...mockEvent, name: 'Updated Workshop' };

    service.updateEvent(1, updatedEvent).subscribe(event => {
      expect(event.id).toBe(1);
    });

    const req = httpMock.expectOne(`${apiUrl}/1`);
    expect(req.request.method).toBe('PUT');
    req.flush(mockEvent);
  });

  it('deleteEvent() should send DELETE request', () => {
    service.deleteEvent(1).subscribe();

    const req = httpMock.expectOne(`${apiUrl}/1`);
    expect(req.request.method).toBe('DELETE');
    req.flush(null);
  });

  it('getEvents() should handle empty array response', () => {
    service.getEvents().subscribe(events => {
      expect(events).toEqual([]);
      expect(events.length).toBe(0);
    });

    const req = httpMock.expectOne(apiUrl);
    req.flush([]);
  });
});
