// Karma configuration file, see link for more information
// https://karma-runner.github.io/1.0/config/configuration-file.html

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
      require('@angular-devkit/build-angular/plugins/karma')
    ],
    client: {
      jasmine: {},
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
      outputDir:          'reports/junit',
      outputFile:         'test-results.xml',
      suite:              '',
      useBrowserName:     false,
      nameFormatter:      undefined,
      classNameFormatter: undefined,
      properties:         {}
    },
    reporters: ['progress', 'kjhtml', 'junit', 'coverage'],

    // ── Custom launcher: ChromeHeadless with --no-sandbox for Docker/CI ──
    customLaunchers: {
      ChromeHeadlessCI: {
        base: 'ChromeHeadless',
        flags: [
          '--no-sandbox',
          '--disable-setuid-sandbox',
          '--disable-dev-shm-usage',
          '--disable-gpu'
        ]
      }
    },

    browsers: ['ChromeHeadlessCI'],
    restartOnFileChange: true
  });
};
