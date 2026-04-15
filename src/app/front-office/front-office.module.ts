import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { NgChartsModule } from 'ng2-charts';
import { QRCodeModule } from 'angularx-qrcode';
import { QuillModule } from 'ngx-quill';

import { FrontOfficeRoutingModule } from './front-office-routing.module';
import { FrontLayoutComponent } from './front-layout/front-layout.component';
import { HomeComponent } from './home/home.component';
import { AboutComponent } from './about/about.component';
import { FooterComponent } from './footer/footer.component';
import { NavbarComponent } from './navbar/navbar.component';
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
import { EvaluationStatsComponent } from './evaluation/evaluation-stats/evaluation-stats.component';
import { CourseCatalogComponent } from './courses/course-catalog/course-catalog.component';
import { CourseCreateComponent } from './courses/course-create/course-create.component';
import { CoursePreviewComponent } from './learner/course-preview/course-preview.component';
import { CourseLearnerList } from './learner/course-learner-list/course-learner-list.component';
import { EnrolledStudentComponent } from './learner/enrolled-student/enrolled-student.component';
import { MycoursesComponent } from './learner/mycourses/mycourses.component';
import { CartComponent } from './cart/cart.component';
import { PaymentComponent } from './payment/payment.component';
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
import { WavesComponent } from './events/waves/waves.component';
import { RibbonsComponent } from './events/ribbons/ribbons.component';
import { PublicPartnersComponent } from './public-partners/public-partners.component';
import { PublicTrainersComponent } from './public-trainers/public-trainers.component';
import { TrainerPartnerHiringComponent } from './trainerpartnerhiring/trainerpartnerhiring.component';
import { LearningPathListComponent } from './pathandSchedule/LearningPath/learning-path-list/learning-path-list.component';
import { LearningPathFormComponent } from './pathandSchedule/LearningPath/learning-path-form/learning-path-form.component';
import { LearningPathDetailComponent } from './pathandSchedule/LearningPath/learning-path-detail/learning-path-detail.component';
import { ScheduleListComponent } from './pathandSchedule/Schedule/schedule-list/schedule-list.component';
import { ScheduleFormComponent } from './pathandSchedule/Schedule/schedule-form/schedule-form.component';
import { EvaluationHistoryComponent } from './certification/evaluation-history/evaluation-history.component';
import { LearnerCertificationsComponent } from './certification/learner-certifications/learner-certifications.component';
import { TemplateEditorComponent } from './certification/template-editor/template-editor.component';
import { SafeHtmlPipe } from '../pipes/safe-html.pipe';
import { ReclamationComponent } from './reclamation/reclamation.component/reclamation.component.component';
import { EvaluationListComponent } from './evaluation/evaluation-list/evaluation-list.component';

//import { ReclamationComponent } from './reclamation/reclamation.component/reclamation.component';


@NgModule({
  declarations: [
    FrontLayoutComponent,
    HomeComponent,
    AboutComponent,
    FooterComponent,
    EvaluationConfigComponent,
    ExamCreateComponent,
    QuizCreateComponent,
    QuizTakingComponent,
    EvaluationDashboardComponent,
    ExamTakingComponent,
    SessionFormComponent,
    SessionListComponent,
    PlanningFormComponent,
    PlanningListComponent,
    PlanningDetailsComponent,
    SessionDetailsComponent,
    EvaluationResultComponent,
    EvaluationStatsComponent,
    CourseCatalogComponent,
    CourseCreateComponent,
    CoursePreviewComponent,
    CourseLearnerList,
    EnrolledStudentComponent,
    MycoursesComponent,
    CartComponent,
    PaymentComponent,
    PaymentSuccessComponent,
    InstallmentPaymentComponent,
    InstallmentSuccessComponent,
    RefundLearnerComponent,
    ForumListComponent,
    ForumDetailsComponent,
    ForumFormComponent,
    EventsListComponent,
    EventDetailsComponent,
    EventFormComponent,
    WavesComponent,
    PublicPartnersComponent,
    PublicTrainersComponent,
    LearningPathListComponent,
    LearningPathFormComponent,
    LearningPathDetailComponent,
    ScheduleListComponent,
    ScheduleFormComponent,
    EvaluationHistoryComponent,
    LearnerCertificationsComponent,
    TemplateEditorComponent,
    SafeHtmlPipe,
    ReclamationComponent,
    EvaluationListComponent,
  ],
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    RouterModule,
    FrontOfficeRoutingModule,
    NgChartsModule,
    QRCodeModule,
    RibbonsComponent,
    NavbarComponent,
    MyInvoicesComponent,
    TrainerPartnerHiringComponent,
    QuillModule.forRoot(),
  ],
})
export class FrontOfficeModule { }
