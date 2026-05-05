import { Component, OnInit } from '@angular/core';
import { Exam, ExamQuestion } from '../models/exam.model';
import { ActivatedRoute, Router } from '@angular/router';
import { EvaluationService } from '../../services/evaluation.service';

@Component({
  selector: 'app-exam-create',
  templateUrl: './exam-create.component.html',
  styleUrl: './exam-create.component.scss',
})
export class ExamCreateComponent implements OnInit {
  exam: Exam = {
    config: null,
    questions: [],
  };

  // Input temporaire pour saisir un mot-clé avant d'appuyer sur Entrée
  currentKeyword: string = '';

  currentQuestion: ExamQuestion = {
    id: 0,
    question: '',
    answerCorrection: '',
    points: 3,
    keywords: [], // Initialisation importante
  };

  isEditMode = false;
  editId: string | null = null;
  editingQuestionId: number | null = null;
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
          this.exam.config = config || data;
          this.exam.questions = data.examQuestions.map(
            (q: any, index: number) => ({
              id: index + 1,
              question: q.question,
              answerCorrection: q.trainerCorrection,
              points: q.points,
              keywords: q.keywords || [], // On récupère les mots-clés existants
            }),
          );
          this.updateStats();
        },
      });
    } else {
      this.exam.config = config;
    }
  }

  // --- Gestion des Mots-clés (Chips) ---

  addKeyword(event: any) {
    event.preventDefault();
    const val = this.currentKeyword.trim().toLowerCase();

    if (val) {
      if (!this.currentQuestion.keywords) {
        this.currentQuestion.keywords = [];
      }
      // Éviter les doublons dans la liste des mots-clés de la question actuelle
      if (!this.currentQuestion.keywords.includes(val)) {
        this.currentQuestion.keywords.push(val);
      }
      this.currentKeyword = '';
    }
  }

  removeKeyword(index: number) {
    this.currentQuestion.keywords.splice(index, 1);
  }

  // --- Gestion des Questions ---

  editQuestion(question: ExamQuestion) {
    this.editingQuestionId = question.id;
    // On utilise le spread operator pour ne pas modifier l'objet original directement
    this.currentQuestion = {
      ...question,
      keywords: [...(question.keywords || [])],
    };
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  addQuestion() {
    if (this.validateCurrentQuestion()) {
      if (this.editingQuestionId !== null) {
        // MODE MODIFICATION
        const index = this.exam.questions.findIndex(
          (q) => q.id === this.editingQuestionId,
        );
        if (index !== -1) {
          this.exam.questions[index] = {
            ...this.currentQuestion,
            id: this.editingQuestionId,
          };
        }
        this.editingQuestionId = null;
      } else {
        // MODE AJOUT
        const newQuestion: ExamQuestion = {
          ...this.currentQuestion,
          id: this.exam.questions.length + 1,
        };
        this.exam.questions.push(newQuestion);
      }

      this.updateStats();
      this.resetCurrentQuestion();
    }
  }

  removeQuestion(questionId: number) {
    this.exam.questions = this.exam.questions.filter(
      (q) => q.id !== questionId,
    );
    // Ré-indexation des IDs
    this.exam.questions.forEach((q, index) => (q.id = index + 1));
    this.updateStats();

    if (this.editingQuestionId === questionId) {
      this.resetCurrentQuestion();
    }
  }

  duplicateQuestion(question: ExamQuestion) {
    const duplicatedQuestion: ExamQuestion = {
      ...question,
      id: this.exam.questions.length + 1,
      question: question.question + ' (Copie)',
      keywords: [...(question.keywords || [])], // Copie profonde des mots-clés
    };

    this.exam.questions.push(duplicatedQuestion);
    this.updateStats();
  }

  // --- Utilitaires et Validation ---

  updateStats() {
    this.totalPoints = this.exam.questions.reduce(
      (sum, q) => sum + q.points,
      0,
    );
    this.questionCount = this.exam.questions.length;
  }

  saveExam() {
    if (this.validateExam()) {
      // Transformation pour correspondre au format attendu par le Microservice Java
      const formattedQuestions = this.exam.questions.map((q) => ({
        question: q.question,
        trainerCorrection: q.answerCorrection,
        points: q.points,
        keywords: q.keywords, // Envoi des mots-clés pour la correction automatique
      }));

      const request = this.isEditMode
        ? this.evaluationService.updateCompleteEvaluation(formattedQuestions)
        : this.evaluationService.saveCompleteEvaluation(formattedQuestions);

      request.subscribe({
        next: () => {
          alert(
            this.isEditMode ? 'Mise à jour réussie !' : 'Examen enregistré !',
          );
          this.evaluationService.clear();
          this.router.navigate(['/dashboard']);
        },
        error: (err) =>
          alert("Erreur lors de l'enregistrement : " + err.message),
      });
    }
  }

  private validateCurrentQuestion(): boolean {
    const hasQuestion = this.currentQuestion.question.trim() !== '';
    const hasCorrection = this.currentQuestion.answerCorrection.trim() !== '';
    const hasKeywords =
      this.currentQuestion.keywords && this.currentQuestion.keywords.length > 0;

    if (!hasQuestion) {
      alert('Veuillez saisir une question.');
      return false;
    }
    if (!hasCorrection) {
      alert('Veuillez fournir une réponse correcte pour référence.');
      return false;
    }
    if (!hasKeywords) {
      alert(
        'Veuillez ajouter au moins un mot-clé pour la validation automatique.',
      );
      return false;
    }

    return true;
  }

  private validateExam(): boolean {
    if (this.exam.questions.length === 0) {
      alert("Veuillez ajouter au moins une question à l'examen.");
      return false;
    }
    return true;
  }

  private resetCurrentQuestion() {
    this.currentQuestion = {
      id: 0,
      question: '',
      answerCorrection: '',
      points: 3,
      keywords: [],
    };
    this.currentKeyword = '';
    this.editingQuestionId = null;
  }

  formatDate(date: any): string {
    if (!date) return 'Non défini';
    return new Date(date).toLocaleDateString('fr-FR', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
    });
  }

  getMinRequiredPoints(): number {
    const minScore = this.exam.config?.minSuccessScore || 70;
    return Math.ceil((this.totalPoints * minScore) / 100);
  }

  updateQuestionPoints(questionId: number, points: number) {
    const question = this.exam.questions.find((q) => q.id === questionId);
    if (question) {
      question.points = points;
      this.updateStats();
    }
  }
}
