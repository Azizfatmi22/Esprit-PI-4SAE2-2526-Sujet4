export interface EvaluationResult {
  learnerName: string;
  evaluationTitle: string;
  type: 'QUIZ' | 'EXAM';
  scoreObtained: number;
  totalPossiblePoints: number;
  correctAnswersCount: number;
  wrongAnswersCount: number;
  totalQuestions: number;
  percentage: number;
  minSuccessScore: number;
  isPassed: boolean;
  completionDate: Date;
}