# Gradle Testing Plugin
Add support for integration- and system-tests for java builds.

## Goal of this plugin:
 * All types of tests (i.e unit-, integration- and system-tests) can be run in isolation.
 * Reports are generated per type of test (i.e one report for unit-, integration- and system-tests).
 * For tasks, i.e the task 'check', leading to the execution of multiple tests tasks, a common report gets generated.

## Future goals (not yet implemented):
  * Aggregate test reports of subprojects

## Features
We add two new source sets (i.e types of tests):

### Integration tests
(Integration tests on class level, which means intra system integration tests)
* These tests do run automatically when the 'check' task is invoked (i.e as well for the 'build' task)
* This is aimed at integration tests that integrate modules (i.e classes or packages), i.e different classes within your
    project. You most likely will use mocking here, i.e to mock a database.
* Such tests are typically fast (< 5s / test)

### System tests
(System tests and potentially inter system integration tests)
* These tests do NOT run automatically. They are intended to be run manually after the project has been built.
* This is aimed at system tests that test systems, i.e your entire module/project.
* As we do not provide a further type of tests for inter system integration tests, such tests fall into this type as well (i.e test your project together with a databse).
* Such tests are typically slow (> 20s / test)

## What more?
* Test reports for multiple test sets usually overwrite each other. As a consequence, when two tasks of type test run,
    only the report of the last run task will be available. This plugin lets all tasks of type test generate their report in different folders
* The test task runs before the integration test task.
