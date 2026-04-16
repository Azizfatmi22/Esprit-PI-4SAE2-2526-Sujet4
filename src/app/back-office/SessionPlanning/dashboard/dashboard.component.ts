import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { SessionService } from '../../../front-office/SessionPlanning/services/session.service';
import { PlanningService } from '../../../front-office/SessionPlanning/services/planning.service';
import { LocationService } from '../../../front-office/SessionPlanning/services/location.service';
import { Session , SessionStatus} from '../../../front-office/SessionPlanning/models/session';


@Component({
  selector: 'app-dashboard',
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss']
})
export class DashboardComponent implements OnInit {
  currentDate = new Date();
  
  // Stats
  totalSessions = 0;
  activeSessions = 0;
  totalParticipants = 0;
  completionRate = 0;
  
  // Counts by status
  plannedCount = 0;
  inProgressCount = 0;
  completedCount = 0;
  cancelledCount = 0;
  
  // Percentages
  plannedPercentage = 0;
  inProgressPercentage = 0;
  completedPercentage = 0;
  cancelledPercentage = 0;
  
  // Trends
  newSessions = 12;
  newActiveSessions = 5;
  newParticipants = 48;
  completionRateChange = 8;
  
  // Data
  upcomingSessions: any[] = [];
  recentActivities: any[] = [];
  
  locationStats = [
    { name: 'Rooms', icon: '🏢', count: 8, color: '#4facfe' },
    { name: 'Buildings', icon: '🏛️', count: 3, color: '#8b5cf6' },
    { name: 'Campuses', icon: '🎓', count: 2, color: '#10b981' },
    { name: 'Online', icon: '💻', count: 5, color: '#f59e0b' }
  ];

  constructor(
    private router: Router,
    private sessionService: SessionService,
    private planningService: PlanningService,
    private locationService: LocationService
  ) {}

  ngOnInit(): void {
    this.loadDashboardData();
  }

  loadDashboardData(): void {
    this.sessionService.getAllSessions().subscribe({
      next: (sessions) => {
        this.totalSessions = sessions.length;
        this.calculateStatusCounts(sessions);
        this.calculatePercentages();
        this.getUpcomingSessions(sessions);
        this.generateRecentActivities(sessions);
      },
      error: (error) => {
        console.error('Error loading dashboard data:', error);
      }
    });
  }

  calculateStatusCounts(sessions: Session[]): void {
    this.plannedCount = sessions.filter(s => s.status === SessionStatus.PLANNED).length;
    this.inProgressCount = sessions.filter(s => s.status === SessionStatus.ONGOING).length;
    this.completedCount = sessions.filter(s => s.status === SessionStatus.COMPLETED).length;
    this.cancelledCount = sessions.filter(s => s.status === SessionStatus.CANCELLED).length;
    this.activeSessions = this.inProgressCount;
  }

  calculatePercentages(): void {
    if (this.totalSessions === 0) return;
    
    this.plannedPercentage = (this.plannedCount / this.totalSessions) * 100;
    this.inProgressPercentage = (this.inProgressCount / this.totalSessions) * 100;
    this.completedPercentage = (this.completedCount / this.totalSessions) * 100;
    this.cancelledPercentage = (this.cancelledCount / this.totalSessions) * 100;
    
    // Completion rate (completed / total)
    this.completionRate = Math.round((this.completedCount / this.totalSessions) * 100);
  }

  getUpcomingSessions(sessions: Session[]): void {
    const now = new Date();
    this.upcomingSessions = sessions
      .filter(s => s.status === SessionStatus.PLANNED && s.createdAt && new Date(s.createdAt) > now)
      .slice(0, 5)
      .map(s => ({
        title: `Session #${s.id}`,
        date: s.createdAt,
        participants: 0,
        maxParticipants: s.maxParticipants,
        status: s.status
      }));
  }

  generateRecentActivities(sessions: Session[]): void {
    this.recentActivities = [];
    
    // Add session activities
    sessions.slice(0, 5).forEach(session => {
      this.recentActivities.push({
        type: 'session',
        text: `Session #${session.id} was created`,
        time: new Date(),
        color: '#4facfe'
      });
    });
  }
}