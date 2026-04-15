import { Component } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

@Component({
  selector: 'app-back-layout',
  templateUrl: './back-layout.component.html',
  styleUrl: './back-layout.component.scss',
})
export class BackLayoutComponent {
  private isSidebarCollapsed = new BehaviorSubject<boolean>(false);
  isSidebarCollapsed$ = this.isSidebarCollapsed.asObservable();

  constructor() {
   
  }
}
