// Polyfills for test environment
// Fix for sockjs-client expecting Node.js 'global'
if (typeof global === 'undefined') {
  (window as any).global = window;
}
if (typeof window !== 'undefined') {
  (window as any).global = window;
}