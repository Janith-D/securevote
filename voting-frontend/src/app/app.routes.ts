import {RouterModule, Routes} from '@angular/router';
import {LandingPageComponent} from './pages/landing-page/landing-page.component';
import {DashboardComponent} from './pages/dashboard/dashboard.component';
import {LoginComponent} from './pages/login/login.component';
import {NgModule} from '@angular/core';
import {RegisterComponent} from './pages/register/register.component';

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
  }
];
@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule  { }


