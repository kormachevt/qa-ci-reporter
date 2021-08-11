package com.github.kormachevt.qa.ci.reporter.plugin

import org.gradle.api.DefaultTask
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import org.openmbee.testrail.cli.JUnitPublisher
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

abstract class ReportToTestRailTask : DefaultTask() {

    init {
        description = "Send report to the TestRail"
        group = BasePlugin.BUILD_GROUP
    }

    @get:Input
    @get:Option(option = "url", description = "url")
    abstract val url: Property<String>

    @get:Input
    @get:Option(option = "username", description = "login")
    abstract val username: Property<String>

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
    abstract val suiteId: Property<String>

    @get:Input
    @get:Option(option = "skip-close-run", description = "skip-close-run")
    abstract val skipCloseRun: Property<String>

    @TaskAction
    @Suppress("SpreadOperator")
    fun publish() {
        val args = mutableListOf(
            "--directory=./build/test-results/test/",
            "--host=${url.get()}",
            "--user=${username.get()}",
            "--password=${password.get()}",
            "--suite-id=${suiteId.get()}",
            "--run-name=[${env.get().toUpperCase()}] ${title.get()} " +
                "[${LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))}]",
        )
        if (skipCloseRun.getOrElse("false").toBoolean()) {
            args.add("--skip-close-run")
        }
        JUnitPublisher.main(*args.toTypedArray())
    }
}
