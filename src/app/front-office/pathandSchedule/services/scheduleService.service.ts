// services/schedule.service.ts
import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Schedule, ScheduleAnalytics, ScheduleStatistics, ScheduleEfficiency, ScheduleStatus, ScheduleType } from '../models/Schedule';

@Injectable({
  providedIn: 'root'
})
export class ScheduleService {
  private apiUrl = 'http://localhost:8085/api/schedules';

  constructor(private http: HttpClient) { }

  // ==================== CRUD OPERATIONS ====================
  
  createSchedule(schedule: Schedule): Observable<Schedule> {
    return this.http.post<Schedule>(this.apiUrl, schedule);
  }

  getScheduleById(id: number): Observable<Schedule> {
    return this.http.get<Schedule>(`${this.apiUrl}/${id}`);
  }

  getAllSchedules(): Observable<Schedule[]> {
    return this.http.get<Schedule[]>(this.apiUrl);
  }

  updateSchedule(id: number, schedule: Schedule): Observable<Schedule> {
    return this.http.put<Schedule>(`${this.apiUrl}/${id}`, schedule);
  }

  deleteSchedule(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  // ==================== PLANNING RELATIONS ====================
  
  getSchedulesByPlanningId(planningId: number): Observable<Schedule[]> {
    return this.http.get<Schedule[]>(`${this.apiUrl}/planning/${planningId}`);
  }

  addScheduleToPlanning(planningId: number, schedule: Schedule): Observable<Schedule> {
    return this.http.post<Schedule>(`${this.apiUrl}/planning/${planningId}`, schedule);
  }

  removeScheduleFromPlanning(scheduleId: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${scheduleId}/planning`);
  }

  // ==================== ANALYTICS ====================
  
  getScheduleAnalytics(planningId: number): Observable<ScheduleAnalytics> {
    return this.http.get<ScheduleAnalytics>(`${this.apiUrl}/planning/${planningId}/analytics`);
  }

  getScheduleStatistics(planningId: number): Observable<ScheduleStatistics> {
    return this.http.get<ScheduleStatistics>(`${this.apiUrl}/planning/${planningId}/statistics`);
  }

  getScheduleEfficiency(planningId: number): Observable<ScheduleEfficiency> {
    return this.http.get<ScheduleEfficiency>(`${this.apiUrl}/planning/${planningId}/efficiency`);
  }

  getUpcomingSchedules(planningId: number, days: number = 7): Observable<any> {
    const params = new HttpParams().set('days', days.toString());
    return this.http.get<any>(`${this.apiUrl}/planning/${planningId}/upcoming`, { params });
  }

  // ==================== CONFLICT DETECTION ====================
  
  checkConflict(planningId: number, start: Date, end: Date): Observable<boolean> {
    const params = new HttpParams()
      .set('start', start.toISOString())
      .set('end', end.toISOString());
    return this.http.get<boolean>(`${this.apiUrl}/planning/${planningId}/conflict`, { params });
  }

  findConflicts(planningId: number, start: Date, end: Date): Observable<Schedule[]> {
    const params = new HttpParams()
      .set('start', start.toISOString())
      .set('end', end.toISOString());
    return this.http.get<Schedule[]>(`${this.apiUrl}/planning/${planningId}/conflicts`, { params });
  }

  checkConflictWithBuffer(planningId: number, start: Date, end: Date): Observable<boolean> {
    const params = new HttpParams()
      .set('start', start.toISOString())
      .set('end', end.toISOString());
    return this.http.get<boolean>(`${this.apiUrl}/planning/${planningId}/conflict-buffer`, { params });
  }

  // ==================== OPTIMIZATION ====================
  
  optimizeScheduleTime(scheduleId: number, preferredStart: Date): Observable<Schedule> {
    const params = new HttpParams().set('preferredStart', preferredStart.toISOString());
    return this.http.post<Schedule>(`${this.apiUrl}/${scheduleId}/optimize`, null, { params });
  }

  autoSchedule(planningId: number, daysNeeded: number): Observable<Schedule[]> {
    const params = new HttpParams().set('daysNeeded', daysNeeded.toString());
    return this.http.post<Schedule[]>(`${this.apiUrl}/planning/${planningId}/auto-schedule`, null, { params });
  }

  // ==================== STATUS MANAGEMENT ====================
  
  updateScheduleStatus(scheduleId: number, status: ScheduleStatus): Observable<Schedule> {
    const params = new HttpParams().set('status', status);
    return this.http.patch<Schedule>(`${this.apiUrl}/${scheduleId}/status`, null, { params });
  }

  updateAllSchedulesStatus(planningId: number): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/planning/${planningId}/update-statuses`, null);
  }

  getSchedulesByStatus(planningId: number, status: ScheduleStatus): Observable<Schedule[]> {
    return this.http.get<Schedule[]>(`${this.apiUrl}/planning/${planningId}/status/${status}`);
  }

  // ==================== GENERATION ====================
  
  generateWeeklySchedule(planningId: number, weeks: number = 4): Observable<Schedule[]> {
    const params = new HttpParams().set('weeks', weeks.toString());
    return this.http.post<Schedule[]>(`${this.apiUrl}/planning/${planningId}/generate-weekly`, null, { params });
  }

  generateFromTemplate(planningId: number, templateId: number): Observable<Schedule> {
    return this.http.post<Schedule>(`${this.apiUrl}/planning/${planningId}/generate-from-template/${templateId}`, null);
  }

  // ==================== TIME SLOTS ====================
  
  getAvailableTimeSlots(planningId: number, durationHours: number, daysAhead: number = 14): Observable<Date[]> {
    const params = new HttpParams()
      .set('durationHours', durationHours.toString())
      .set('daysAhead', daysAhead.toString());
    return this.http.get<Date[]>(`${this.apiUrl}/planning/${planningId}/available-slots`, { params });
  }

  getAlternativeSlots(planningId: number, durationHours: number, maxSuggestions: number = 5): Observable<Date[]> {
    const params = new HttpParams()
      .set('durationHours', durationHours.toString())
      .set('maxSuggestions', maxSuggestions.toString());
    return this.http.get<Date[]>(`${this.apiUrl}/planning/${planningId}/alternative-slots`, { params });
  }

  // ==================== RECURRING SESSIONS ====================
  
  createRecurringSessions(planningId: number, template: Schedule, startDate: Date, weeks: number, days: string[]): Observable<Schedule[]> {
    const params = new HttpParams()
      .set('startDate', startDate.toISOString().split('T')[0])
      .set('weeks', weeks.toString())
      .set('days', days.join(','));
    return this.http.post<Schedule[]>(`${this.apiUrl}/planning/${planningId}/recurring`, template, { params });
  }

  // ==================== SESSION MANAGEMENT ====================
  
  splitSession(scheduleId: number, breakMinutes: number = 30): Observable<Schedule[]> {
    const params = new HttpParams().set('breakMinutes', breakMinutes.toString());
    return this.http.post<Schedule[]>(`${this.apiUrl}/${scheduleId}/split`, null, { params });
  }

  shiftFutureSessions(planningId: number, fromDate: Date, offsetHours: number): Observable<void> {
    const params = new HttpParams()
      .set('fromDate', fromDate.toISOString())
      .set('offsetHours', offsetHours.toString());
    return this.http.post<void>(`${this.apiUrl}/planning/${planningId}/shift`, null, { params });
  }

  duplicateSchedule(scheduleId: number, newStartTime: Date): Observable<Schedule> {
    const params = new HttpParams().set('newStartTime', newStartTime.toISOString());
    return this.http.post<Schedule>(`${this.apiUrl}/${scheduleId}/duplicate`, null, { params });
  }

  // ==================== BULK OPERATIONS ====================
  
  bulkCreateSchedules(planningId: number, schedules: Schedule[]): Observable<Schedule[]> {
    return this.http.post<Schedule[]>(`${this.apiUrl}/planning/${planningId}/bulk`, schedules);
  }

  bulkUpdateStatus(scheduleIds: number[], status: ScheduleStatus): Observable<void> {
    const params = new HttpParams()
      .set('scheduleIds', scheduleIds.join(','))
      .set('status', status);
    return this.http.patch<void>(`${this.apiUrl}/bulk/status`, null, { params });
  }

  bulkDelete(scheduleIds: number[]): Observable<void> {
    const params = new HttpParams().set('scheduleIds', scheduleIds.join(','));
    return this.http.delete<void>(`${this.apiUrl}/bulk`, { params });
  }

  // ==================== EXPORT/IMPORT ====================
  
  exportSchedules(planningId: number): Observable<string> {
    return this.http.get(`${this.apiUrl}/planning/${planningId}/export`, { responseType: 'text' });
  }

  exportToIcal(planningId: number): Observable<string> {
    return this.http.get(`${this.apiUrl}/planning/${planningId}/export/ical`, { responseType: 'text' });
  }

  importSchedules(planningId: number, jsonData: string): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/planning/${planningId}/import`, jsonData);
  }

  // ==================== VALIDATION & SUMMARY ====================
  
  validateSchedule(scheduleId: number): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/${scheduleId}/validate`);
  }

  getScheduleSummary(planningId: number): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/planning/${planningId}/summary`);
  }
}