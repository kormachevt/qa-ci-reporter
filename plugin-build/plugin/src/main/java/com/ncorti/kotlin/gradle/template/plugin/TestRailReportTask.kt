package com.ncorti.kotlin.gradle.template.plugin

import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import org.openmbee.testrail.cli.JUnitPublisher

abstract class TestRailReportTask : DefaultTask() {

    init {
        description = "Send report to the TestRail"

        // Don't forget to set the group here.
        // group = BasePlugin.BUILD_GROUP
    }

    @get:Input
    @get:Option(option = "url", description = "url")
    abstract val url: Property<String>

    @get:Input
    @get:Option(option = "login", description = "login")
    abstract val login: Property<String>

    @get:Input
    @get:Option(option = "password", description = "password")
    abstract val password: Property<String>

    @get:Input
    @get:Option(option = "env", description = "env")
    abstract val env: Property<String>

    @get:Input
    @get:Option(option = "title", description = "title")
    abstract val title: Property<String>

    @get:Input
    @get:Option(option = "suite-id", description = "suite-id")
    abstract val suiteId: Property<Int>

    @get:Input
    @get:Option(option = "skip-close-run", description = "skip-close-run")
    abstract val skipCloseRun: Property<Boolean>

    @TaskAction
    fun sampleAction() {
        JUnitPublisher.main(
            "-d ./build/test-results/test",
            "-h $url",
            "-u $login",
            "-p $password",
            "-sid $suiteId",
            "--run-name \"[${env.get().toUpperCase()}] $title [${java.util.Calendar.getInstance()}]\"",
            "--skip-close-run"
        )
    }
}
