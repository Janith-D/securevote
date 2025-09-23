import {RouterModule, Routes} from '@angular/router';
import {LandingPageComponent} from './pages/landing-page/landing-page.component';
import {DashboardComponent} from './pages/dashboard/dashboard.component';
import {LoginComponent} from './pages/login/login.component';
import {NgModule} from '@angular/core';
import {RegisterComponent} from './pages/register/register.component';
import {AdminDashboardComponent} from './pages/admin-dashboard/admin-dashboard.component';

// @ts-ignore
export const routes: Routes = [
  {
    path:'',
    redirectTo:'landingPage',
    pathMatch:'full',
  },
  {
    path:'landingPage',
    component:LandingPageComponent
  },
  {
    path:'register',
    component:RegisterComponent
  },
  {
    path:'login',
    component:LoginComponent
  },
  {
    path:'dashboard',
    component:DashboardComponent
  },
  {
    path:'admin-dashboard',
    component:AdminDashboardComponent
  }
];
@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule  { }


