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
      require('karma-junit-reporter'),                          // ← added
      require('@angular-devkit/build-angular/plugins/karma')
    ],
    client: {
      jasmine: {
        // you can add configuration options for Jasmine here
        // the possible options are listed at https://jasmine.github.io/api/edge/Configuration.html
        // for example, you can disable the random execution with `random: false`
        // or set a specific seed with `seed: 4321`
      },
    },
    jasmineHtmlReporter: {
      suppressAll: true // removes the duplicated traces
    },
    coverageReporter: {
      dir: require('path').join(__dirname, './coverage/formini-app'),
      subdir: '.',
      reporters: [
        { type: 'html' },
        { type: 'text-summary' },
        { type: 'lcovonly', file: 'lcov.info' }                // ← added for coverage tracking
      ]
    },
    // ── JUnit reporter config (Jenkins reads this XML) ───────────────────────
    junitReporter: {
      outputDir:          'reports/junit',   // folder where XML is written
      outputFile:         'test-results.xml',
      suite:              '',
      useBrowserName:     false,             // keeps filename predictable in CI
      nameFormatter:      undefined,
      classNameFormatter: undefined,
      properties:         {}
    },
    // ─────────────────────────────────────────────────────────────────────────
    reporters: ['progress', 'kjhtml', 'junit', 'coverage'],    // ← added junit & coverage
    browsers: ['Chrome'],
    restartOnFileChange: true
  });
};