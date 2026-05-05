// Karma configuration file
// https://karma-runner.github.io/latest/config/configuration-file.html

module.exports = function (config) {
  config.set({
    basePath: '',
    frameworks: ['jasmine', '@angular-devkit/build-angular'],
    plugins: [
      require('karma-jasmine'),
      require('karma-chrome-launcher'),
      require('karma-jasmine-html-reporter'),
      require('karma-coverage'),
      require('karma-junit-reporter'),
      require('karma-spec-reporter'),
      require('@angular-devkit/build-angular/plugins/karma')
    ],

    client: {
      jasmine: {},
      clearContext: false
    },

    jasmineHtmlReporter: {
      suppressAll: true
    },

    coverageReporter: {
      dir: require('path').join(__dirname, './coverage/formini-app'),
      subdir: '.',
      reporters: [
        { type: 'html' },
        { type: 'text-summary' },
        { type: 'lcovonly', file: 'lcov.info' }
      ]
    },

    junitReporter: {
      outputDir: 'reports/junit',
      outputFile: 'test-results.xml',
      useBrowserName: false
    },

    // Use 'spec' instead of 'progress' to show individual test names in the terminal
    reporters: ['spec', 'kjhtml', 'coverage', 'junit'],

    specReporter: {
      maxLogLines: 5,
      suppressErrorSummary: false,
      suppressFailed: false,
      suppressPassed: false,
      suppressSkipped: true,
      showSpecTiming: false,
      failFast: false
    },

    customLaunchers: {
      ChromeHeadlessCI: {
        base: 'ChromeHeadless',
        flags: [
          '--no-sandbox',
          '--disable-setuid-sandbox',
          '--disable-dev-shm-usage',
          '--disable-gpu',
          '--disable-hang-monitor'
        ]
      }
    },

    // Fix for "Client disconnected" exit code 1 on Windows
    browserDisconnectTimeout: 10000,
    browserDisconnectTolerance: 2,
    browserNoActivityTimeout: 60000,

    browsers: ['ChromeHeadlessCI'],
    restartOnFileChange: true
  });
};