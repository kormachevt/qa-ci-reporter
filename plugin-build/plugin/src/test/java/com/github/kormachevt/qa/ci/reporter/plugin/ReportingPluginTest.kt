package com.github.kormachevt.qa.ci.reporter.plugin

import org.gradle.testfixtures.ProjectBuilder
import org.junit.Test

class ReportingPluginTest {

    @Test
    fun `plugin is applied correctly to the project`() {
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply("com.github.kormachevt.qa.ci.reporter.plugin")

        assert(project.tasks.getByName("publishToAllure") is ReportToAllureServerTask)
        assert(project.tasks.getByName("publishToTestRail") is ReportToTestRailTask)
    }
}
