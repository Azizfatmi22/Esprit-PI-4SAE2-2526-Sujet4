export interface Session {
  id?: number;
  trainerId?: number;
  courseId?: number;
  maxParticipants: number;
  createdAt?: Date;
    status: SessionStatus;
 
}
export enum SessionStatus {
  PLANNED = 'PLANNED',
  ONGOING = 'ONGOING',
  COMPLETED = 'COMPLETED',
  CANCELLED = 'CANCELLED'
}