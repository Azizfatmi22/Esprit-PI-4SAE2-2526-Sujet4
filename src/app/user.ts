export interface User {
    id: string;            // Keycloak ID (sub)
  username: string;      // preferred_username
  email: string;
  fullName: string;
  roles: string[];
}
