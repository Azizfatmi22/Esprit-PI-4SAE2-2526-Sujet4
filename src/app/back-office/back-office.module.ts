import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';

import { BackOfficeRoutingModule } from './back-office-routing.module';
import { BackLayoutComponent } from './back-layout/back-layout.component';
import { SideBarComponent } from './side-bar/side-bar.component';
import { StatisticsComponent } from './statistics/statistics.component';
import { LocationListComponent } from './SessionPlanning/location/location-list/location-list.component';
import { LocationFormComponent } from './SessionPlanning/location/location-form/location-form.component';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { DashboardComponent } from './SessionPlanning/dashboard/dashboard.component';
import { AdminEvaluationComponent } from './evaluation/admin-evaluation/admin-evaluation.component';
import { AdminCourseComponent } from './admin-course/admin-course.component';
import{AdminEnrollmentsComponent} from './admin-enrollments/admin-enrollments/admin-enrollments.component';
import { AdminInstallmentsComponent } from './admin-installments/admin-installments/admin-installments.component';
import { AdminPaymentsInvoicesComponent } from './admin-payments-invoices/admin-payments-invoices.component'
import { ForumAdminComponent } from './forum/forum-admin.component';
import { ParticipantsAdminComponent } from './events/participants-admin.component';
import { LocationDetailComponent } from './SessionPlanning/location/location-detail/location-detail.component';
import { AdminBadgeComponent } from './certification/admin-badge/admin-badge.component';
import { AdminReclamationsComponent } from './admin-reclamations/admin-reclamations.component';

@NgModule({
  declarations: [
    BackLayoutComponent,
    StatisticsComponent,
    LocationListComponent,
    LocationFormComponent,
    DashboardComponent,
    AdminEvaluationComponent,
    AdminCourseComponent,
    AdminInstallmentsComponent,
    AdminPaymentsInvoicesComponent,
    LocationDetailComponent,
    AdminEnrollmentsComponent,
    AdminBadgeComponent,
    AdminReclamationsComponent
  ],
  imports: [
    CommonModule,
    BackOfficeRoutingModule,
    ReactiveFormsModule,
    FormsModule,
    RouterModule,
    SideBarComponent,
    ForumAdminComponent,
    ParticipantsAdminComponent,
  ],
})
export class BackOfficeModule {}
