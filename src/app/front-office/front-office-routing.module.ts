import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { FrontLayoutComponent } from './front-layout/front-layout.component';
import { HomeComponent } from './home/home.component';
import { AboutComponent } from './about/about.component';
import { EvaluationConfigComponent } from './evaluation/evaluation-config/evaluation-config.component';
import { ExamCreateComponent } from './evaluation/exam-create/exam-create.component';
import { QuizCreateComponent } from './evaluation/quiz-create/quiz-create.component';
import { EvaluationDashboardComponent } from './evaluation/evaluation-dashboard/evaluation-dashboard.component';
import { ExamTakingComponent } from './evaluation/exam-taking/exam-taking.component';
import { SessionFormComponent } from './SessionPlanning/Sessions/session-form/session-form.component';
import { SessionListComponent } from './SessionPlanning/Sessions/session-list/session-list.component';
import { PlanningFormComponent } from './SessionPlanning/Planning/planning-form/planning-form.component';
import { PlanningListComponent } from './SessionPlanning/Planning/planning-list/planning-list.component';
import { PlanningDetailsComponent } from './SessionPlanning/Planning/planning-details/planning-details.component';
import { SessionDetailsComponent } from './SessionPlanning/Sessions/session-details/session-details.component';
import { QuizTakingComponent } from './evaluation/quiz-taking/quiz-taking.component';
import { EvaluationResultComponent } from './evaluation/evaluation-result/evaluation-result.component';
import { trainerGuard } from '../Guard/trainer.guard';
import { EvaluationStatsComponent } from './evaluation/evaluation-stats/evaluation-stats.component';
import { CourseCatalogComponent } from './courses/course-catalog/course-catalog.component';
import { CourseCreateComponent } from './courses/course-create/course-create.component';
import { CoursePreviewComponent } from './learner/course-preview/course-preview.component';
import { CourseLearnerList } from './learner/course-learner-list/course-learner-list.component';
import { EnrolledStudentComponent } from './learner/enrolled-student/enrolled-student.component';
import { MycoursesComponent } from './learner/mycourses/mycourses.component';
import { PaymentComponent } from './payment/payment.component';
import { CartComponent } from './cart/cart.component';
import { PaymentSuccessComponent } from './payment-success/payment-success.component';
import { InstallmentPaymentComponent } from './installment/installment-payment/installment-payment.component';
import { InstallmentSuccessComponent } from './installment/installment-success/installment-success.component';
import { MyInvoicesComponent } from './my-invoices/my-invoices/my-invoices.component';
import { RefundLearnerComponent } from './RefundLearner/refund-learner/refund-learner.component';
import { ForumListComponent } from './forum/forum-list/forum-list.component';
import { ForumDetailsComponent } from './forum/forum-details/forum-details.component';
import { ForumFormComponent } from './forum/forum-form/forum-form.component';
import { EventsListComponent } from './events/events-list/events-list.component';
import { EventDetailsComponent } from './events/event-details/event-details.component';
import { EventFormComponent } from './events/event-form/event-form.component';
import { PublicPartnersComponent } from './public-partners/public-partners.component';
import { PublicTrainersComponent } from './public-trainers/public-trainers.component';
import { TrainerPartnerHiringComponent } from './trainerpartnerhiring/trainerpartnerhiring.component';
import { LearningPathListComponent } from './pathandSchedule/LearningPath/learning-path-list/learning-path-list.component';
import { LearningPathFormComponent } from './pathandSchedule/LearningPath/learning-path-form/learning-path-form.component';
import { LearningPathDetailComponent } from './pathandSchedule/LearningPath/learning-path-detail/learning-path-detail.component';
import { ScheduleFormComponent } from './pathandSchedule/Schedule/schedule-form/schedule-form.component';
import { EvaluationHistoryComponent } from './certification/evaluation-history/evaluation-history.component';
import { LearnerCertificationsComponent } from './certification/learner-certifications/learner-certifications.component';
import { TemplateEditorComponent } from './certification/template-editor/template-editor.component';
import { ReclamationComponent } from './reclamation/reclamation.component/reclamation.component.component';
import { EvaluationListComponent } from './evaluation/evaluation-list/evaluation-list.component';
import { AvailableCouponsComponent } from './available-coupons/available-coupons.component';

const routes: Routes = [
  {
    path: '',
    component: FrontLayoutComponent,
    children: [
      { path: '', component: HomeComponent },
      { path: 'coupons', component: AvailableCouponsComponent },
      { path: 'about', component: AboutComponent },
      {
        path: 'evaluation',
        component: EvaluationConfigComponent,
        canActivate: [trainerGuard],
      },
      {
        path: 'evaluation/edit/:id',
        component: EvaluationConfigComponent,
        canActivate: [trainerGuard],
      },
      {
        path: 'exam/create',
        component: ExamCreateComponent,
        canActivate: [trainerGuard],
      },
      {
        path: 'exam/edit/:id',
        component: ExamCreateComponent,
        canActivate: [trainerGuard],
      },
      {
        path: 'quiz/create',
        component: QuizCreateComponent,
        canActivate: [trainerGuard],
      },
      {
        path: 'quiz/edit/:id',
        component: QuizCreateComponent,
        canActivate: [trainerGuard],
      },
      {
        path: 'dashboard',
        component: EvaluationDashboardComponent,
        canActivate: [trainerGuard],
      },
      {
        path: 'evaluations/stats/:id',
        component: EvaluationStatsComponent,
        canActivate: [trainerGuard],
      },
          {
        path: 'evaluation/certificate-setup/:id',
        component: TemplateEditorComponent,
        canActivate: [trainerGuard],
      },
      {
        path: 'evaluations/history/:id',
        component: EvaluationHistoryComponent,
        canActivate: [trainerGuard],
      },
      {
        path: 'certifications',
        component: LearnerCertificationsComponent,
        
      },
      {
        path: 'cart/payment',
        component: PaymentComponent,
      },
      {
        path: 'cart/success',
        component: PaymentSuccessComponent,

      },
      {
        path: 'cart',
        component: CartComponent,
      },
      {
        path: 'learner/courses/:id',
        component: CoursePreviewComponent,
      },
      {
        path: 'learner/courses',
        component: CourseLearnerList,
      },
      {
        path: 'learner/enrolled-student/:courseId/:learnerId',
        component: EnrolledStudentComponent,
      },
      {
        path: 'learner/card',
        component: EnrolledStudentComponent,
      },
      {
        path: 'learner/card/payment',
        component: PaymentComponent,
      },
      {
        path: 'learner/mycourses',
        component: MycoursesComponent,
      },

      {
        path: 'cart/installment',
        component: InstallmentPaymentComponent,
      },


      {
        path: 'cart/installment-success',
        component: InstallmentSuccessComponent,
      },
      {
        path: 'cart/my-invoices',
        component: MyInvoicesComponent,
      },
      {
        path: 'refunds',
        component: RefundLearnerComponent,
      },







      {path: 'evaluations', component: EvaluationListComponent },
      { path: 'examPlayer/:id', component: ExamTakingComponent },
      { path: 'quizPlayer/:id', component: QuizTakingComponent },
      { path: 'evaluation-result', component: EvaluationResultComponent },

      { path: 'sessions', component: SessionListComponent },
      { path: 'sessionsList', component: SessionListComponent },

      { path: 'sessions/new', component: SessionFormComponent },
      { path: 'sessions/edit/:id', component: SessionFormComponent },
      { path: 'sessions/:id', component: SessionDetailsComponent },

      { path: 'planning', component: PlanningListComponent },
      { path: 'plannings/:id', component: PlanningDetailsComponent },
      { path: 'planning/add', component: PlanningFormComponent },
      { path: 'plannings/edit/:id', component: PlanningFormComponent },

      { path: 'courses', component: CourseCatalogComponent },
      { path: 'course/create', component: CourseCreateComponent },
      { path: 'course/edit/:id', component: CourseCreateComponent },
      { path: 'forum', component: ForumListComponent },
      { path: 'forum/create', component: ForumFormComponent },
      { path: 'forum/:id', component: ForumDetailsComponent },
      { path: 'forum/:id/edit', component: ForumFormComponent },
      { path: 'events', component: EventsListComponent },
      { path: 'events/create', component: EventFormComponent },
      { path: 'events/:id', component: EventDetailsComponent },
      { path: 'events/:id/edit', component: EventFormComponent },
      { path: 'trainer-partner-hiring', component: TrainerPartnerHiringComponent },
      { path: 'partners', component: PublicPartnersComponent },
      { path: 'trainers', component: PublicTrainersComponent },
      { path: 'learning-paths', component: LearningPathListComponent },           // List all learning paths
      { path: 'learning-paths/new', component: LearningPathFormComponent },       // Create new learning path
      { path: 'learning-paths/edit/:id', component: LearningPathFormComponent },  // Edit existing learning path
   { path: 'learning-paths/:id', component: LearningPathDetailComponent },
   { path: 'schedule/new', component: ScheduleFormComponent },
   { path: 'reclamation', component: ReclamationComponent },
    // View learning path details

    ],
  },
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
})
export class FrontOfficeRoutingModule { }
