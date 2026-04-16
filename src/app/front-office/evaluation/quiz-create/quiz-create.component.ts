import { Component, OnInit } from '@angular/core';
import { Quiz, QuizQuestion } from '../models/quiz.model';
import { EvaluationService } from '../../services/evaluation.service';
import { ActivatedRoute, Router } from '@angular/router';

@Component({
  selector: 'app-quiz-create',
  templateUrl: './quiz-create.component.html',
  styleUrl: './quiz-create.component.scss',
})
export class QuizCreateComponent implements OnInit {
  quiz: Quiz = {
    config: null,
    questions: [],
  };

  currentQuestion: QuizQuestion = {
    id: 0,
    question: '',
    points: 10,
    answers: [
      { id: 1, text: '', isCorrect: false },
      { id: 2, text: '', isCorrect: false },
      { id: 3, text: '', isCorrect: false },
      { id: 4, text: '', isCorrect: false },
    ],
    explanation: '',
  };

  // Variables pour l'update
  isEditMode = false;
  editId: string | null = null;
  editingQuestionId: number | null = null;

  newAnswerText: string = '';
  totalPoints: number = 0;
  questionCount: number = 0;

  constructor(
    private evaluationService: EvaluationService,
    private router: Router,
    private route: ActivatedRoute,
  ) {}

  ngOnInit() {
    this.editId = this.route.snapshot.paramMap.get('id');
    const config = this.evaluationService.getCurrentEvaluation();

    if (this.editId) {
      this.isEditMode = true;
      this.evaluationService.getEvaluationDetails(this.editId).subscribe({
        next: (data) => {
          this.quiz.config = config || data;
          // Mapping des questions du backend vers ton modèle local
          this.quiz.questions = data.quizzQuestions.map(
            (q: any, index: number) => ({
              id: index + 1,
              question: q.question,
              points: q.points,
              answers: q.quizzAnswers.map((a: any, aIndex: number) => ({
                id: aIndex + 1,
                text: a.answer,
                isCorrect: a.isCorrect,
              })),
            }),
          );
          this.updateStats();
        },
      });
    } else {
      this.quiz.config = config;
    }

    if (!this.quiz.config && !this.editId) {
      this.router.navigate(['/evaluation/create']);
    }
  }

  // Charger une question dans le formulaire pour modification
  editQuestion(question: QuizQuestion) {
    this.editingQuestionId = question.id;
    // On fait une copie profonde pour ne pas modifier la liste directement
    this.currentQuestion = JSON.parse(JSON.stringify(question));
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  addQuestion() {
    if (this.validateCurrentQuestion()) {
      const questionData = {
        question: this.currentQuestion.question,
        points: this.currentQuestion.points,
        explanation: this.currentQuestion.explanation,
        answers: this.currentQuestion.answers
          .filter((answer) => answer.text.trim() !== '')
          .map((answer, index) => ({
            ...answer,
            id: index + 1,
          })),
      };

      if (this.editingQuestionId !== null) {
        // Mode modification de question
        const index = this.quiz.questions.findIndex(
          (q) => q.id === this.editingQuestionId,
        );
        if (index !== -1) {
          this.quiz.questions[index] = {
            ...questionData,
            id: this.editingQuestionId,
          };
        }
        this.editingQuestionId = null;
      } else {
        // Mode ajout normal
        const newQuestion: QuizQuestion = {
          ...questionData,
          id: this.quiz.questions.length + 1,
        };
        this.quiz.questions.push(newQuestion);
      }

      this.updateStats();
      this.resetCurrentQuestion();
    }
  }

  removeQuestion(questionId: number) {
    this.quiz.questions = this.quiz.questions.filter(
      (q) => q.id !== questionId,
    );
    this.quiz.questions.forEach((q, index) => (q.id = index + 1));
    this.updateStats();
    if (this.editingQuestionId === questionId) this.resetCurrentQuestion();
  }

  addAnswer() {
    if (this.newAnswerText.trim()) {
      const newAnswerId = this.currentQuestion.answers.length + 1;
      this.currentQuestion.answers.push({
        id: newAnswerId,
        text: this.newAnswerText,
        isCorrect: false,
      });
      this.newAnswerText = '';
    }
  }

  removeAnswer(answerId: number) {
    if (this.currentQuestion.answers.length > 2) {
      this.currentQuestion.answers = this.currentQuestion.answers.filter(
        (a) => a.id !== answerId,
      );
    }
  }

  setCorrectAnswer(answerId: number) {
    const answer = this.currentQuestion.answers.find((a) => a.id === answerId);
    if (answer) answer.isCorrect = !answer.isCorrect;
  }

  duplicateQuestion(question: QuizQuestion) {
    const duplicatedQuestion: QuizQuestion = JSON.parse(
      JSON.stringify(question),
    );
    duplicatedQuestion.id = this.quiz.questions.length + 1;
    duplicatedQuestion.question += ' (Copie)';
    this.quiz.questions.push(duplicatedQuestion);
    this.updateStats();
  }

  updateStats() {
    this.totalPoints = this.quiz.questions.reduce(
      (sum, q) => sum + q.points,
      0,
    );
    this.questionCount = this.quiz.questions.length;
  }

  saveQuiz() {
    if (this.validateQuiz()) {
      const formattedQuestions = this.quiz.questions.map((q) => ({
        question: q.question,
        points: q.points,
        quizzAnswers: q.answers.map((a) => ({
          answer: a.text,
          isCorrect: a.isCorrect,
        })),
      }));

      const request = this.isEditMode
        ? this.evaluationService.updateCompleteEvaluation(formattedQuestions)
        : this.evaluationService.saveCompleteEvaluation(formattedQuestions);

      request.subscribe({
        next: () => {
          alert(this.isEditMode ? 'Quiz mis à jour !' : 'Quiz enregistré !');
          this.evaluationService.clear();
          this.router.navigate(['/dashboard']);
        },
        error: (err) => alert('Erreur serveur : ' + err.message),
      });
    }
  }

  goBack() {
    this.router.navigate(['/evaluation/create']);
  }

  private validateCurrentQuestion(): boolean {
    const hasQuestion = this.currentQuestion.question.trim() !== '';
    const validAnswers = this.currentQuestion.answers.filter(
      (a) => a.text.trim() !== '',
    );
    const hasCorrectAnswer = validAnswers.some((a) => a.isCorrect);

    if (!hasQuestion) {
      alert('Veuillez saisir une question');
      return false;
    }
    if (validAnswers.length < 2) {
      alert('Veuillez ajouter au moins 2 réponses');
      return false;
    }
    if (!hasCorrectAnswer) {
      alert('Veuillez sélectionner au moins une réponse correcte');
      return false;
    }
    return true;
  }

  private validateQuiz(): boolean {
    if (this.quiz.questions.length === 0) {
      alert('Veuillez ajouter au moins une question');
      return false;
    }
    return true;
  }

  private resetCurrentQuestion() {
    this.currentQuestion = {
      id: 0,
      question: '',
      points: 10,
      answers: [
        { id: 1, text: '', isCorrect: false },
        { id: 2, text: '', isCorrect: false },
        { id: 3, text: '', isCorrect: false },
      ],
      explanation: '',
    };
    this.newAnswerText = '';
    this.editingQuestionId = null;
  }

  getAnswerLetter(index: number): string {
    return String.fromCharCode(65 + index);
  }
}
