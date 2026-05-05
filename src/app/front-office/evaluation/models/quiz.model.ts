export interface QuizAnswer {
  id: number;
  text: string;
  isCorrect: boolean;
}

export interface QuizQuestion {
  id: number;
  question: string;
  points: number;
  answers: QuizAnswer[];
  explanation?: string;
}

export interface Quiz {
  config: any; // EvaluationConfig
  questions: QuizQuestion[];
}