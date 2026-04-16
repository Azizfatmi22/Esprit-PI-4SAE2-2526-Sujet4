import { isPlatformBrowser, DatePipe } from '@angular/common';
import {
  Component,
  Inject,
  OnDestroy,
  OnInit,
  PLATFORM_ID,
} from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import {
  EvaluationService,
  QuizResult,
} from '../../services/evaluation.service';
import { UserService } from '../../services/user.service';

interface QuizAnswer {
  id: number;
  text: string;
  isCorrect: boolean;
}

interface QuizQuestion {
  id: number;
  question: string;
  points: number;
  answers: QuizAnswer[];
  explanation?: string;
}

interface Quiz {
  id: number;
  title: string;
  duration: number;
  questions: QuizQuestion[];
  minSuccessScore: number;
}

interface StudentAnswer {
  questionId: number;
  selectedAnswerId: number | null;
}

type AccessStatus = 'WAITING' | 'AUTHORIZED' | 'EXPIRED' | 'LOADING';

@Component({
  selector: 'app-quiz-taking',
  templateUrl: './quiz-taking.component.html',
  styleUrl: './quiz-taking.component.scss',
})
export class QuizTakingComponent implements OnInit, OnDestroy {
  // Données du quiz
  quiz: Quiz | null = null;
  quizId: string | null = null;

  // État du quiz
  currentIndex: number = 0;
  studentAnswers: StudentAnswer[] = [];

  // Timer Quiz en cours
  remainingTime: number = 0;
  timerInterval: any;
  timePercentage: number = 100;

  // Gestion des accès et temps d'attente
  accessStatus: AccessStatus = 'LOADING';
  startTime: Date | null = null;
  timeUntilStart: number = 0; // en secondes
  waitingInterval: any;
  startTimeMillis: number = 0;

  // Rôle de l'utilisateur
  userRole: 'TRAINER' | 'LEARNER' = 'LEARNER';

  // Loading state
  isLoading: boolean = true;

  constructor(
    private evaluationService: EvaluationService,
    private userService: UserService,
    private route: ActivatedRoute,
    private router: Router,
    @Inject(PLATFORM_ID) private platformId: Object,
  ) {}

  ngOnInit() {
    this.quizId = this.route.snapshot.paramMap.get('id');

    // Détection du rôle
    this.userRole = this.userService.hasRole('TRAINER') ? 'TRAINER' : 'LEARNER';

    if (this.quizId) {
      this.loadQuiz(this.quizId);
    }
  }

  ngOnDestroy() {
    this.clearTimer();
    if (this.waitingInterval) clearInterval(this.waitingInterval);
  }

  loadQuiz(id: string) {
    this.isLoading = true;
    this.accessStatus = 'LOADING';

    // Si TRAINER, on utilise getEvaluationDetails, sinon getEvaluationById
    const request =
      this.userRole === 'TRAINER'
        ? this.evaluationService.getEvaluationDetails(id)
        : this.evaluationService.getEvaluationById(id);

    request.subscribe({
      next: (data) => {
        const now = new Date();
        this.startTime = new Date(data.date);
        const endTime = new Date(
          this.startTime.getTime() + data.duration * 60000,
        );

        // --- GESTION DU TEMPS : UNIQUEMENT POUR LE LEARNER ---
        if (this.userRole === 'LEARNER') {
          if (now < this.startTime) {
            this.accessStatus = 'WAITING';
            this.timeUntilStart = Math.floor(
              (this.startTime.getTime() - now.getTime()) / 1000,
            );
            this.startWaitingTimer();
            this.isLoading = false;
            return;
          }

          if (now > endTime) {
            this.accessStatus = 'EXPIRED';
            this.isLoading = false;
            return;
          }
        }

        // Accès autorisé
        this.accessStatus = 'AUTHORIZED';
        this.startTimeMillis = Date.now();
        this.quiz = {
          id: data.id,
          title: data.title,
          duration: data.duration,
          minSuccessScore: data.minSuccessScore,
          questions: data.quizzQuestions.map((q: any) => ({
            id: q.id,
            question: q.question,
            points: q.points,
            answers: q.quizzAnswers.map((a: any) => ({
              id: a.id,
              text: a.answer,
              isCorrect: a.isCorrect,
            })),
          })),
        };

        if (this.quiz) {
          if (this.userRole === 'LEARNER') {
            const secondsRemaining = Math.floor(
              (endTime.getTime() - now.getTime()) / 1000,
            );
            this.remainingTime = Math.min(secondsRemaining, data.duration * 60);
            this.loadSavedAnswers();
            this.startTimer();
          } else {
            // Pas de timer pour le Trainer
            this.remainingTime = data.duration * 60;
            this.timePercentage = 100;
          }
        }
        this.isLoading = false;
      },
      error: (err) => {
        console.error('Erreur lors du chargement du quiz', err);
        this.isLoading = false;
        this.accessStatus = 'EXPIRED';
      },
    });
  }

  // --- Fonctions inchangées ---
  startWaitingTimer() {
    if (!isPlatformBrowser(this.platformId)) return;
    if (this.waitingInterval) clearInterval(this.waitingInterval);

    this.waitingInterval = setInterval(() => {
      if (this.timeUntilStart > 0) {
        this.timeUntilStart--;
      } else {
        clearInterval(this.waitingInterval);
        this.loadQuiz(this.quizId!);
      }
    }, 1000);
  }

  loadSavedAnswers() {
    if (isPlatformBrowser(this.platformId) && this.quiz && this.quizId) {
      const saved = localStorage.getItem(`quiz_${this.quizId}_answers`);
      if (saved) {
        this.studentAnswers = JSON.parse(saved);
      } else if (this.quiz) {
        this.studentAnswers = this.quiz.questions.map((q) => ({
          questionId: q.id,
          selectedAnswerId: null,
        }));
      }
    }
  }

  saveAnswers() {
    if (
      isPlatformBrowser(this.platformId) &&
      this.quizId &&
      this.userRole === 'LEARNER'
    ) {
      localStorage.setItem(
        `quiz_${this.quizId}_answers`,
        JSON.stringify(this.studentAnswers),
      );
    }
  }

  startTimer() {
    if (
      !isPlatformBrowser(this.platformId) ||
      !this.quiz ||
      this.userRole === 'TRAINER'
    )
      return;
    this.timerInterval = setInterval(() => {
      if (this.remainingTime > 0) {
        this.remainingTime--;
        if (this.quiz) {
          this.timePercentage =
            (this.remainingTime / (this.quiz.duration * 60)) * 100;
        }
      } else {
        this.submitQuiz();
      }
    }, 1000);
  }

  clearTimer() {
    if (this.timerInterval) clearInterval(this.timerInterval);
  }

  getCurrentAnswer(): number | null {
    if (!this.quiz) return null;
    const currentQuestion = this.quiz.questions[this.currentIndex];
    const answer = this.studentAnswers.find(
      (a) => a.questionId === currentQuestion.id,
    );
    return answer?.selectedAnswerId ?? null;
  }

  selectAnswer(answerId: number) {
    if (!this.quiz || this.userRole === 'TRAINER') return;
    const currentQuestion = this.quiz.questions[this.currentIndex];
    const existingIndex = this.studentAnswers.findIndex(
      (a) => a.questionId === currentQuestion.id,
    );

    if (existingIndex >= 0) {
      this.studentAnswers[existingIndex].selectedAnswerId = answerId;
    } else {
      this.studentAnswers.push({
        questionId: currentQuestion.id,
        selectedAnswerId: answerId,
      });
    }
    this.saveAnswers();
  }

  isQuestionAnswered(questionId: number): boolean {
    const answer = this.studentAnswers.find((a) => a.questionId === questionId);
    return answer ? answer.selectedAnswerId !== null : false;
  }

  getAnsweredCount(): number {
    if (!this.quiz) return 0;
    return this.quiz.questions.filter((q) => this.isQuestionAnswered(q.id))
      .length;
  }

  getAnswerLetter(index: number): string {
    return String.fromCharCode(65 + index);
  }

  goToQuestion(index: number) {
    if (this.quiz && index >= 0 && index < this.quiz.questions.length) {
      this.currentIndex = index;
    }
  }

  previousQuestion() {
    if (this.currentIndex > 0) this.currentIndex--;
  }

  nextQuestion() {
    if (!this.quiz) return;
    if (this.currentIndex < this.quiz.questions.length - 1) {
      this.currentIndex++;
    } else {
      this.confirmSubmit();
    }
  }

  confirmSubmit() {
    if (!this.quiz) return;
    if (this.userRole === 'TRAINER') {
      alert('Mode Aperçu : Le formateur ne peut pas soumettre de réponses.');
      return;
    }
    if (isPlatformBrowser(this.platformId)) {
      const answeredCount = this.getAnsweredCount();
      const totalQuestions = this.quiz.questions.length;
      const confirmMessage = `Vous avez répondu à ${answeredCount}/${totalQuestions} questions.\nVoulez-vous vraiment terminer le quiz ?`;
      if (window.confirm(confirmMessage)) {
        this.submitQuiz();
      }
    }
  }

  // --- TA STRUCTURE MAINTENUE ---
  submitQuiz() {
    this.clearTimer();
    if (!this.quizId || !this.quiz) return;
    this.isLoading = true;
    // Calcul de la durée en secondes
    const timeSpentSeconds = Math.floor(
      (Date.now() - this.startTimeMillis) / 1000,
    );

    const answersToSubmit = this.studentAnswers.map((ans) => ({
      learnerId: this.userService.getUser()?.id || 1,
      learnerName: this.userService.getUser()?.fullName || 'Unknown',
      question: { id: ans.questionId },
      timeSpentSeconds: timeSpentSeconds,
      selectedAnswer: { id: ans.selectedAnswerId },
      responseDate: new Date().toISOString(),
    }));

    this.evaluationService.saveQuizAnswers(answersToSubmit).subscribe({
      next: (result: QuizResult) => {
        console.log('submit du quiz:', answersToSubmit);
        this.isLoading = false;
        if (isPlatformBrowser(this.platformId)) {
          localStorage.removeItem(`quiz_${this.quizId}_answers`);
        }
        this.router.navigate(['/evaluation-result'], {
          state: { result: result },
        });
      },
      error: (err) => {
        this.isLoading = false;
        console.error('Erreur de soumission:', err);
        alert('Une erreur est survenue lors de la soumission.');
      },
    });
  }

  formatTime(seconds: number): string {
    const h = Math.floor(seconds / 3600);
    const m = Math.floor((seconds % 3600) / 60);
    const s = seconds % 60;
    if (h > 0) {
      return `${h}:${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}`;
    }
    return `${m}:${s.toString().padStart(2, '0')}`;
  }

  getTimerColor(): string {
    if (this.timePercentage > 50) return 'normal';
    if (this.timePercentage > 20) return 'warning';
    return 'danger';
  }

  isLastQuestion(): boolean {
    return this.quiz
      ? this.currentIndex === this.quiz.questions.length - 1
      : false;
  }
}
