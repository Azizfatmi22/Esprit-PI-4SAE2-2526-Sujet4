import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { PageNotFoundComponent } from './page-not-found/page-not-found.component';
import { adminGuard } from './Guard/admin.guard';
import { roleRedirectGuard } from './Guard/role-redirect.guard';

const routes: Routes = [
  {
    path: '',
    canActivate: [roleRedirectGuard],
    loadChildren: () =>
      
      import('./front-office/front-office.module').then(
        (m) => m.FrontOfficeModule,
      ),
  },
  {
    path: 'admin',
    canActivate: [adminGuard],
    loadChildren: () =>
      import('./back-office/back-office.module').then(
        (m) => m.BackOfficeModule,
      ),
  },
  {
    path: '**',
    component:PageNotFoundComponent
  },
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule],
})
export class AppRoutingModule {}
