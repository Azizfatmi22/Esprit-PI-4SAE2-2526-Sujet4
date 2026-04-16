import { isPlatformBrowser } from '@angular/common';
import {
  Component,
  Inject,
  OnDestroy,
  OnInit,
  PLATFORM_ID,
} from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { EvaluationService } from '../../services/evaluation.service';
import { UserService } from '../../services/user.service';

interface ExamQuestion {
  id: number;
  question: string;
  points: number;
}

interface Exam {
  id: number;
  title: string;
  duration: number;
  questions: ExamQuestion[];
}

interface StudentAnswer {
  questionId: number;
  answer: string;
}

type AccessStatus = 'WAITING' | 'AUTHORIZED' | 'EXPIRED' | 'LOADING';

@Component({
  selector: 'app-exam-taking',
  templateUrl: './exam-taking.component.html',
  styleUrl: './exam-taking.component.scss',
})
export class ExamTakingComponent implements OnInit, OnDestroy {
  // Données de l'examen
  exam: Exam | null = null;
  examId: string | null = null;
  currentIndex: number = 0;
  studentAnswers: StudentAnswer[] = [];

  // Timer Examen en cours
  remainingTime: number = 0;
  timerInterval: any;
  timePercentage: number = 100;

  // À ajouter avec vos autres propriétés
  private startTimeMillis: number = 0;

  // Gestion des accès et temps d'attente
  accessStatus: AccessStatus = 'LOADING';
  startTime: Date | null = null;
  timeUntilStart: number = 0;
  waitingInterval: any;

  // Capture du moment réel du début pour l'anti-triche
  examActualStartTime: Date | null = null;

  // Dynamique Keycloak
  userRole: 'TRAINER' | 'LEARNER' = 'LEARNER';

  // Loading state
  isLoading: boolean = true;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private evaluationService: EvaluationService,
    private userService: UserService,
    @Inject(PLATFORM_ID) private platformId: Object,
  ) {}

  ngOnInit() {
    this.examId = this.route.snapshot.paramMap.get('id');

    // Détection dynamique du rôle via UserService (Keycloak)
    this.userRole = this.userService.hasRole('TRAINER') ? 'TRAINER' : 'LEARNER';

    if (this.examId) {
      this.loadExam(this.examId);
    }
  }

  ngOnDestroy() {
    this.clearTimer();
    if (this.waitingInterval) clearInterval(this.waitingInterval);
  }

  loadExam(id: string) {
    this.isLoading = true;
    this.accessStatus = 'LOADING';

    // 1. Tentative d'appel standard
    this.evaluationService.getEvaluationById(id).subscribe({
      next: (data) => {
        const now = new Date();
        this.startTime = new Date(data.date);
        const endTime = new Date(
          this.startTime.getTime() + data.duration * 60000,
        );

        // --- GESTION PAR RÔLE ---
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

        // Si TRAINER ou LEARNER dans les temps : AUTHORIZED
        this.setupExamData(data, now);
      },
      error: (err) => {
        // 2. Fallback pour Trainer si l'accès standard est bloqué par le temps (403/410)
        if (this.userRole === 'TRAINER') {
          this.evaluationService.getEvaluationDetails(id).subscribe({
            next: (data) => this.setupExamData(data, new Date()),
            error: () => {
              this.isLoading = false;
              this.accessStatus = 'EXPIRED';
            },
          });
        } else {
          console.error('Erreur API:', err);
          this.isLoading = false;
          this.accessStatus = 'EXPIRED';
        }
      },
    });
  }

  private setupExamData(data: any, referenceDate: Date) {
    this.accessStatus = 'AUTHORIZED';
    this.examActualStartTime = new Date();
    this.startTimeMillis = Date.now();

    this.exam = {
      id: data.id,
      title: data.title,
      duration: data.duration,
      questions: data.examQuestions || [],
    };

    if (this.exam && this.exam.questions.length > 0) {
      if (this.userRole === 'LEARNER') {
        const totalSessionSeconds = data.duration * 60;
        const secondsSinceStart = Math.floor(
          (referenceDate.getTime() - new Date(data.date).getTime()) / 1000,
        );
        this.remainingTime =
          totalSessionSeconds - (secondsSinceStart > 0 ? secondsSinceStart : 0);

        if (this.remainingTime <= 0) {
          this.accessStatus = 'EXPIRED';
        } else {
          this.loadSavedAnswers();
          this.startTimer();
        }
      } else {
        this.remainingTime = data.duration * 60;
        this.timePercentage = 100;
      }
    }
    this.isLoading = false;
  }

  startWaitingTimer() {
    if (!isPlatformBrowser(this.platformId)) return;
    if (this.waitingInterval) clearInterval(this.waitingInterval);

    this.waitingInterval = setInterval(() => {
      if (this.timeUntilStart > 0) {
        this.timeUntilStart--;
      } else {
        clearInterval(this.waitingInterval);
        this.loadExam(this.examId!);
      }
    }, 1000);
  }

  loadSavedAnswers() {
    if (isPlatformBrowser(this.platformId) && this.exam && this.examId) {
      const saved = localStorage.getItem(`exam_${this.examId}_answers`);
      if (saved) {
        this.studentAnswers = JSON.parse(saved);
      } else {
        this.studentAnswers = this.exam.questions.map((q) => ({
          questionId: q.id,
          answer: '',
        }));
      }
    }
  }

  saveAnswers() {
    if (
      isPlatformBrowser(this.platformId) &&
      this.examId &&
      this.userRole === 'LEARNER'
    ) {
      localStorage.setItem(
        `exam_${this.examId}_answers`,
        JSON.stringify(this.studentAnswers),
      );
    }
  }

  startTimer() {
    if (
      !isPlatformBrowser(this.platformId) ||
      !this.exam ||
      this.userRole === 'TRAINER'
    )
      return;
    this.timerInterval = setInterval(() => {
      if (this.remainingTime > 0) {
        this.remainingTime--;
        this.timePercentage =
          (this.remainingTime / (this.exam!.duration * 60)) * 100;
      } else {
        this.submitExam();
      }
    }, 1000);
  }

  clearTimer() {
    if (this.timerInterval) clearInterval(this.timerInterval);
  }

  getCurrentAnswer(): string {
    if (!this.exam) return '';
    const currentQuestion = this.exam.questions[this.currentIndex];
    return (
      this.studentAnswers.find((a) => a.questionId === currentQuestion.id)
        ?.answer || ''
    );
  }

  updateAnswer(event: Event) {
    if (!this.exam || this.userRole === 'TRAINER') return;
    const value = (event.target as HTMLTextAreaElement).value;
    const currentQuestion = this.exam.questions[this.currentIndex];
    const answerObj = this.studentAnswers.find(
      (a) => a.questionId === currentQuestion.id,
    );

    if (answerObj) {
      answerObj.answer = value;
      this.saveAnswers();
    }
  }

  isQuestionAnswered(questionId: number): boolean {
    const answer = this.studentAnswers.find((a) => a.questionId === questionId);
    return !!answer && answer.answer.trim().length > 0;
  }

  getAnsweredCount(): number {
    return this.studentAnswers.filter((a) => a.answer.trim().length > 0).length;
  }

  goToQuestion(index: number) {
    this.currentIndex = index;
  }

  previousQuestion() {
    if (this.currentIndex > 0) this.currentIndex--;
  }

  nextQuestion() {
    if (this.currentIndex < (this.exam?.questions.length || 0) - 1) {
      this.currentIndex++;
    } else {
      this.confirmSubmit();
    }
  }

  confirmSubmit() {
    if (this.userRole === 'TRAINER') {
      alert('Mode Aperçu : Le formateur ne peut pas soumettre de réponses.');
      return;
    }
    if (
      isPlatformBrowser(this.platformId) &&
      window.confirm("Voulez-vous vraiment terminer l'examen ?")
    ) {
      this.submitExam();
    }
  }

  submitExam() {
    this.clearTimer();
    if (!this.examId || !this.exam || this.userRole === 'TRAINER') return;

    this.isLoading = true;
    // Calcul de la durée en secondes
    const timeSpentSeconds = Math.floor(
      (Date.now() - this.startTimeMillis) / 1000,
    );

    const answersToSubmit = this.studentAnswers.map((ans, index) => ({
      learnerId: this.userService.getUser()?.id || 1,
      learnerName: this.userService.getUser()?.fullName || 'Unknown',
      answerOfLearner: ans.answer || '',
      timeSpentSeconds: timeSpentSeconds,
      score: 0,
      responseDate:
        index === 0
          ? this.examActualStartTime?.toLocaleString('sv').replace(' ', 'T')
          : null,
      question: {
        id: ans.questionId,
      },
    }));
    this.evaluationService.saveExamAnswers(answersToSubmit).subscribe({
      next: (response) => {
        this.isLoading = false;
        if (isPlatformBrowser(this.platformId)) {
          localStorage.removeItem(`exam_${this.examId}_answers`);
        }
        this.router.navigate(['/evaluation-result'], {
          state: { result: response },
        });
      },
      error: (err) => {
        this.isLoading = false;
        console.error('Erreur détaillée du serveur:', err.error); // Pour voir le message précis de Spring
        alert(
          'Erreur 400 : Les données envoyées ne sont pas valides pour le serveur.',
        );
      },
    });
  }

  formatTime(seconds: number): string {
    const minutes = Math.floor(seconds / 60);
    const secs = seconds % 60;
    return `${minutes}:${secs.toString().padStart(2, '0')}`;
  }

  getTimerColor(): string {
    return this.timePercentage > 20 ? 'normal' : 'danger';
  }

  isLastQuestion(): boolean {
    return !!this.exam && this.currentIndex === this.exam.questions.length - 1;
  }
}
