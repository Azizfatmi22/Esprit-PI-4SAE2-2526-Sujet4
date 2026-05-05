// models/learning-path.model.ts

export enum LearningLevel {
  BEGINNER = 'BEGINNER',
  INTERMEDIATE = 'INTERMEDIATE',
  ADVANCED = 'ADVANCED'
}

export enum LearningPathStatus {
  ACTIVE = 'ACTIVE',
  INACTIVE = 'INACTIVE',
  DRAFT = 'DRAFT',
  ARCHIVED = 'ARCHIVED'
}

export interface LearningPath {
  id?: number;
  title: string;
  description: string;
  level: LearningLevel;
  status: LearningPathStatus;
  totalHours?: number;
  objectives: string;
  sessionIds?: number[];
}

export interface PathRisk {
  risk: string;
  severity: 'low' | 'medium' | 'high';
  recommendation?: string;
}

export interface PathAnalytics {
  totalHours: number;
  difficulty: 'EASY' | 'MEDIUM' | 'HARD';
  risks: string[];
  sessionCount: number;
  completionRate?: number;
}