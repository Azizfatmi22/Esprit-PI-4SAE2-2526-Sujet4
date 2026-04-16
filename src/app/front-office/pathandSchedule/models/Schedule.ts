// models/schedule.model.ts
export enum ScheduleStatus {
  PENDING = 'PENDING',
  ACTIVE = 'ACTIVE',
  CONFIRMED = 'CONFIRMED',
  CANCELLED = 'CANCELLED'
}

export enum ScheduleType {
  LIVE = 'LIVE',
  RECORDED = 'RECORDED',
  WORKSHOP = 'WORKSHOP'
}

export interface Schedule {
  id?: number;
  title: string;
  notes?: string;
  startTime: Date;
  endTime: Date;
  status: ScheduleStatus;
  type: ScheduleType;
  planningId?: number;
}

export interface ScheduleAnalytics {
  totalSchedules: number;
  totalHours: number;
  averageDurationHours: number;
  statusDistribution: Record<ScheduleStatus, number>;
  typeDistribution: Record<ScheduleType, number>;
  dailyDistribution: Record<string, number>;
  peakHours: Record<number, number>;
}

export interface ScheduleStatistics {
  totalSchedules: number;
  pending: number;
  active: number;
  confirmed: number;
  cancelled: number;
  completionRate: number;
  upcomingSchedules: number;
  delayedSchedules: number;
  totalHours: number;
}

export interface ScheduleEfficiency {
  efficiencyScore: number;
  utilizationRate: number;
  totalGapHours: number;
  recommendation: string;
}