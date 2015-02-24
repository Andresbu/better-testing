package ch.noser.bettertesting

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.testing.Test
import org.gradle.api.tasks.testing.TestReport
import org.gradle.plugins.ide.idea.IdeaPlugin

/**
 * Add support for integration- and system-tests for java builds.
 *
 * Goal of this plugin:
 * - All types of tests (i.e unit-, integration- and system-tests) can be run in isolation.
 * - Reports are generated per type of test (i.e one report for unit-, integration- and system-tests).
 * - For tasks, i.e the task 'check', leading to the execution of multiple tests tasks, a common report gets generated.
 *
 * Future goals (not yet implemented):
 * - Aggregate test reports of subprojects
 *
 * We add two new source sets (i.e types of tests):
 * - 'integration' (integration tests on class level, which means intra system integration tests)
 *   These tests do run automatically when the 'check' task is invoked (i.e as well for the 'build' task)
 *   This is aimed at integration tests that integrate modules (i.e classes or packages), i.e different classes within your
 *   project. You most likely will use mocking here, i.e to mock a database.
 *   Such tests are typically fast (< 5s / test)
 *
 * - 'system' (System tests and potentially inter system integration tests)
 *   These tests do NOT run automatically. They are intended to be run manually after the project has been built.
 *   This is aimed at system tests that test systems, i.e your entire module/project.
 *   As we do not provide a further type of tests for inter system integration tests, such tests
 *   fall into this type as well (i.e test your project together with a databse).
 *   Such tests are typically slow (> 20s / test)
 *
 * What more?
 * - Test reports for multiple test sets usually overwrite each other. As a consequence, when two tasks of type test run,
 *   only the report of the last run task will be available.
 *   > This plugin lets all tasks of type test generate their report in different folders
 *
 * - The test task runs before the integration test task.
 */
class BetterTesting implements Plugin<Project> {
    @Override
    void apply(final Project project) {

        // Do not reapply plugin
        if (project.plugins.hasPlugin(BetterTesting.class)) {
            return;
        }

        // We need the JavaPlugin as we extend its functionality
        if (!project.plugins.hasPlugin(JavaPlugin.class)) {
            project.plugins.apply(JavaPlugin.class)
        }

        project.sourceSets {
            system // System integration

            integration // Module integration
        }

        project.dependencies {
            /* System tests */
            systemCompile project.sourceSets.main.output
            systemCompile project.configurations.testCompile
            systemCompile project.sourceSets.test.output
            systemRuntime project.configurations.testRuntime

            /* Module tests */
            integrationCompile project.sourceSets.main.output
            integrationCompile project.configurations.testCompile
            integrationCompile project.sourceSets.test.output
            integrationRuntime project.configurations.testRuntime
        }

        /** Module integration tests */
        project.task('integration', type: Test, description: 'Runs the integration tests.', group: 'Verification') {
            testClassesDir = project.sourceSets.integration.output.classesDir
            classpath = project.sourceSets.integration.runtimeClasspath
            shouldRunAfter 'test'
        }

        /** System integration tests */
        project.task('system', type: Test, dependsOn: [project.build], description: 'Runs the system tests.', group: 'Verification') {
            testClassesDir = project.sourceSets.system.output.classesDir
            classpath = project.sourceSets.system.runtimeClasspath
        }

        /** Create report for unit and module integration tests */
        project.task('reportOnCheck', type: TestReport, dependsOn: [project.test, project.integration], description: 'Create common report for unit- and integration-tests.', group: 'Verification') {
            destinationDir = project.file("${project.reporting.baseDir}/allOnCheck")
            reportOn project.test, project.integration

            /* Continue on test failures as we want to report on all tests */
            project.tasks.withType(Test) {
                ignoreFailures true
            }
        }

        project.check.dependsOn 'reportOnCheck'

        /** Run all tests and produce a common report for all tests run */
        project.task('allTests', type: TestReport, dependsOn: [project.build, project.system], description: 'Execute unit-, integration- and system-tests and create a common report.', group: 'Verification') {
            destinationDir = project.file("${project.reporting.baseDir}/all")
            reportOn project.test, project.integration, project.system

            /* Continue on test failures as we want to report on all tests */
            project.tasks.withType(Test) {
                ignoreFailures true
            }
        }

        /* Separate test reports of different tests (otherwise the latest wins and overrides others) */
        project.tasks.withType(Test) {
            reports.html.destination = "${project.reporting.baseDir}/${name}s"
        }

        if (project.plugins.hasPlugin(IdeaPlugin.class)) {
            project.allprojects {
                idea {
                    module {
                        testSourceDirs += sourceSets.getByName('integration').getAllJava().getSrcDirs()
                        testSourceDirs += sourceSets.getByName('system').getAllJava().getSrcDirs()
                    }
                }
            }
        }
    }
}
