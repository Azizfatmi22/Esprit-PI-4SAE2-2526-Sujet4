// Environment configuration for API endpoints
export const environment = {
  production: false,
  apiGatewayUrl: 'http://127.0.0.1:8085',
  courseServiceBasePath: '/api/courses',
  uploadBasePath: '/api/courses/uploads',  // ← Add /uploads
};