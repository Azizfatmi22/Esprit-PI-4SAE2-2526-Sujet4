import { Component } from '@angular/core';
import { KeycloakService } from './front-office/services/keycloak.service';
import { User } from './user';
import { UserService } from './front-office/services/user.service';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrl: './app.component.scss'
})
export class AppComponent {
  title = 'formini-app';

  

  currentUser: User | undefined;

  constructor(private userService: UserService) {}

  async ngOnInit() {
    this.currentUser = await this.userService.loadUser();
    console.log('Current User:', this.currentUser);
  }


  
}

