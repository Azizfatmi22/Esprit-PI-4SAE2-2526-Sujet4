export interface ExamQuestion {
  id: number;
  question: string;
  answerCorrection: string;
  points: number;
  keywords: string[];
}

export interface Exam {
  config: any;
  questions: ExamQuestion[];
}
