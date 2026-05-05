import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { BackLayoutComponent } from './back-layout/back-layout.component';
import { StatisticsComponent } from './statistics/statistics.component';
import { LocationListComponent } from './SessionPlanning/location/location-list/location-list.component';
import { LocationFormComponent } from './SessionPlanning/location/location-form/location-form.component';
import { DashboardComponent } from './SessionPlanning/dashboard/dashboard.component';
import { AdminEvaluationComponent } from './evaluation/admin-evaluation/admin-evaluation.component';
import { AdminCourseComponent } from './admin-course/admin-course.component';
import { AdminEnrollmentsComponent } from './admin-enrollments/admin-enrollments/admin-enrollments.component';
import { AdminInstallmentsComponent } from './admin-installments/admin-installments/admin-installments.component';
import { AdminPaymentsInvoicesComponent } from './admin-payments-invoices/admin-payments-invoices.component'
import { ForumAdminComponent } from './forum/forum-admin.component';
import { ParticipantsAdminComponent } from './events/participants-admin.component';
import { LocationDetailComponent } from './SessionPlanning/location/location-detail/location-detail.component';
import { AdminBadgeComponent } from './certification/admin-badge/admin-badge.component';
import { AdminReclamationsComponent } from './admin-reclamations/admin-reclamations.component';

const routes: Routes = [
  {
    path: '',
    component: BackLayoutComponent,
    children: [
      {
        path: '',
        component: StatisticsComponent,
        data: { title: 'Tableau de bord' },
      },
      { path: 'evaluations', component: AdminEvaluationComponent },
      { path: 'badges', component: AdminBadgeComponent },
      { path: 'courses', component: AdminCourseComponent },
      //Session
      { path: 'locations', component: LocationListComponent },
      { path: 'locations/new', component: LocationFormComponent },
      { path: 'locations/edit/:id', component: LocationFormComponent },
      { path: 'dashboardSession', component: DashboardComponent },
      { path: 'payments', component: AdminPaymentsInvoicesComponent, data: { title: 'Paiements et Factures' } },
      { path: 'installments', component: AdminInstallmentsComponent, data: { title: 'Paiements Echelonnes' } },
      { path: 'enrollments', component: AdminEnrollmentsComponent, data: { title: 'Inscriptions' } },
      { path: 'forum', component: ForumAdminComponent },
      { path: 'events', component: ParticipantsAdminComponent },
      { path: 'locations/:id', component: LocationDetailComponent },

      {
        path: 'partners',
        loadComponent: () => import('./partners/partners.component').then(m => m.PartnersComponent),
        data: { title: 'Partners Management' },
      },
      {
        path: 'trainers',
        loadComponent: () => import('./trainers/trainers.component').then(m => m.TrainersComponent),
        data: { title: 'Trainers Management' },
      },
      {
        path: 'hr-analysis',
        loadComponent: () => import('./hr-analysis/hr-analysis.component').then(m => m.HrAnalysisComponent),
        data: { title: 'HR Analysis' },
      },
      
      {
        path: 'jobs',
        loadComponent: () => import('./jobs/jobs.component').then(m => m.JobsComponent),
        data: { title: 'Jobs Management' },
      },
      {
        path: 'coupons',
        loadComponent: () => import('./coupons/coupons.component').then(m => m.CouponsComponent),
        data: { title: 'Coupons Management' },
      },
      { path: 'reclamations', component: AdminReclamationsComponent, data: { title: 'Réclamations' } },
    ],
  },
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
})
export class BackOfficeRoutingModule { }
