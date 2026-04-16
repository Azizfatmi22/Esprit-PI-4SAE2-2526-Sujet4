export type EvaluationType = 'QUIZ' | 'EXAM';

export interface EvaluationConfig {
  id?: string;
  courseId?: number;
  title: string;
  duration: number;
  date: any;
  type: EvaluationType;
  minSuccessScore: number;
  createdAt?: Date;
}
