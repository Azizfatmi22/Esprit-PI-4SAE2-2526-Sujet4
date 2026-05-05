// Environment configuration for API endpoints
export const environment = {
  production: false,
  apiGatewayUrl: 'http://localhost:8085',
  courseServiceBasePath: '/api/courses',
  uploadBasePath: '/api/courses/uploads',  // ← Add /uploads

  // Keycloak Configuration
  keycloakUrl: 'http://192.168.222.136:8180', // Default: http://localhost:8180 (Set for Linux IP)
  keycloakRealm: 'myrealm',
  keycloakClientId: 'angular-frontend'
};