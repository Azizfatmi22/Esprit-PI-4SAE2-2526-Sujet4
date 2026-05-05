import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { ParticipationService } from './participation.service';
import { Participant } from '../models/participant.model';
import { environment } from '../../environment/envirement';

describe('ParticipationService', () => {
  let service: ParticipationService;
  let httpMock: HttpTestingController;

  const apiUrl = `${environment.apiGatewayUrl}/api/events`;

  const mockParticipant: Participant = {
    id: 1,
    name: 'Ali Ben Salem',
    email: 'ali@example.com'
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [ParticipationService]
    });
    service = TestBed.inject(ParticipationService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('addParticipant() should POST a new participant to an event', () => {
    const payload = { name: 'Ali Ben Salem', email: 'ali@example.com' };

    service.addParticipant(1, payload).subscribe(p => {
      expect(p.id).toBe(1);
      expect(p.name).toBe('Ali Ben Salem');
    });

    const req = httpMock.expectOne(`${apiUrl}/1/participants`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(payload);
    req.flush(mockParticipant);
  });

  it('getParticipants() should GET participants for an event', () => {
    service.getParticipants(1).subscribe(participants => {
      expect(participants.length).toBe(1);
      expect(participants[0].email).toBe('ali@example.com');
    });

    const req = httpMock.expectOne(`${apiUrl}/1/participants`);
    expect(req.request.method).toBe('GET');
    req.flush([mockParticipant]);
  });

  it('getParticipants() should return empty array on error fallback', () => {
    service.getParticipants(99).subscribe(participants => {
      expect(participants).toEqual([]);
    });

    // Fail the primary request
    const req1 = httpMock.expectOne(`${apiUrl}/99/participants`);
    req1.error(new ProgressEvent('Network error'));

    // Fail the fallback request too
    const req2 = httpMock.expectOne(`${apiUrl}/99/participants/all`);
    req2.error(new ProgressEvent('Network error'));
  });

  it('removeParticipant() should send DELETE request', () => {
    service.removeParticipant(1, 5).subscribe();

    const req = httpMock.expectOne(`${apiUrl}/1/participants/5`);
    expect(req.request.method).toBe('DELETE');
    req.flush(null);
  });

  it('confirm() should GET with token query param', () => {
    service.confirm(1, 'abc-token-123').subscribe();

    const req = httpMock.expectOne(r =>
      r.url === `${apiUrl}/1/participants/confirm` && r.params.get('token') === 'abc-token-123'
    );
    expect(req.request.method).toBe('GET');
    req.flush(null);
  });
});
